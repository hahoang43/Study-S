
package com.example.study_s.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Message(
    val senderId: String = "",
    val content: String = "",
    @ServerTimestamp
    val timestamp: Date? = null
)
