// ĐƯỜNG DẪN: app/src/main/java/com/example/study_s/data/repository/ScheduleRepository.kt

package com.example.study_s.data.repository

import android.util.Log
import com.example.study_s.data.model.ScheduleModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.*
import com.google.firebase.firestore.SetOptions

class ScheduleRepository {

    private val usersCollection = FirebaseFirestore.getInstance().collection("users")
    private val userId: String? get() = FirebaseAuth.getInstance().currentUser?.uid

    // Hàm này phải được sửa đổi để truy vấn vào sub-collection
    suspend fun getSchedulesForMonth(year: Int, month: Int): List<ScheduleModel> {
        val uid = userId ?: return emptyList()
        return try {
            usersCollection.document(uid).collection("schedules") // Truy vấn vào sub-collection
                .whereEqualTo("year", year)
                .whereEqualTo("month", month)
                .get().await()
                .documents.mapNotNull { doc ->
                    // Đảm bảo gán ID của document vào đối tượng ScheduleModel
                    doc.toObject(ScheduleModel::class.java)?.copy(scheduleId = doc.id)
                }
        } catch (e: Exception) {
            Log.e("ScheduleRepo", "Lỗi lấy lịch học: ${e.message}")
            emptyList()
        }
    }

    // ✅ 3. THAY THẾ HOÀN TOÀN `addSchedule` BẰNG HÀM "THÔNG MINH" NÀY
    /**
     * Tự động thêm mới một lịch nếu scheduleId rỗng, hoặc cập nhật nếu đã có.
     * @return ID của lịch (dù là mới hay cũ) để ViewModel có thể sử dụng.
     */
    suspend fun saveOrUpdateSchedule(schedule: ScheduleModel): String {
        val uid = userId ?: return "" // Không làm gì nếu chưa đăng nhập

        return if (schedule.scheduleId.isBlank()) {
            // === TRƯỜNG HỢP 1: THÊM MỚI (vì scheduleId rỗng) ===
            try {
                // Lấy sub-collection của user hiện tại
                val schedulesCollection = usersCollection.document(uid).collection("schedules")
                // Tạo một document rỗng để lấy ID
                val newDocRef = schedulesCollection.document()
                // Gán ID mới này vào đối tượng schedule trước khi lưu
                val finalSchedule = schedule.copy(scheduleId = newDocRef.id)
                // Lưu đối tượng hoàn chỉnh vào document
                newDocRef.set(finalSchedule).await()
                Log.d("ScheduleRepo", "Đã thêm lịch mới với ID: ${newDocRef.id}")
                newDocRef.id // Trả về ID mới
            } catch (e: Exception) {
                Log.e("ScheduleRepo", "Lỗi thêm lịch học: ${e.message}", e)
                ""
            }
        } else {
            // === TRƯỜNG HỢP 2: CẬP NHẬT (vì scheduleId đã có) ===
            try {
                // Tìm đến đúng document cần cập nhật bằng ID
                val docRef = usersCollection.document(uid).collection("schedules").document(schedule.scheduleId)
                // Dùng .set() với SetOptions.merge() để chỉ cập nhật các trường đã thay đổi
                // Điều này tránh ghi đè và làm mất các trường khác nếu có
                docRef.set(schedule, SetOptions.merge()).await()
                Log.d("ScheduleRepo", "Đã cập nhật lịch với ID: ${schedule.scheduleId}")
                schedule.scheduleId // Trả về ID cũ
            } catch (e: Exception) {
                Log.e("ScheduleRepo", "Lỗi cập nhật lịch học: ${e.message}", e)
                ""
            }
        }
    }

    // Hàm này cũng cần được sửa để hoạt động với cấu trúc sub-collection
    suspend fun deleteSchedule(scheduleId: String) {
        val uid = userId ?: return
        if (scheduleId.isBlank()) return // Không xóa nếu không có ID
        try {
            usersCollection.document(uid).collection("schedules").document(scheduleId).delete().await()
            Log.d("ScheduleRepo", "Đã xóa lịch: $scheduleId")
        } catch (e: Exception) {
            Log.e("ScheduleRepo", "Lỗi xóa lịch học: ${e.message}")
        }
    }


    suspend fun markScheduleForDailySummary(scheduleId: String) {
        val uid = userId ?: return
        try {
            usersCollection.document(uid).collection("schedules").document(scheduleId)
                .update("needsDailySummary", true).await()
        } catch (e: Exception) {
            Log.e("ScheduleRepo", "Lỗi đánh dấu nhắc chung: ${e.message}")
        }
    }

    suspend fun getSchedulesForDailySummary(): List<ScheduleModel> {
        val uid = userId ?: return emptyList()
        val today = Calendar.getInstance()
        return try {
            // ✅ 1. SỬA LẠI ĐƯỜNG DẪN TRUY VẤN
            // Đi vào collection con của user hiện tại
            usersCollection.document(uid).collection("schedules")
                .whereEqualTo("year", today.get(Calendar.YEAR))
                .whereEqualTo("month", today.get(Calendar.MONTH) + 1)
                .whereEqualTo("day", today.get(Calendar.DAY_OF_MONTH))
                .whereEqualTo("needsDailySummary", true)
                .get().await()
                // ✅ 2. DÙNG .mapNotNull ĐỂ LẤY ID
                // Thay thế .toObjects() bằng cách duyệt qua từng document
                .documents.mapNotNull { doc ->
                    // Chuyển document thành đối tượng ScheduleModel và GÁN ID vào
                    doc.toObject(ScheduleModel::class.java)?.copy(scheduleId = doc.id)
                }
        } catch (e: Exception) {
            Log.e("ScheduleRepo", "Lỗi lấy lịch học tóm tắt hằng ngày: ${e.message}", e)
            emptyList()
        }
    }
}
