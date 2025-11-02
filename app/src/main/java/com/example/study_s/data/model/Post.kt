package com.example.study_s.data.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

/**
 * Lớp dữ liệu đại diện cho một bài đăng.
 * @param authorId ID của người dùng tạo bài đăng (từ Firebase Auth).
 * @param content Nội dung văn bản của bài đăng.
 * @param imageUrl URL của hình ảnh đính kèm (có thể null).
 * @param timestamp Thời gian bài đăng được tạo, tự động gán bởi server.
 * @param likesCount Số lượt thích, mặc định là 0.
 * @param commentsCount Số bình luận, mặc định là 0.
 * @param postId ID của tài liệu trong Firestore, sẽ được gán sau khi lấy dữ liệu.
 */
data class Post(
    val authorId: String = "",
    val content: String = "",
    val imageUrl: String? = null,
    @ServerTimestamp // Annotation này giúp Firebase tự động điền thời gian của server
    val timestamp: Timestamp? = null,
    val likesCount: Long = 0,
    val commentsCount: Long = 0,
    // Trường này không lưu trong Firestore nhưng rất hữu ích ở phía client
    @get:Exclude val postId: String = ""
) {
    // Thêm một constructor trống để Firestore có thể tự động chuyển đổi DocumentSnapshot thành đối tượng Post
    constructor() : this("", "", null, null, 0, 0, "")
}
