package com.example.study_s.viewmodel

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.study_s.data.model.ScheduleModel
import com.example.study_s.data.repository.ScheduleRepository
import com.example.study_s.ui.screens.schedule.EventAlarmManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*

// Enum cho các tùy chọn trong hộp thoại, đặt ở đây cho tiện
enum class ReminderOption(val minutes: Int, val description: String) {
    BEFORE_5_MIN(5, "Trước 5 phút"),
    BEFORE_10_MIN(10, "Trước 10 phút"),
    BEFORE_30_MIN(30, "Trước 30 phút"),
    DAILY_SUMMARY(-1, "Nhắc chung vào cuối ngày");
}

class ScheduleViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ScheduleRepository()

    // State cho năm và tháng đang hiển thị
    private val _currentYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val currentYear = _currentYear.asStateFlow()

    private val _currentMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH)) // Tháng bắt đầu từ 0
    val currentMonth = _currentMonth.asStateFlow()

    // State cho danh sách các sự kiện của tháng
    private val _events = MutableStateFlow<List<ScheduleModel>>(emptyList())
    val events = _events.asStateFlow()

    // State để điều khiển việc hiển thị hộp thoại chọn lời nhắc
    private val _showReminderDialog = mutableStateOf<ScheduleModel?>(null)
    val showReminderDialog: State<ScheduleModel?> = _showReminderDialog

    init {
        // Tải dữ liệu lần đầu khi ViewModel được tạo
        loadSchedulesForCurrentMonth()
    }

    // Tải dữ liệu từ Repository
    fun loadSchedulesForCurrentMonth() {
        viewModelScope.launch {
            _events.value = repository.getSchedulesForMonth(_currentYear.value, _currentMonth.value + 1)
        }
    }

    // Thay đổi tháng
    fun changeMonth(newMonth: Int, newYear: Int) {
        _currentYear.value = newYear
        _currentMonth.value = newMonth
        loadSchedulesForCurrentMonth()
    }

    // Hàm này được gọi từ UI khi người dùng nhấn nút "Lưu" hoặc "Cập nhật" trong Dialog
    fun onSaveOrUpdateClicked(schedule: ScheduleModel) {
        // Hiển thị hộp thoại chọn lời nhắc thay vì lưu ngay
        _showReminderDialog.value = schedule
    }

    fun dismissReminderDialog() {
        _showReminderDialog.value = null
    }

    // Hàm được gọi khi người dùng chọn một tùy chọn trong hộp thoại lời nhắc
    fun processScheduleWithReminder(schedule: ScheduleModel, option: ReminderOption) {
        viewModelScope.launch {
            // Bước 1: Thêm/Cập nhật lịch học vào Firestore
            val finalScheduleId = repository.saveOrUpdateSchedule(schedule) // Repository sẽ tự xử lý thêm mới hoặc cập nhật
            if (finalScheduleId.isNotEmpty()) {
                val processedSchedule = schedule.copy(scheduleId = finalScheduleId)

                // Bước 2: Dựa vào lựa chọn để đặt báo thức
                if (option.minutes > 0) {
                    // Trường hợp đặt báo thức chính xác
                    EventAlarmManager.setAlarmForEvent(
                        context = getApplication(),
                        schedule = processedSchedule,
                        reminderMinutes = option.minutes
                    )
                } else {
                    // Trường hợp "Nhắc chung vào cuối ngày"
                    repository.markScheduleForDailySummary(processedSchedule.scheduleId)
                    EventAlarmManager.scheduleDailySummaryAlarm(getApplication())
                }
                // Tải lại danh sách để UI được cập nhật
                loadSchedulesForCurrentMonth()
            }
            // Ẩn hộp thoại đi
            dismissReminderDialog()
        }
    }

    fun deleteSchedule(schedule: ScheduleModel) {
        viewModelScope.launch {
            repository.deleteSchedule(schedule.scheduleId)
            // Hủy báo thức tương ứng
            EventAlarmManager.cancelAlarmForEvent(getApplication(), schedule)
            // Tải lại danh sách
            loadSchedulesForCurrentMonth()
        }
    }
}
