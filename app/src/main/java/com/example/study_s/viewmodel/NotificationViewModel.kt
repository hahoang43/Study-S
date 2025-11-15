// ĐƯỜNG DẪN: viewmodel/NotificationViewModel.kt
// NỘI DUNG HOÀN CHỈNH, ĐÃ NÂNG CẤP

package com.example.study_s.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.study_s.data.model.Notification
import com.example.study_s.data.repository.NotificationRepository
import com.example.study_s.ui.navigation.Routes
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.navigation.NavController // ✅ 1. IMPORT NavController

class NotificationViewModel(
    private val repository: NotificationRepository = NotificationRepository()
) : ViewModel() {

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications = _notifications.asStateFlow()

    init {
        loadNotifications()
    }

    private fun loadNotifications() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            _notifications.value = repository.getNotificationsForUser(userId)
        }
    }

    // ✅ 2. HÀM MỚI VÀ QUAN TRỌNG NHẤT
    fun onNotificationClicked(notification: Notification, navController: NavController) {
        viewModelScope.launch {
            // Bước 1: Đánh dấu đã đọc (chỉ khi nó chưa được đọc)
            if (!notification.isRead) {
                repository.markAsRead(notification.notificationId)
                // Cập nhật lại UI ngay lập tức để mất chấm đỏ
                // Thay vì load lại toàn bộ, ta chỉ cần tìm và sửa item trong list hiện tại
                val updatedList = _notifications.value.map {
                    if (it.notificationId == notification.notificationId) {
                        it.copy(isRead = true)
                    } else {
                        it
                    }
                }
                _notifications.value = updatedList
            }

            // Bước 2: Điều hướng dựa trên loại thông báo
            when (notification.type) {
                "like", "comment" -> {
                    // Nếu là like hoặc comment, đi đến bài viết chi tiết
                    notification.postId?.let { postId ->
                        navController.navigate("${Routes.PostDetail}/$postId")
                    }
                }
                "follow" -> {
                    // Nếu là follow, đi đến trang cá nhân của người đã follow mình
                    notification.actorId?.let { actorId ->
                        navController.navigate("${Routes.OtherProfile}/$actorId")
                    }
                }
                // Các loại thông báo khác (nếu có)
                else -> {
                    // Mặc định không làm gì hoặc đi đến một màn hình chung
                }
            }
        }
    }
}
