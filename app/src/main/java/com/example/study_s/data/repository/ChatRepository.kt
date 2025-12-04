package com.example.study_s.data.repository

import android.content.Context
import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.study_s.data.model.MessageModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume

data class CloudinaryResult(
    val url: String,
    val resourceType: String, 
    val originalFilename: String,
    val bytes: Long
)

class ChatRepository (private val context: Context){

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val chatsCollection = db.collection("chats")

    suspend fun uploadFile(fileUri: Uri): Result<CloudinaryResult> {
        return suspendCancellableCoroutine { continuation ->
            val requestId = MediaManager.get().upload(fileUri)
                .callback(object : UploadCallback {
                    override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                        val url = resultData?.get("secure_url") as? String ?: ""
                        val resourceType = resultData?.get("resource_type") as? String ?: "raw"
                        val originalFilename = resultData?.get("original_filename") as? String ?: "file"
                        val bytes = (resultData?.get("bytes") as? Number)?.toLong() ?: 0L

                        val cloudinaryResult = CloudinaryResult(
                            url = url,
                            resourceType = resourceType,
                            originalFilename = originalFilename,
                            bytes = bytes
                        )
                        if (continuation.isActive) {
                            continuation.resume(Result.success(cloudinaryResult))
                        }
                    }

                    override fun onError(requestId: String?, error: ErrorInfo?) {
                        if (continuation.isActive) {
                            continuation.resume(Result.failure(Exception(error?.description ?: "Cloudinary upload failed")))
                        }
                    }

                    override fun onStart(requestId: String?) { }
                    override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) { }
                    override fun onReschedule(requestId: String?, error: ErrorInfo?) { }
                }).dispatch()

            continuation.invokeOnCancellation {
                MediaManager.get().cancelRequest(requestId)
            }
        }
    }


    private fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    suspend fun getOrCreateChat(targetUserId: String): Result<String> {
        val currentUserId = getCurrentUserId() ?: return Result.failure(Exception("User not logged in"))
        val members = listOf(currentUserId, targetUserId).sorted()
        val chatId = members.joinToString("_")

        return try {
            val chatDocRef = chatsCollection.document(chatId)
            val chatDoc = chatDocRef.get().await()

            if (!chatDoc.exists()) {
                val chatData = mapOf(
                    "members" to members,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "lastMessage" to null
                )
                chatDocRef.set(chatData).await()
            }
            Result.success(chatId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendMessage(chatId: String, message: MessageModel): Result<Unit> {
        return try {
            val currentUserId = getCurrentUserId() ?: return Result.failure(Exception("User not logged in"))
            val targetUserId = getTargetUserId(chatId, currentUserId)

            val messageWithReadStatus = message.copy(
                senderId = currentUserId, 
                readBy = mapOf(currentUserId to true, targetUserId to false)
            )

            val messageDocRef = chatsCollection.document(chatId)
                .collection("messages")
                .add(messageWithReadStatus)
                .await()

            val finalMessage = messageWithReadStatus.copy(id = messageDocRef.id)
            chatsCollection.document(chatId).update("lastMessage", finalMessage).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getMessages(chatId: String): Flow<List<MessageModel>> = callbackFlow {
        val listenerRegistration = chatsCollection.document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error) 
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val messages = snapshot.documents.mapNotNull { doc ->
                        doc.toObject<MessageModel>()?.copy(id = doc.id)
                    }
                    trySend(messages) 
                }
            }
        awaitClose { listenerRegistration.remove() }
    }

    suspend fun deleteMessage(chatId: String, messageId: String): Result<Unit> {
        return try {
            val messageRef = chatsCollection.document(chatId).collection("messages").document(messageId)
            messageRef.delete().await()
            updateLastMessage(chatId) 
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteChat(chatId: String): Result<Unit> {
        return try {
            val chatDocRef = chatsCollection.document(chatId)
            val messagesCollection = chatDocRef.collection("messages")

            val allMessages = messagesCollection.get().await()
            if (!allMessages.isEmpty) {
                allMessages.documents.chunked(499).forEach { chunk ->
                    val batch = db.batch()
                    chunk.forEach { document ->
                        batch.delete(document.reference)
                    }
                    batch.commit().await()
                }
            }

            chatDocRef.delete().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun editMessage(chatId: String, messageId: String, newContent: String): Result<Unit> {
        return try {
            val messageRef = chatsCollection.document(chatId).collection("messages").document(messageId)
            messageRef.update(
                mapOf(
                    "content" to newContent,
                    "isEdited" to true
                )
            ).await()
            updateLastMessage(chatId) 
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun updateLastMessage(chatId: String) {
        val lastMessageQuery = chatsCollection.document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .await()

        val lastMessage = if (lastMessageQuery.isEmpty) {
            null
        } else {
            val doc = lastMessageQuery.documents.first()
            doc.toObject<MessageModel>()?.copy(id = doc.id)
        }

        chatsCollection.document(chatId).update("lastMessage", lastMessage).await()
    }


    private suspend fun getTargetUserId(chatId: String, currentUserId: String): String {
        val chatDoc = chatsCollection.document(chatId).get().await()
        val members = chatDoc.get("members") as? List<String> ?: emptyList()
        return members.firstOrNull { it != currentUserId } ?: ""
    }
    fun getFileType(fileUri: Uri): String? {
        return context.contentResolver.getType(fileUri)
    }
}