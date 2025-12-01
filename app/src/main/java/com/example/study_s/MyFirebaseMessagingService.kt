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
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.util.Random

private const val FCM_DEBUG_TAG = "FCM_DEBUG_SERVICE"

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d(FCM_DEBUG_TAG, "--- onMessageReceived CALLED! ---")
        Log.d(FCM_DEBUG_TAG, "Data payload: " + remoteMessage.data.toString())

        val title = remoteMessage.notification?.title
        val body = remoteMessage.notification?.body

        if (title != null && body != null) {
            // 1. Luôn hiển thị thông báo đẩy
            showNotification(title, body)

            // 2. Kiểm tra xem có lệnh "lưu vào lịch sử" không
            val shouldSave = remoteMessage.data["saveToHistory"]
            if (shouldSave == "true") {
                Log.d(FCM_DEBUG_TAG, "Condition MET. Attempting to save to history...")
                // 3. Gọi hàm lưu vào Firestore
                saveNotificationToHistory(title, body)
            }
        }
    }

    /**
     * ✅ SỬA ĐỔI HÀM NÀY ĐỂ GHI VÀO COLLECTION GỐC "notifications"
     */
    private fun saveNotificationToHistory(title: String, body: String) {
        val currentUserId = Firebase.auth.currentUser?.uid

        if (currentUserId == null) {
            Log.e(FCM_DEBUG_TAG, "SAVE FAILED: userId is NULL. User must be logged in to receive history.")
            return
        }

        Log.d(FCM_DEBUG_TAG, "SAVE ATTEMPT: Saving for userId: $currentUserId")

        // Tạo một object Notification mới để lưu.
        // Cấu trúc này phải khớp với cách NotificationRepository đang lưu thông báo like/comment.
        val notificationPayload = hashMapOf(
            "userId" to currentUserId,             // ✅ QUAN TRỌNG: Thêm userId để Repository có thể query
            "type" to "SYSTEM_ADMIN",              // Loại thông báo mới
            "title" to title,                      // Title từ thông báo đẩy
            "body" to body,                        // Body từ thông báo đẩy
            "message" to body,                     // Dùng body làm message chính để UI hiển thị
            "isRead" to false,
            "createdAt" to FieldValue.serverTimestamp(), // Dùng timestamp của server để sắp xếp
            // Các trường khác có thể để null hoặc giá trị mặc định cho thông báo hệ thống
            "actorId" to null,
            "actorName" to "Study-S", // Tên hệ thống
            "actorAvatarUrl" to null, // Bạn có thể đặt URL avatar mặc định của app ở đây
            "postId" to null,
            "postImageUrl" to null
        )

        // ✅ SỬA ĐỔI: Ghi vào collection gốc "notifications"
        Firebase.firestore.collection("notifications")
            .add(notificationPayload) // Dùng add() để Firestore tự tạo ID cho document
            .addOnSuccessListener { documentReference ->
                Log.d(FCM_DEBUG_TAG, ">>> SUCCESS! Admin Notification saved to ROOT 'notifications' collection. ID: ${documentReference.id} <<<")
            }
            .addOnFailureListener { e ->
                Log.e(FCM_DEBUG_TAG, ">>> FIRESTORE SAVE FAILED! Error: ", e)
            }
    }

    // --- CÁC HÀM CÒN LẠI GIỮ NGUYÊN, KHÔNG CẦN THAY ĐỔI ---

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM_TOKEN", "New FCM Token generated: $token")
        sendTokenToServer(token)
    }

    private fun sendTokenToServer(token: String) {
        Firebase.auth.currentUser?.uid?.let { userId ->
            val userDocRef = Firebase.firestore.collection("users").document(userId)
            userDocRef.update("fcmToken", token)
                .addOnSuccessListener {
                    Log.d("FCM_TOKEN", "FCM token updated successfully for user: $userId")
                }
                .addOnFailureListener { e ->
                    Log.e("FCM_TOKEN", "Error updating FCM token", e)
                }
        }
    }

    private fun showNotification(title: String, body: String) {
        val channelId = "default_notification_channel"
        val channelName = "Thông báo chung"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Thay bằng icon của bạn
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(this)) {
            if (ActivityCompat.checkSelfPermission(this@MyFirebaseMessagingService, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
            val notificationId = Random().nextInt()
            notify(notificationId, builder.build())
            Log.d(FCM_DEBUG_TAG, "Notification shown with ID: $notificationId")
        }
    }
}