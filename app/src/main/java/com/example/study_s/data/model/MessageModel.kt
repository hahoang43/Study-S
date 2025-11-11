package com.example.study_s.data.model

data class MessageModel(
    val messageId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
