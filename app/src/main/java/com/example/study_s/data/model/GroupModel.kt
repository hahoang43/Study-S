package com.example.study_s.data.model

import com.google.firebase.firestore.DocumentId
import java.util.Date

/**
 * Represents a study group.
 *
 * This data class is structured to match the data stored in Firestore, preventing
 * deserialization crashes. The `createdAt` field is a Long to match the existing
 * timestamp format in the database.
 */
data class Group(
    @DocumentId
    val groupId: String = "",
    val groupName: String = "",
    val groupNameLowercase: String = "",
    val description: String = "",
    val subject: String = "",
    val members: List<String> = emptyList(),
    val bannedUsers: List<String> = emptyList(),
    val createdBy: String = "",
    val searchKeywords: List<String> = emptyList(),
    val avatarUrl: String? = null,
    val createdAt: Date? = null,
)
