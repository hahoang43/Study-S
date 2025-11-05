package com.example.study_s.data.model

data class Group(
    val groupId: String = "",
    val groupName: String = "",
    val description: String = "",
    val members: List<String> = emptyList(),
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
