package com.example.study_s.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.study_s.data.model.Notification
import com.example.study_s.data.repository.NotificationRepository
import com.example.study_s.ui.navigation.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotificationViewModel : ViewModel() {

    private val repository = NotificationRepository()

    // Liste thông báo mà UI dùng
    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications

    // Đếm số chưa đọc (cho icon chuông, nếu cần)
    val unreadNotificationCount: StateFlow<Int> =
        notifications
            .map { list -> list.count { !it.isRead } }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = 0
            )

    init {
        // Lắng nghe realtime từ Firestore qua Repository
        viewModelScope.launch {
            repository.getNotificationsFlow().collect { list ->
                Log.d("NotificationVM", "Flow update: ${list.size} notifications")
                _notifications.value = list
            }
        }
    }

    fun onNotificationClicked(notification: Notification, navController: NavController) {
        Log.d(
            "NotificationVM",
            "Clicked: id=${notification.notificationId}, isRead=${notification.isRead}, type=${notification.type}"
        )

        if (!notification.isRead) {
            markAsRead(notification.notificationId)
        }

        when (notification.type) {
            "like", "comment" -> {
                notification.postId?.let { postId ->
                    navController.navigate("${Routes.PostDetail}/$postId")
                }
            }

            "follow" -> {
                val actorId = notification.actorId
                if (actorId.isNotBlank()) {
                    navController.navigate("${Routes.OtherProfile}/$actorId")
                }
            }

            "schedule_reminder" -> {
                navController.navigate(Routes.Schedule)
            }

            else -> {
                Log.d("NotificationVM", "Unknown notification type: ${notification.type}")
            }
        }
    }

    private fun markAsRead(notificationId: String) {
        if (notificationId.isBlank()) return

        viewModelScope.launch {
            try {
                // Update Firestore
                repository.markAsRead(notificationId)

                // Update luôn list local để UI đổi ngay
                _notifications.value = _notifications.value.map {
                    if (it.notificationId == notificationId) it.copy(isRead = true) else it
                }

                Log.d("NotificationVM", "Marked as read (local + firestore): $notificationId")
            } catch (e: Exception) {
                Log.e("NotificationVM", "Error marking as read", e)
            }
        }
    }
}
