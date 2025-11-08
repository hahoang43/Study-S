package com.example.study_s.data.model

data class LibraryFile(
    val fileId: String = "",
    val fileName: String = "",
    val fileUrl: String = "", // URL Cloudinary sau khi upload
    val mimeType: String = "",
    val uploaderId: String = "",
    val uploaderName: String = "",
    val uploadedAt: Long = System.currentTimeMillis()
)