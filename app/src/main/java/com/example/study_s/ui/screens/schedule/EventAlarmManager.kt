// ĐƯỜNG DẪN MỚI: app/src/main/java/com/example/study_s/ui/screens/schedule/EventAlarmManager.kt

package com.example.study_s.ui.screens.schedule // ✅ ĐÃ THAY ĐỔI PACKAGE

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

    fun setAlarmForEvent(context: Context, schedule: ScheduleModel, reminderMinutes: Int) {
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
            Log.d("EventAlarm", "Thời gian báo thức cho '${schedule.content}' đã ở trong quá khứ. Bỏ qua.")
            return
        }

        val intent = Intent(context, EventAlarmReceiver::class.java).apply {
            putExtra("EVENT_ID", schedule.scheduleId)
            putExtra("EVENT_CONTENT", schedule.content)
            putExtra("EVENT_TIME_HOUR", schedule.hour)
            putExtra("EVENT_TIME_MINUTE", schedule.minute)
        }

        val requestCode = schedule.scheduleId.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            Log.d("EventAlarm", "✅ Đã đặt báo thức chính xác cho '${schedule.content}' lúc: ${calendar.time}")
        } catch (e: SecurityException) {
            Log.e("EventAlarm", "Lỗi SecurityException khi đặt báo thức chính xác", e)
        }
    }

    fun cancelAlarmForEvent(context: Context, schedule: ScheduleModel) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, EventAlarmReceiver::class.java)
        val requestCode = schedule.scheduleId.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d("EventAlarm", "✅ Đã HỦY báo thức chính xác cho '${schedule.content}'.")
        }
    }

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
