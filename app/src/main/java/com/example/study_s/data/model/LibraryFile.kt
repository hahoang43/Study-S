package com.example.study_s.data.model

data class LibraryFile(
    val fileName: String = "",
    val fileUrl: String = "",
    val mimeType: String = "",
    val uploaderId: String = "",
    val uploaderName: String = "",
    val uploadedAt: Long = System.currentTimeMillis() // Thêm trường thời gian tải lên
)
