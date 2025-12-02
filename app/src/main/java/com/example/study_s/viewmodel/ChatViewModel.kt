package com.example.study_s.viewmodel

import android.app.Application
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.study_s.data.model.MessageModel
import com.example.study_s.data.model.UserModel
import com.example.study_s.data.repository.ChatRepository
import com.example.study_s.data.repository.UserRepository
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ChatViewModel(
    application: Application,
    private val chatId: String,
    private val targetUserId: String
) : AndroidViewModel(application) {

    private val chatRepository: ChatRepository = ChatRepository(application.applicationContext)
    private val userRepository: UserRepository = UserRepository()

    private val _messages = MutableStateFlow<List<MessageModel>>(emptyList())
    val messages: StateFlow<List<MessageModel>> = _messages.asStateFlow()

    private val _chatId = MutableStateFlow<String?>(null)

    private val _targetUser = MutableStateFlow<UserModel?>(null)
    val targetUser: StateFlow<UserModel?> = _targetUser.asStateFlow()

    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState: StateFlow<UploadState> = _uploadState.asStateFlow()

    private val _isBlockedByTarget = MutableStateFlow(false)
    val isBlockedByTarget: StateFlow<Boolean> = _isBlockedByTarget.asStateFlow()

    private val _haveIBlockedTarget = MutableStateFlow(false)
    val haveIBlockedTarget: StateFlow<Boolean> = _haveIBlockedTarget.asStateFlow()

    init {
        loadInitialStates()
        loadChatAndFilterMessages() // Đổi tên hàm cho rõ nghĩa
    }

    private fun loadInitialStates() {
        val currentUserId = userRepository.getCurrentUserId() ?: return

        viewModelScope.launch {
            userRepository.getUserProfileFlow(targetUserId).collect { targetUser ->
                _targetUser.value = targetUser
                _isBlockedByTarget.value = targetUser?.blockedUsers?.contains(currentUserId) == true
            }
        }

        viewModelScope.launch {
            userRepository.getUserProfileFlow(currentUserId).collect { currentUser ->
                _haveIBlockedTarget.value = currentUser?.blockedUsers?.contains(targetUserId) == true
            }
        }
    }

    private fun loadChatAndFilterMessages() {
        val currentUserId = userRepository.getCurrentUserId() ?: return

        viewModelScope.launch {
            chatRepository.getOrCreateChat(targetUserId).onSuccess { chatId ->
                _chatId.value = chatId

                val myProfileFlow = userRepository.getUserProfileFlow(currentUserId)
                val messagesFlow = chatRepository.getMessages(chatId)

                myProfileFlow.combine(messagesFlow) { currentUser, messages ->
                    val blockedList = currentUser?.blockedUsers ?: emptyList()
                    messages.filter { message -> message.senderId !in blockedList }
                }.collect { filteredMessages ->
                    _messages.value = filteredMessages
                }
            }
        }
    }

    fun sendMessage(text: String) {
        val currentUserId = userRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            chatRepository.getOrCreateChat(targetUserId).onSuccess { actualChatId ->
                val message = MessageModel(
                    senderId = currentUserId,
                    content = text,
                    type = "text"
                )
                chatRepository.sendMessage(actualChatId, message)
            }
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            chatRepository.getOrCreateChat(targetUserId).onSuccess { actualChatId ->
                chatRepository.deleteMessage(actualChatId, messageId)
            }
        }
    }

    fun editMessage(messageId: String, newContent: String) {
        viewModelScope.launch {
            chatRepository.getOrCreateChat(targetUserId).onSuccess { actualChatId ->
                chatRepository.editMessage(actualChatId, messageId, newContent)
            }
        }
    }

    fun deleteChat() {
        viewModelScope.launch {
            chatRepository.getOrCreateChat(targetUserId).onSuccess { actualChatId ->
                chatRepository.deleteChat(actualChatId)
            }
        }
    }

    fun sendFile(fileUri: Uri, type: String) {
        viewModelScope.launch {
            _uploadState.value = UploadState.Uploading
            // ✅ LẤY TÊN TỆP GỐC TRƯỚC KHI UPLOAD
            val localFileName = getFileName(getApplication(), fileUri)

            chatRepository.getOrCreateChat(targetUserId).onSuccess { actualChatId ->
                val result = chatRepository.uploadFile(fileUri)
                result.onSuccess { uploadResult ->
                    val message = MessageModel(
                        senderId = userRepository.getCurrentUserId() ?: "",
                        content = uploadResult.url,
                        type = type,
                        // ✅ SỬ DỤNG TÊN TỆP GỐC, KHÔNG DÙNG TÊN TỪ CLOUDINARY
                        fileName = localFileName ?: "file",
                        fileSize = uploadResult.bytes
                    )
                    chatRepository.sendMessage(actualChatId, message)
                    _uploadState.value = UploadState.Idle
                }.onFailure { exception ->
                    _uploadState.value = UploadState.Error(exception.message ?: "Lỗi không xác định")
                }
            }.onFailure {
                _uploadState.value = UploadState.Error("Không thể lấy thông tin cuộc trò chuyện.")
            }
        }
    }

    fun downloadFile(url: String, fileName: String) {
        val context = getApplication<Application>().applicationContext
        try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle(fileName)
                .setDescription("Đang tải xuống...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            downloadManager.enqueue(request)
            Toast.makeText(context, "Bắt đầu tải xuống...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Tải xuống thất bại: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    fun blockUser() {
        val currentUserId = userRepository.getCurrentUserId() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentUserDocRef = userRepository.getUserDocumentRef(currentUserId)
                currentUserDocRef.update("blockedUsers", FieldValue.arrayUnion(targetUserId)).await()
                withContext(Dispatchers.Main) { /* Optional UI update */ }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun unblockUser() {
        val currentUserId = userRepository.getCurrentUserId() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentUserDocRef = userRepository.getUserDocumentRef(currentUserId)
                currentUserDocRef.update("blockedUsers", FieldValue.arrayRemove(targetUserId)).await()
                withContext(Dispatchers.Main) {
                    println("User ${targetUserId} unblocked successfully.")
                }
            } catch (e: Exception) {
                println("Error unblocking user: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    // ✅ HÀM TIỆN ÍCH MỚI ĐỂ LẤY TÊN TỆP
    private fun getFileName(context: Context, uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        result = cursor.getString(index)
                    }
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                result = result.substring(cut + 1)
            }
        }
        return result
    }

    sealed class UploadState {
        object Idle : UploadState()
        object Uploading : UploadState()
        data class Error(val message: String) : UploadState()
    }
}
