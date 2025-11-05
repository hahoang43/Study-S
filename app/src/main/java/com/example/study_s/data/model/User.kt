// file: data/model/User.kt
package com.example.study_s.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class User(    val userId: String = "",
                    val name: String = "",
                    val email: String = "",
                    val avatarUrl: String? = null,
                    val bio: String? = "",
                    @ServerTimestamp // Tự động gán thời gian tạo trên server
                    val createdAt: Date? = null
)
