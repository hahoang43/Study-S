// file: data/model/User.kt
package com.example.study_s.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class User(    val userId: String = "",
                    val name: String = "",
                    val email: String = "",
                    val avatarUrl: String? = null,
                    val bio: String? = "",
                    val followerCount: Int = 0,
                    val followingCount: Int = 0,
                    @ServerTimestamp // Tự động gán thời gian tạo trên server
                    val createdAt: Date? = null,
                    val nameLowercase: String = name.lowercase(),
                    val searchKeywords: List<String> = emptyList()
) {
    // Constructor này rất cần thiết để Firestore có thể chuyển đổi dữ liệu thành đối tượng User.
    constructor() : this(
        userId = "",
        name = "",
        email = "",
        avatarUrl = null,
        bio = "",
        createdAt = null,
        nameLowercase = "" ,// Thêm giá trị mặc định cho trường mới
        searchKeywords = emptyList()
    )
}

