// ĐƯỜNG DẪN MỚI: app/src/main/java/com/example/study_s/ui/screens/schedule/DailySummaryReceiver.kt

package com.example.study_s.ui.screens.schedule // ✅ ĐÃ THAY ĐỔI PACKAGE

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.study_s.R
import com.example.study_s.data.repository.ScheduleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.app.PendingIntent
import androidx.core.net.toUri
import com.example.study_s.MainActivity
import com.example.study_s.ui.navigation.Routes
class DailySummaryReceiver : BroadcastReceiver() {

    private val DAILY_SUMMARY_REQUEST_CODE = 2024

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val repository = ScheduleRepository()

        Log.d("DailySummary", "--- Receiver tóm tắt cuối ngày được kích hoạt ---")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val eventsToSummarize = repository.getSchedulesForDailySummary()
                if (eventsToSummarize.isNotEmpty()) {
                    val count = eventsToSummarize.size
                    val title = "Tóm tắt lịch học hôm nay"
                    val message = "Bạn có $count lịch học đã đặt lời nhắc chung. Đừng quên nhé!"

                    sendSummaryNotification(context, title, message)
                    Log.d("DailySummary", "Phát hiện $count lịch học. Đã gửi thông báo tóm tắt.")
                } else {
                    Log.d("DailySummary", "Không có lịch học nào cần tóm tắt hôm nay.")
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun sendSummaryNotification(context: Context, title: String, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "daily_summary_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Tóm Tắt Hàng Ngày", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }
        val deepLinkIntent = Intent(
            Intent.ACTION_VIEW,
            "app://example.study_s/${Routes.Schedule}".toUri(), // Địa chỉ của màn hình Lịch
            context,
            MainActivity::class.java
        )

        // ✅ BƯỚC 2: TẠO PENDINGINTENT BỌC LẤY DEEP LINK
        val pendingIntent = PendingIntent.getActivity(
            context,
            2, // Dùng một requestCode khác để tránh xung đột (ví dụ: 2)
            deepLinkIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_calendar) // Thay bằng icon của bạn
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        notificationManager.notify(DAILY_SUMMARY_REQUEST_CODE, builder.build())
        Log.d("DailySummary", "=> Đã gửi thông báo tóm tắt CÓ THỂ NHẤN VÀO.")
    }
}
