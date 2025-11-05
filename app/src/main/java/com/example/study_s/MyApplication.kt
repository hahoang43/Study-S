// File: MyApplication.kt
package com.example.study_s

import android.app.Application
import com.cloudinary.android.MediaManager

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Khởi tạo MediaManager với thông tin Cloudinary của bạn
        val config = mapOf(
            "cloud_name" to "dpx9pegbu",
            "api_key" to "556966687289974",
            "api_secret" to "G3agYfxjmeGMBbx3w30JfsxL128"
        )
        MediaManager.init(this, config)
    }
}
    