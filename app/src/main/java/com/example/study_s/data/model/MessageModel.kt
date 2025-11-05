package com.example.study_s.data.model

data class Message(
    val messageId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
