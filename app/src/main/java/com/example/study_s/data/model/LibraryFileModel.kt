package com.example.study_s.data.model

import com.google.firebase.firestore.DocumentId

data class LibraryFile(
    @DocumentId val id: String = "",
    val publicId: String = "",
    val fileName: String = "",
    val fileUrl: String = "",
    val mimeType: String = "",
    val uploaderId: String = "",
    val uploaderName: String = "",
    val uploadedAt: Long = System.currentTimeMillis()
)
