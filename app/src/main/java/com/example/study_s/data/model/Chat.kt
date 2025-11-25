
package com.example.study_s.data.model

data class Chat(
    val members: List<String> = emptyList(),
    val lastMessage: Message? = null
)
