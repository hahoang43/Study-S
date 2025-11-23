package com.example.study_s.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.study_s.data.model.MessageModel
import com.example.study_s.data.repository.GroupChatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class GroupChatViewModel(private val groupChatRepository: GroupChatRepository = GroupChatRepository()) : ViewModel() {

    private val _messages = MutableStateFlow<List<MessageModel>>(emptyList())
    val messages: StateFlow<List<MessageModel>> = _messages

    private val _userRemoved = MutableSharedFlow<String>()
    val userRemoved = _userRemoved.asSharedFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading

    private val _uploadSuccess = MutableSharedFlow<Boolean>()
    val uploadSuccess = _uploadSuccess.asSharedFlow()

    fun listenToGroupMessages(groupId: String) {
        viewModelScope.launch {
            groupChatRepository.getGroupMessages(groupId).collect {
                _messages.value = it
            }
        }
    }

    fun sendMessage(groupId: String, senderId: String, content: String, senderName: String) {
        viewModelScope.launch {
            val message = MessageModel(
                senderId = senderId,
                content = content,
                senderName = senderName,
                timestamp = System.currentTimeMillis(),
                type = "text"
            )
            groupChatRepository.sendGroupMessage(groupId, message)
        }
    }

    fun sendFile(
        context: Context,
        groupId: String,
        senderId: String,
        senderName: String,
        fileUri: Uri,
        fileType: String // "image" or "file"
    ) {
        viewModelScope.launch {
            _isUploading.value = true
            val fileName = getFileName(context, fileUri)
            val finalUri = if (fileType == "image") {
                withContext(Dispatchers.IO) {
                    compressImage(context, fileUri)
                }
            } else {
                fileUri
            }

            if (finalUri == null) {
                _isUploading.value = false
                _uploadSuccess.emit(false)
                return@launch
            }

            MediaManager.get().upload(finalUri)
                .unsigned("Study_S")
                .option("resource_type", if (fileType == "image") "image" else "raw")
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String?) {}

                    override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}

                    override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                        val url = resultData?.get("secure_url") as? String
                        if (url != null) {
                            val message = MessageModel(
                                senderId = senderId,
                                senderName = senderName,
                                content = fileName,
                                timestamp = System.currentTimeMillis(),
                                type = fileType,
                                fileUrl = url
                            )
                            viewModelScope.launch {
                                groupChatRepository.sendGroupMessage(groupId, message)
                                _uploadSuccess.emit(true)
                            }
                        } else {
                            viewModelScope.launch { _uploadSuccess.emit(false) }
                        }
                        _isUploading.value = false
                    }

                    override fun onError(requestId: String?, error: ErrorInfo?) {
                        _isUploading.value = false
                        viewModelScope.launch { _uploadSuccess.emit(false) }
                    }

                    override fun onReschedule(requestId: String?, error: ErrorInfo?) {}
                }).dispatch(context)
        }
    }

    private suspend fun compressImage(context: Context, imageUri: Uri): Uri? {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(imageUri)
                val originalBitmap = BitmapFactory.decodeStream(inputStream)

                val maxHeight = 1280
                val maxWidth = 1280
                val scale = Math.min(
                    maxWidth.toFloat() / originalBitmap.width,
                    maxHeight.toFloat() / originalBitmap.height
                )
                val newWidth = (originalBitmap.width * scale).toInt()
                val newHeight = (originalBitmap.height * scale).toInt()

                val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)

                val outputStream = ByteArrayOutputStream()
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                val byteArray = outputStream.toByteArray()

                val tempFile = File(context.cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
                val fileOutputStream = FileOutputStream(tempFile)
                fileOutputStream.write(byteArray)
                fileOutputStream.close()

                Uri.fromFile(tempFile)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    private fun getFileName(context: Context, uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (columnIndex != -1) {
                        result = cursor.getString(columnIndex)
                    }
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result ?: "Unknown"
    }


    fun notifyUserRemoved(removedUserId: String) {
        viewModelScope.launch {
            _userRemoved.emit(removedUserId)
        }
    }

    fun editMessage(groupId: String, messageId: String, newContent: String) {
        viewModelScope.launch {
            groupChatRepository.editMessage(groupId, messageId, newContent)
        }
    }

    fun deleteMessage(groupId: String, messageId: String) {
        viewModelScope.launch {
            groupChatRepository.deleteMessage(groupId, messageId)
        }
    }
}
