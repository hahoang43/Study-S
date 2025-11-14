package com.example.study_s.data.model

import com.google.firebase.firestore.DocumentId

data class MessageModel(
    @DocumentId
    val messageId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val content: String = "",
    val timestamp: Long? = null
)
