package com.example.study_s.data.model

import com.google.firebase.Timestamp

data class Notification(
    val notificationId: String = "",       // ID của thông báo
    val userId: String = "",               // ID của người nhận thông báo
    val actorId: String = "",              // ID của người gây ra hành động (người like, follow)
    val actorName: String? = null,         // Tên của người gây ra hành động
    val actorAvatarUrl: String? = null,    // Avatar của người gây ra hành động
    val type: String = "",                 // Loại thông báo: "like", "follow", "comment"
    val message: String = "",              // Nội dung thông báo, ví dụ: "đã bắt đầu theo dõi bạn."
    val postId: String? = null,            // ID bài viết liên quan (nếu có)
    val postImageUrl: String? = null,      // Ảnh bài viết liên quan (nếu có)
    val createdAt: Timestamp = Timestamp.now(), // Thời gian tạo
    val isRead: Boolean = false            // Đã đọc hay chưa
)
    