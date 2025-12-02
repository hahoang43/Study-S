package com.example.study_s.data.repository

import com.example.study_s.data.model.ChatModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ChatListRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val chatsCollection = db.collection("chats")

    private fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    // ✅ NEW: Function to get the total unread message count
    fun getUnreadMessagesCountFlow(): Flow<Int> = callbackFlow {
        val currentUserId = getCurrentUserId()
        if (currentUserId == null) {
            trySend(0)
            close(Exception("User not logged in"))
            return@callbackFlow
        }

        val listenerRegistration = chatsCollection
            .whereArrayContains("members", currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val unreadCount = snapshot?.documents?.count { doc ->
                    val lastMessage = doc.get("lastMessage") as? Map<String, Any>
                    val readBy = lastMessage?.get("readBy") as? Map<String, Boolean>
                    // Count if the current user has NOT read the last message
                    readBy?.get(currentUserId) == false
                } ?: 0

                trySend(unreadCount)
            }

        awaitClose { listenerRegistration.remove() }
    }


    suspend fun markChatAsRead(chatId: String) {
        val userId = getCurrentUserId()
        if (userId != null) {
            chatsCollection.document(chatId)
                .update("lastMessage.readBy.$userId", true)
                .await()
        }
    }

    suspend fun deleteChats(chatIds: Set<String>) {
        val batch = db.batch()
        chatIds.forEach {
            val docRef = chatsCollection.document(it)
            batch.delete(docRef)
        }
        batch.commit().await()
    }

    fun getChats(): Flow<List<ChatModel>> = callbackFlow {
        val currentUserId = getCurrentUserId()
        if (currentUserId == null) {
            close(Exception("User not logged in"))
            return@callbackFlow
        }

        val listenerRegistration = chatsCollection
            .whereArrayContains("members", currentUserId)
            .orderBy("lastMessage.timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val chats = snapshot?.toObjects(ChatModel::class.java) ?: emptyList()
                trySend(chats)
            }

        awaitClose { listenerRegistration.remove() }
    }
}
