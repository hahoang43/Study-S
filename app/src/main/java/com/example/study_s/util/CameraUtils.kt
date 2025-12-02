package com.example.study_s.util

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun createImageFile(context: Context): Uri? {
    try {
        // Tạo tên file ảnh độc nhất dựa trên thời gian
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"

        // Lấy thư mục lưu trữ ảnh riêng của ứng dụng
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        // Tạo file tạm
        val imageFile = File.createTempFile(
            imageFileName, /* prefix */
            ".jpg",       /* suffix */
            storageDir    /* directory */
        )

        // Lấy Uri an toàn cho file bằng FileProvider
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider", // Phải khớp với 'authorities' trong Manifest
            imageFile
        )
    } catch (ex: Exception) {
        // Lỗi xảy ra khi tạo file
        ex.printStackTrace()
        return null
    }
}