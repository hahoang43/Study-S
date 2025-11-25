
package com.example.study_s.data.repository

import android.net.Uri
import android.util.Log
import com.example.study_s.data.model.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ChatRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val chatsCollection = db.collection("chats")
    private val storage = FirebaseStorage.getInstance()

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

    suspend fun sendMessage(chatId: String, message: Message): Result<Unit> {
        return try {
            val messageRef = chatsCollection.document(chatId).collection("messages").add(message).await()
            chatsCollection.document(chatId).update("lastMessage", message.copy(id = messageRef.id)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getMessages(chatId: String): Flow<List<Message>> = callbackFlow {
        val listenerRegistration = chatsCollection.document(chatId).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val messages = snapshot?.toObjects(Message::class.java) ?: emptyList()
                trySend(messages).isSuccess
            }
        awaitClose { listenerRegistration.remove() }
    }

    suspend fun deleteMessage(chatId: String, messageId: String) {
        val messageRef = chatsCollection.document(chatId).collection("messages").document(messageId)
        messageRef.delete().await()
        updateLastMessage(chatId)
    }

    suspend fun editMessage(chatId: String, messageId: String, newContent: String) {
        val messageRef = chatsCollection.document(chatId).collection("messages").document(messageId)
        messageRef.update("content", newContent).await()
        updateLastMessage(chatId)
    }

    private suspend fun updateLastMessage(chatId: String) {
        val lastMessage = chatsCollection.document(chatId).collection("messages")
            .orderBy("timestamp", Query.Direction.DESCENDING).limit(1).get().await()
            .toObjects(Message::class.java).firstOrNull()
        chatsCollection.document(chatId).update("lastMessage", lastMessage).await()
    }

    suspend fun uploadFile(chatId: String, fileUri: Uri, fileType: String): Result<Message> {
        return try {
            val fileName = UUID.randomUUID().toString()
            val storageRef = storage.reference.child("chats/$chatId/$fileName")
            val uploadTask = storageRef.putFile(fileUri).await()
            val downloadUrl = uploadTask.storage.downloadUrl.await().toString()

            val message = Message(
                senderId = getCurrentUserId()!!,
                type = fileType,
                url = downloadUrl,
                fileName = fileUri.lastPathSegment,
                fileSize = uploadTask.bytesTransferred
            )
            Result.success(message)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getFileType(uri: Uri): String {
        // This is a simplified version. In a real app, you might want to use a more robust method
        // to determine the file type, such as checking the content type.
        val extension = uri.toString().substringAfterLast('.', "")
        return when {
            extension.matches(Regex("(?i)(jpg|jpeg|png|gif)")) -> "image"
            else -> "file"
        }
    }
}
