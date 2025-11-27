
package com.example.study_s.data.model
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Chat(
    @DocumentId val id: String = "", // <-- THÊM DÒNG NÀY
    val members: List<String> = emptyList(),
    val lastMessage: Message? = null,
    @ServerTimestamp val createdAt: Date? = null // <-- THÊM DÒNG NÀY
)
