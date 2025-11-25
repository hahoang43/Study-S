
package com.example.study_s.data.repository

import com.example.study_s.data.model.Chat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class ChatListRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val chatsCollection = db.collection("chats")

    private fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    fun getChats(): Flow<List<Chat>> = callbackFlow {
        val currentUserId = getCurrentUserId() ?: close(Exception("User not logged in"))

        val listenerRegistration = chatsCollection
            .whereArrayContains("members", currentUserId as Any)
            .orderBy("lastMessage.timestamp", Query.Direction.DESCENDING)
            .orderBy("__name__", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val chats = snapshot?.toObjects(Chat::class.java) ?: emptyList()
                trySend(chats).isSuccess
            }
        awaitClose { listenerRegistration.remove() }
    }
}
