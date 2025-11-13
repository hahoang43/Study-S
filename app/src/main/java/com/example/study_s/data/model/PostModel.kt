package com.example.study_s.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp

/**
 * Data class đại diện cho một bài viết (Post).
 * Đã được sửa lại cú pháp cho đúng chuẩn Kotlin.
 */
data class PostModel(
    // === CÁC TRƯỜNG DỮ LIỆU CỦA BẠN (KHÔNG THAY ĐỔI) ===
    val authorId: String = "",
    val content: String = "",
    val imageUrl: String? = null,
    val fileUrl: String? = null,
    val fileName: String? = null,
    @ServerTimestamp
    val timestamp: Timestamp? = null,
    val likesCount: Long = 0,
    val commentsCount: Long = 0,
    val likedBy: List<String> = emptyList(),
    val savedBy: List<String> = emptyList(), // Giữ nguyên trường 'savedBy'
    val authorName: String = "",
    val authorAvatarUrl: String? = null,
    val contentLowercase: String = content.lowercase(),

    @get:Exclude
    var postId: String = "" // Sửa 'val' thành 'var' để có thể gán lại ID
) {
    // =================================================================
    // SỬA LỖI CÚ PHÁP: Đặt constructor phụ vào đúng vị trí trong thân class {}
    // =================================================================
    constructor() : this(
        // Cung cấp giá trị mặc định cho TẤT CẢ các trường trong constructor chính
        authorId = "",
        content = "",
        imageUrl = null,
        fileUrl = null,
        fileName = null,
        timestamp = null,
        likesCount = 0,
        commentsCount = 0,
        likedBy = emptyList(),
        savedBy = emptyList(), // Giữ nguyên trường 'savedBy'
        authorName = "",
        authorAvatarUrl = null,
        contentLowercase = "", // Quan trọng: phải có giá trị mặc định
        postId = ""
    )
}
