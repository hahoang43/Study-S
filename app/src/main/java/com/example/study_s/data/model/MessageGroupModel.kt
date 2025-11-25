package com.example.study_s.data.model

import com.google.firebase.firestore.DocumentId

data class MessageGroupModel(
    @DocumentId
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val content: String = "",
    val timestamp: Long? = null,
    val type: String = "text", // "text", "image", "file"
    val fileUrl: String? = null
)
