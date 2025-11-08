package com.example.study_s.data.repository

import android.content.Context
import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.study_s.data.model.LibraryFile
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class LibraryRepository {
    private val db = FirebaseFirestore.getInstance()
    private val filesCollection = db.collection("libraryFiles")

    private val UPLOAD_PRESET_NAME = "Study_S"

    fun getAllFiles(): Flow<List<LibraryFile>> = flow {
        val snapshot = filesCollection
            .orderBy("uploadedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .await()

        val files = snapshot.toObjects(LibraryFile::class.java)
        emit(files)
    }

    suspend fun uploadFile(
        context: Context,
        fileUri: Uri,
        fileName: String,
        mimeType: String,
        uploaderId: String,
        uploaderName: String
    ): String = suspendCoroutine { continuation ->
        MediaManager.get().upload(fileUri)
            .option("resource_type", "auto")
            .option("folder", "study_s_files")
            .unsigned(UPLOAD_PRESET_NAME)
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {
                    println("Cloudinary Upload Started: $requestId")
                }

                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}

                override fun onSuccess(requestId: String, resultData: Map<*, *>?) {
                    val secureUrl = resultData?.get("secure_url") as? String
                    if (secureUrl != null) {
                        continuation.resume(secureUrl)
                        saveFileMetadata(
                            LibraryFile(
                                fileName = fileName,
                                fileUrl = secureUrl,
                                mimeType = mimeType,
                                uploaderId = uploaderId,
                                uploaderName = uploaderName,
                                uploadedAt = System.currentTimeMillis() // Gán giá trị thời gian
                            )
                        )
                    } else {
                        continuation.resumeWithException(Exception("Lỗi: Không nhận được URL an toàn từ Cloudinary."))
                    }
                }

                override fun onError(requestId: String, error: ErrorInfo) {
                    continuation.resumeWithException(Exception("Tải lên Cloudinary thất bại: ${error.description}"))
                }

                override fun onReschedule(requestId: String, error: ErrorInfo) {}
            })
            .dispatch(context)
    }

    private fun saveFileMetadata(file: LibraryFile) {
        filesCollection.document().set(file).addOnFailureListener { e ->
            println("Lỗi lưu metadata vào Firestore: ${e.message}")
        }
    }
}