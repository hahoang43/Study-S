// ĐƯỜNG DẪN MỚI: app/src/main/java/com/example/study_s/ui/screens/schedule/EventAlarmReceiver.kt

package com.example.study_s.ui.screens.schedule // ✅ ĐÃ THAY ĐỔI PACKAGE

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import com.example.study_s.MainActivity
import com.example.study_s.R
import com.example.study_s.ui.navigation.Routes
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
class EventAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val eventContent = intent.getStringExtra("EVENT_CONTENT") ?: "một lịch học"
        val eventHour = intent.getIntExtra("EVENT_TIME_HOUR", 0)
        val eventMinute = intent.getIntExtra("EVENT_TIME_MINUTE", 0)
        val timeString = String.format("%02d:%02d", eventHour, eventMinute)
        val pendingResult: PendingResult = goAsync()

        Log.d("EventAlarmReceiver", "--- Receiver được kích hoạt cho: $eventContent ---")

        val notificationTitle = "Nhắc nhở lịch học sắp tới"
        val notificationMessage = "Bạn có lịch học '${eventContent}' vào lúc ${timeString}."

        sendNotification(context, notificationTitle, notificationMessage)
        // ✅ GỌI HÀM LƯU VÀO FIRESTORE
        CoroutineScope(Dispatchers.IO).launch {
            try {
                saveActivityToFirestore(notificationMessage)
            } finally {
                pendingResult.finish() // Báo cho hệ thống đã xử lý xong
            }
        }
    }


    private fun sendNotification(context: Context, title: String, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "event_reminder_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Nhắc Nhở Sự Kiện", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Thông báo cho các lịch học sắp diễn ra"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val deepLinkIntent = Intent(Intent.ACTION_VIEW, "app://example.study_s/${Routes.Schedule}".toUri(), context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 1, deepLinkIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_calendar) // Thay bằng icon của bạn
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
        Log.d("EventAlarmReceiver", "=> Đã gửi thông báo hệ thống.")
    }
    // ✅ THÊM HÀM NÀY
    private suspend fun saveActivityToFirestore(message: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        try {
            val db = FirebaseFirestore.getInstance()
            val notificationData = hashMapOf(
                "userId" to userId,
                "actorId" to "system_reminder", // ID đặc biệt cho hệ thống
                "actorName" to "Study_S",      // Tên hiển thị của hệ thống
                "actorAvatarUrl" to null,      // Hoặc một URL logo của bạn
                "type" to "schedule_reminder", // ✅ LOẠI THÔNG BÁO QUAN TRỌNG
                "message" to message,
                "postId" to null,
                "postImageUrl" to null,
                "createdAt" to Timestamp.now(),
                "isRead" to false
            )
            db.collection("notifications").add(notificationData).await()
            Log.d("EventAlarmReceiver", "=> Đã lưu hoạt động nhắc nhở vào Firestore.")
        } catch (e: Exception) {
            Log.e("EventAlarmReceiver", "!!! Lỗi khi lưu hoạt động: ${e.message}", e)
        }
    }
}

