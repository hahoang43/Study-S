// ĐƯỜNG DẪN: com/example/study_s/MyFirebaseMessagingService.kt

package com.example.study_s

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.util.Random

class MyFirebaseMessagingService : FirebaseMessagingService() {

    /**
     * Được gọi khi có tin nhắn FCM mới đến từ server.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Dữ liệu payload của thông báo được gửi từ server/app khác
        val title = remoteMessage.data["title"]
        val body = remoteMessage.data["body"]

        Log.d("FCM_RECEIVE", "Received notification: Title='$title', Body='$body'")

        if (title != null && body != null) {
            showNotification(title, body)
        }
    }

    /**
     * Được gọi khi Firebase cấp một token mới hoặc token hiện tại được làm mới.
     * Đây chính là "địa chỉ nhà" của thiết bị.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM_TOKEN", "New FCM Token generated: $token")
        // Gửi token này lên Firestore để lưu lại cho người dùng đang đăng nhập.
        sendTokenToServer(token)
    }

    private fun sendTokenToServer(token: String) {
        // Chỉ cập nhật token nếu có người dùng đang đăng nhập
        Firebase.auth.currentUser?.uid?.let { userId ->
            val userDocRef = Firebase.firestore.collection("users").document(userId)
            userDocRef.update("fcmToken", token)
                .addOnSuccessListener {
                    Log.d("FCM_TOKEN", "FCM token updated successfully on Firestore for user: $userId")
                }
                .addOnFailureListener { e ->
                    Log.e("FCM_TOKEN", "Error updating FCM token on Firestore", e)
                }
        }
    }

    private fun showNotification(title: String, body: String) {
        val channelId = "default_notification_channel"
        val channelName = "Thông báo chung"

        // Từ Android 8.0 (API 26) trở lên, thông báo phải thuộc về một "Channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH // Ưu tiên cao để thông báo hiện lên
            ).apply {
                description = "Kênh cho các thông báo chung của ứng dụng"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Xây dựng giao diện của thông báo
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // THAY BẰNG ICON THÔNG BÁO CỦA BẠN
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true) // Thông báo sẽ tự biến mất khi người dùng nhấn vào

        // Hiển thị thông báo
        with(NotificationManagerCompat.from(this)) {
            // Kiểm tra lại quyền trước khi hiển thị trên Android 13+
            if (ActivityCompat.checkSelfPermission(this@MyFirebaseMessagingService, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // Nếu không có quyền, không làm gì cả. Việc xin quyền do Activity đảm nhiệm.
                Log.w("FCM_SHOW", "Cannot show notification due to missing permission.")
                return
            }
            // Dùng ID ngẫu nhiên để các thông báo không ghi đè lên nhau
            val notificationId = Random().nextInt()
            notify(notificationId, builder.build())
            Log.d("FCM_SHOW", "Notification shown with ID: $notificationId")
        }
    }
}
