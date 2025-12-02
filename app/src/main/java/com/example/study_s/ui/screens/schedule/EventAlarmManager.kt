// ĐƯỜNG DẪN: app/src/main/java/com/example/study_s/ui/screens/schedule/EventAlarmManager.kt

package com.example.study_s.ui.screens.schedule

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.study_s.data.model.ScheduleModel
import java.util.Calendar

object EventAlarmManager {

    private const val DAILY_SUMMARY_REQUEST_CODE = 2024
    // ✅ Hằng số để tạo sự khác biệt cho ID của báo thức "Đúng giờ"
    private const val ON_TIME_ALARM_OFFSET = 1_000_000

    /**
     * NÂNG CẤP: Đặt cả báo thức nhắc trước VÀ báo thức đúng giờ cho một sự kiện.
     */
    fun setAlarmsForEvent(context: Context, schedule: ScheduleModel, reminderMinutes: Int) {
        // --- 1. Đặt báo thức MẶC ĐỊNH (đúng giờ) ---
        // RequestCode sẽ là hashCode của ID + một số lớn để đảm bảo không trùng
        val onTimeRequestCode = schedule.scheduleId.hashCode() + ON_TIME_ALARM_OFFSET
        val onTimeMessage = "Sự kiện '${schedule.content}' đang diễn ra."
        setSingleAlarm(
            context = context,
            schedule = schedule,
            reminderMinutes = 0, // Đặt reminderMinutes = 0 để báo thức đúng giờ
            message = onTimeMessage,
            requestCode = onTimeRequestCode
        )

        // --- 2. Đặt báo thức TÙY CHỌN (nhắc trước) ---
        // Chỉ đặt nếu người dùng chọn một khoảng thời gian > 0
        if (reminderMinutes > 0) {
            val reminderRequestCode = schedule.scheduleId.hashCode() // Giữ nguyên requestCode cũ
            val reminderMessage = "Sắp diễn ra: ${schedule.content}"
            setSingleAlarm(
                context = context,
                schedule = schedule,
                reminderMinutes = reminderMinutes,
                message = reminderMessage,
                requestCode = reminderRequestCode
            )
        }
    }

    /**
     * NÂNG CẤP: Hủy cả hai báo thức liên quan đến một sự kiện.
     */
    fun cancelAlarmsForEvent(context: Context, schedule: ScheduleModel) {
        // Hủy báo thức nhắc trước (dùng requestCode cũ)
        cancelSingleAlarm(context, schedule.scheduleId.hashCode())

        // Hủy báo thức đúng giờ (dùng requestCode đã được offset)
        cancelSingleAlarm(context, schedule.scheduleId.hashCode() + ON_TIME_ALARM_OFFSET)
    }

    /**
     * HÀM LÕI (private) để đặt một báo thức duy nhất.
     * Hàm này được tái cấu trúc từ `setAlarmForEvent` cũ của bạn.
     */
    private fun setSingleAlarm(
        context: Context,
        schedule: ScheduleModel,
        reminderMinutes: Int,
        message: String,
        requestCode: Int
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Log.e("EventAlarm", "Không có quyền SCHEDULE_EXACT_ALARM.")
            return
        }

        val calendar = Calendar.getInstance().apply {
            set(schedule.year, schedule.month - 1, schedule.day, schedule.hour, schedule.minute, 0)
            add(Calendar.MINUTE, -reminderMinutes)
        }

        if (calendar.timeInMillis < System.currentTimeMillis()) {
            Log.d("EventAlarm", "Thời gian báo thức cho '${schedule.content}' (ID: $requestCode) đã ở trong quá khứ. Bỏ qua.")
            return
        }

        val intent = Intent(context, EventAlarmReceiver::class.java).apply {
            putExtra("EVENT_ID", schedule.scheduleId)
            // ✅ Sử dụng message được truyền vào thay vì content mặc định
            putExtra("EVENT_CONTENT", message)
            putExtra("EVENT_TIME_HOUR", schedule.hour)
            putExtra("EVENT_TIME_MINUTE", schedule.minute)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode, // ✅ Sử dụng requestCode được truyền vào
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            Log.d("EventAlarm", "✅ Đã đặt báo thức (ID: $requestCode) cho '${schedule.content}' lúc: ${calendar.time}")
        } catch (e: SecurityException) {
            Log.e("EventAlarm", "Lỗi SecurityException khi đặt báo thức (ID: $requestCode)", e)
        }
    }

    /**
     * HÀM LÕI (private) để hủy một báo thức duy nhất.
     * Hàm này được tái cấu trúc từ `cancelAlarmForEvent` cũ của bạn.
     */
    private fun cancelSingleAlarm(context: Context, requestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, EventAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode, // ✅ Sử dụng requestCode để tìm đúng báo thức cần hủy
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d("EventAlarm", "✅ Đã HỦY báo thức (ID: $requestCode).")
        }
    }


    // --- HÀM TÓM TẮT CUỐI NGÀY GIỮ NGUYÊN, KHÔNG THAY ĐỔI ---
    fun scheduleDailySummaryAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, DailySummaryReceiver::class.java)

        val alarmUp = PendingIntent.getBroadcast(context, DAILY_SUMMARY_REQUEST_CODE, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE) != null
        if (alarmUp) {
            Log.d("EventAlarm", "Báo thức tóm tắt cuối ngày đã được đặt. Bỏ qua.")
            return
        }

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 19)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }

        if (Calendar.getInstance().after(calendar)) {
            Log.d("EventAlarm", "Đã qua 19:00, không đặt báo thức tóm tắt hôm nay.")
            return
        }

        val pendingIntent = PendingIntent.getBroadcast(context, DAILY_SUMMARY_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            Log.d("EventAlarm", "✅ Đã đặt báo thức tóm tắt cuối ngày vào lúc 19:00.")
        } catch (e: SecurityException) {
            Log.e("EventAlarm", "Lỗi SecurityException khi đặt báo thức tóm tắt", e)
        }
    }
}
