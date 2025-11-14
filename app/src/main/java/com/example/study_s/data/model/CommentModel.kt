package com.example.study_s.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class CommentModel(
    val commentId: String = "",
    val postId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorAvatar: String? = null,
    val content: String = "",
    @ServerTimestamp
    val timestamp: Timestamp? = null
) {
    // Constructor trống cho Firestore
    constructor() : this("", "", "", "", null)
}