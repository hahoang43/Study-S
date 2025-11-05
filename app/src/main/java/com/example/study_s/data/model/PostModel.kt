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
    // Trường này không lưu trong Firestore nhưng rất hữu ích ở phía client
    @get:Exclude var postId: String = ""
) {
    // Thêm một constructor trống để Firestore có thể tự động chuyển đổi DocumentSnapshot thành đối tượng Post
    constructor() : this("", "", null, null, null, null, 0, 0, "")
}
