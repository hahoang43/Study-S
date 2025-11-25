
package com.example.study_s.data.repository

import android.util.Log
import com.example.study_s.data.model.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ChatRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val chatsCollection = db.collection("chats")

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
            chatsCollection.document(chatId).collection("messages").add(message).await()
            chatsCollection.document(chatId).update("lastMessage", message).await()
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
}
