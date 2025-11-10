// BẮT ĐẦU FILE: MyApplication.kt
package com.example.study_s

import android.app.Application
import com.cloudinary.android.MediaManager
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // =========================================================================
        // == PHẦN 1: KHỞI TẠO FIREBASE VÀ APP CHECK (PHẦN BỊ THIẾU) ==
        // =========================================================================
        // Khởi tạo Firebase trước
        FirebaseApp.initializeApp(this)

        // Lấy thực thể FirebaseAppCheck
        val firebaseAppCheck = FirebaseAppCheck.getInstance()

        // Cài đặt nhà cung cấp Debug. Đây là dòng lệnh quan trọng nhất.
        firebaseAppCheck.installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance()
        )

        // =========================================================================
        // == PHẦN 2: KHỞI TẠO CLOUDINARY (PHẦN BẠN ĐÃ CÓ) ==
        // =========================================================================
        val config = mapOf(
            "cloud_name" to "dzhiudnhu",
            "secure" to true
        )
        // Lưu ý: Không cần gọi MediaManager.init() nữa nếu bạn đã cấu hình nó trong ViewModel
        // hoặc các phần khác. Nhưng để đây cũng không gây hại.
        // Nếu bạn đã có MediaManager.init() ở nơi khác, có thể xóa dòng dưới.
        MediaManager.init(this, config)
    }
}
// KẾT THÚC FILE
