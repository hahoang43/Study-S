package com.example.study_s

import android.app.Application
import com.cloudinary.android.MediaManager

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Khởi tạo MediaManager với cấu hình chính xác cho unsigned uploads.
        val config = mapOf(
            "cloud_name" to "dzhiudnhu",
            "secure" to true
        )
        MediaManager.init(this, config)
    }
}
