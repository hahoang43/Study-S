package com.example.study_s.data.repository

import com.example.study_s.data.model.MessageModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await

class GroupChatRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private fun messagesRef(groupId: String) =
        db.collection("groups").document(groupId).collection("messages")

    suspend fun sendMessage(groupId: String, message: MessageModel) {
        messagesRef(groupId).add(message).await()
    }

    fun getGroupMessages(groupId: String, onMessagesChanged: (List<MessageModel>) -> Unit): ListenerRegistration {
        return messagesRef(groupId)
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val messages = snapshot.toObjects(MessageModel::class.java)
                    onMessagesChanged(messages)
                }
            }
    }
}
