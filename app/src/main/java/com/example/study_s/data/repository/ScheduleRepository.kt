// ĐƯỜNG DẪN: app/src/main/java/com/example/study_s/data/repository/ScheduleRepository.kt

package com.example.study_s.data.repository

import android.util.Log
import com.example.study_s.data.model.ScheduleModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.*

class ScheduleRepository {

    private val db = FirebaseFirestore.getInstance().collection("schedules")
    private val userId: String? get() = FirebaseAuth.getInstance().currentUser?.uid

    suspend fun getSchedulesForMonth(year: Int, month: Int): List<ScheduleModel> {
        val uid = userId ?: return emptyList()
        return try {
            db.whereEqualTo("userId", uid)
                .whereEqualTo("year", year)
                .whereEqualTo("month", month)
                .get().await()
                .documents.mapNotNull { doc ->
                    doc.toObject(ScheduleModel::class.java)?.copy(scheduleId = doc.id)
                }
        } catch (e: Exception) {
            Log.e("ScheduleRepo", "Lỗi lấy lịch học: ${e.message}")
            emptyList()
        }
    }

    suspend fun addSchedule(schedule: ScheduleModel): String {
        return try {
            val documentReference = db.add(schedule).await()
            documentReference.id
        } catch (e: Exception) {
            Log.e("ScheduleRepo", "Lỗi thêm lịch học: ${e.message}")
            ""
        }
    }

    suspend fun deleteSchedule(scheduleId: String) {
        try {
            db.document(scheduleId).delete().await()
        } catch (e: Exception) {
            Log.e("ScheduleRepo", "Lỗi xóa lịch học: ${e.message}")
        }
    }

    suspend fun markScheduleForDailySummary(scheduleId: String) {
        try {
            db.document(scheduleId).update("needsDailySummary", true).await()
        } catch (e: Exception) {
            Log.e("ScheduleRepo", "Lỗi đánh dấu nhắc chung: ${e.message}")
        }
    }

    suspend fun getSchedulesForDailySummary(): List<ScheduleModel> {
        val uid = userId ?: return emptyList()
        val today = Calendar.getInstance()
        return try {
            db.whereEqualTo("userId", uid)
                .whereEqualTo("year", today.get(Calendar.YEAR))
                .whereEqualTo("month", today.get(Calendar.MONTH) + 1)
                .whereEqualTo("day", today.get(Calendar.DAY_OF_MONTH))
                .whereEqualTo("needsDailySummary", true)
                .get().await()
                .toObjects(ScheduleModel::class.java)
        } catch (e: Exception) {
            Log.e("ScheduleRepo", "Lỗi lấy lịch học tóm tắt: ${e.message}")
            emptyList()
        }
    }
}
