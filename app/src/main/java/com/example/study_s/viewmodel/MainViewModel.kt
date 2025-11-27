// ĐƯỜNG DẪN: viewmodel/MainViewModel.kt

package com.example.study_s.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.study_s.data.repository.NotificationRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel này được chia sẻ trên toàn bộ ứng dụng để cung cấp các trạng thái chung,
 * ví dụ như số lượng thông báo chưa đọc.
 */
class MainViewModel : ViewModel() {

    // MainViewModel sử dụng NotificationRepository để lấy dữ liệu.
    private val notificationRepository = NotificationRepository()

    // Dòng dữ liệu này lắng nghe real-time số lượng thông báo chưa đọc.
    // Bất kỳ màn hình nào truy cập biến này cũng sẽ có được con số mới nhất.
    val unreadNotificationCount: StateFlow<Int> = notificationRepository.getNotificationsFlow()
        .map { notificationList ->
            // Đếm các thông báo có trường isRead = false
            notificationList.count { !it.isRead }
        }
        .stateIn(
            // viewModelScope: Đảm bảo Flow này sẽ bị hủy khi ViewModel bị hủy.
            scope = viewModelScope,
            // SharingStarted.WhileSubscribed: Chỉ bắt đầu lắng nghe khi có màn hình đang sử dụng nó.
            started = SharingStarted.WhileSubscribed(5000L),
            // Giá trị ban đầu khi chưa có dữ liệu là 0.
            initialValue = 0
        )
}
