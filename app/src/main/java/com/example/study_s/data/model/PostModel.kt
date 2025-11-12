package com.example.study_s.data.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp


data class PostModel(
    val authorId: String = "",
    val content: String = "",
    val imageUrl: String? = null,
    val fileUrl: String? = null, // Thêm URL của tệp
    val fileName: String? = null, // Thêm tên của tệp
    @ServerTimestamp // Annotation này giúp Firebase tự động điền thời gian của server
    val timestamp: Timestamp? = null,
    val likesCount: Long = 0,
    val commentsCount: Long = 0,
    val likedBy: List<String> = emptyList(),
    val savedBy: List<String> = emptyList(),
    // <-- 1. THÊM TRƯỜNG NÀY
    @get:Exclude var postId: String = ""
)