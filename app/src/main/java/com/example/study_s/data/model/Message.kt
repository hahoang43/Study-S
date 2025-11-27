package com.example.study_s.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Message(
    @DocumentId
    val id: String = "",
    val senderId: String = "",
    val content: String = "", // used for text content, or caption for files
    @ServerTimestamp
    val timestamp: Date? = null,
    val type: String = "text", // "text", "image", "file"
    val url: String? = null,
    val fileName: String? = null,
    val fileSize: Long? = null,
    val readBy: Map<String, Boolean> = emptyMap()
)
