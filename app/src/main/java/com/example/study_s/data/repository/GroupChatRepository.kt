package com.example.study_s.data.repository

import com.example.study_s.data.model.Message
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await

class GroupChatRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private fun messagesRef(groupId: String) =
        db.collection("groups").document(groupId).collection("messages")

    suspend fun sendMessage(groupId: String, message: Message) {
        messagesRef(groupId).add(message).await()
    }

    fun listenForMessages(groupId: String, onMessagesChanged: (List<Message>) -> Unit): ListenerRegistration {
        return messagesRef(groupId)
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val messages = snapshot.toObjects(Message::class.java)
                    onMessagesChanged(messages)
                }
            }
    }
}
