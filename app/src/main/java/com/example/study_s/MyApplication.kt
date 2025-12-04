package com.example.study_s

import android.app.Application
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.cloudinary.android.MediaManager
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MyApplication : Application(), LifecycleEventObserver {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate() {
        super.onCreate()

        // Khởi tạo Firebase
        FirebaseApp.initializeApp(this)

        // Đăng ký observer để theo dõi vòng đời của ứng dụng
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        // Cấu hình Firebase App Check
        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        firebaseAppCheck.installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance()
        )

        // Cấu hình Cloudinary
        val config = mapOf(
            "cloud_name" to "dzhiudnhu",
            "api_key" to "877213433711958",
            "api_secret" to "yX_V2OZ6JB0GsYPwdkPKO8Aiinc",
            "secure" to true
        )
        MediaManager.init(this, config)
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_START -> {
                // Ứng dụng được mở (chuyển sang nền trước)
                updateUserStatus(true)
            }
            Lifecycle.Event.ON_STOP -> {
                // Ứng dụng bị đóng hoặc chuyển xuống nền
                updateUserStatus(false)
            }
            else -> {
                // Không làm gì cho các sự kiện khác
            }
        }
    }

    private fun updateUserStatus(isOnline: Boolean) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val userRef = firestore.collection("users").document(userId)
            userRef.update("online", isOnline)
                .addOnSuccessListener {
                    // Tùy chọn: Ghi log khi cập nhật thành công
                }
                .addOnFailureListener {
                    // Tùy chọn: Ghi log khi cập nhật thất bại
                }
        }
    }
}
