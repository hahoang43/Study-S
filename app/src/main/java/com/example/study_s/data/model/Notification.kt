package com.example.study_s.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class Notification(
    @get:PropertyName("notificationId") @set:PropertyName("notificationId")
    var notificationId: String = "",

    val userId: String = "",
    val actorId: String = "",
    val actorName: String? = null,
    val actorAvatarUrl: String? = null,
    val type: String = "",
    val message: String = "",
    val postId: String? = null,
    val postImageUrl: String? = null,
    val createdAt: Timestamp = Timestamp.now(),

    @get:PropertyName("isRead") @set:PropertyName("isRead")
    var isRead: Boolean = false
)
