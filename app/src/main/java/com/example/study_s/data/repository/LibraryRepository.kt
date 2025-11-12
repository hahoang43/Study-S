package com.example.study_s.data.repository

import android.content.Context
import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import com.example.study_s.data.model.LibraryFile
class LibraryRepository {
    private val db = FirebaseFirestore.getInstance()
    private val filesCollection = db.collection("libraryFiles")

    private val UPLOAD_PRESET_NAME = "Study_S"

    fun getAllFiles(): Flow<List<LibraryFile>> = flow {
        val snapshot = filesCollection
            .orderBy("uploadedAt", Query.Direction.DESCENDING)
            .get()
            .await()

        val files = snapshot.documents.mapNotNull { document ->
            document.toObject(LibraryFile::class.java)?.copy(id = document.id)
        }
        emit(files)
    }

    suspend fun uploadFile(
        context: Context,
        fileUri: Uri,
        fileName: String,
        mimeType: String,
        uploaderId: String,
        uploaderName: String,
        onProgress: (Int) -> Unit
    ): String = suspendCoroutine { continuation ->
        MediaManager.get().upload(fileUri)
            .option("resource_type", "raw")
            .option("flags", "attachment")
            .option("folder", "study_s_files")
            .unsigned(UPLOAD_PRESET_NAME)
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {
                    println("Cloudinary Upload Started: $requestId")
                    onProgress(0)
                }

                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                    val progress = if (totalBytes > 0) ((bytes.toDouble() / totalBytes) * 100).toInt() else 0
                    onProgress(progress)
                }

                override fun onSuccess(requestId: String, resultData: Map<*, *>?) {
                    onProgress(100)
                    val secureUrl = resultData?.get("secure_url") as? String
                    val publicId = resultData?.get("public_id") as? String

                    if (secureUrl != null && publicId != null) {
                        continuation.resume(secureUrl)
                        saveFileMetadata(
                            LibraryFile(
                                publicId = publicId,
                                fileName = fileName,
                                fileUrl = secureUrl,
                                mimeType = mimeType,
                                uploaderId = uploaderId,
                                uploaderName = uploaderName,
                                uploadedAt = System.currentTimeMillis()
                            )
                        )
                    } else {
                        continuation.resumeWithException(Exception("Lỗi: Không nhận được URL hoặc Public ID từ Cloudinary."))
                    }
                }

                override fun onError(requestId: String, error: ErrorInfo) {
                    onProgress(0)
                    continuation.resumeWithException(Exception("Tải lên Cloudinary thất bại: ${error.description}"))
                }

                override fun onReschedule(requestId: String, error: ErrorInfo) {}
            })
            .dispatch(context)
    }

    suspend fun deleteFile(file: LibraryFile) {
        if (file.id.isNotBlank()) {
            filesCollection.document(file.id).delete().await()
        } else {
            println("Lỗi: ID tệp không hợp lệ, không thể xóa.")
        }
    }

    suspend fun updateFileName(fileId: String, newName: String) {
        if (fileId.isNotBlank()) {
            filesCollection.document(fileId).update("fileName", newName).await()
        } else {
            println("Lỗi: ID tệp không hợp lệ, không thể cập nhật.")
        }
    }

    private fun saveFileMetadata(file: LibraryFile) {
        filesCollection.document().set(file).addOnFailureListener { e ->
            println("Lỗi lưu metadata vào Firestore: ${e.message}")
        }
    }
}