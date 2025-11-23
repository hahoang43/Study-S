package com.example.study_s.data.repository

import com.example.study_s.data.model.MessageModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class GroupChatRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    fun getGroupMessages(groupId: String): Flow<List<MessageModel>> = callbackFlow {
        val messagesRef = db.collection("groups").document(groupId).collection("messages")
            .orderBy("timestamp", Query.Direction.DESCENDING)

        val listener = messagesRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                close(e)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val messages = snapshot.toObjects(MessageModel::class.java)
                trySend(messages).isSuccess
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun sendGroupMessage(groupId: String, message: MessageModel) {
        val messagesRef = db.collection("groups").document(groupId).collection("messages")
        messagesRef.add(message).await()
    }

    suspend fun editMessage(groupId: String, messageId: String, newContent: String) {
        val messageRef = db.collection("groups").document(groupId).collection("messages").document(messageId)
        messageRef.update("content", newContent).await()
    }

    suspend fun deleteMessage(groupId: String, messageId: String) {
        val messageRef = db.collection("groups").document(groupId).collection("messages").document(messageId)
        messageRef.delete().await()
    }
}
