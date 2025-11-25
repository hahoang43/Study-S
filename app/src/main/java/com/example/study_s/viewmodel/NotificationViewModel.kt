package com.example.study_s.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.study_s.data.model.Notification
import com.example.study_s.ui.navigation.Routes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class NotificationViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications = _notifications.asStateFlow()

    init {
        loadNotifications()
    }

    private fun loadNotifications() {
        if (userId == null) return

        viewModelScope.launch {
            try {
                val snapshot = db.collection("notifications")
                    .whereEqualTo("userId", userId)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get()
                    .await()

                // ✅ LOGIC QUAN TRỌNG NHẤT NẰM Ở ĐÂY
                // Chuyển đổi các document thành đối tượng Notification
                // và gán ID của document vào trường notificationId
                _notifications.value = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Notification::class.java)?.apply {
                        this.notificationId = doc.id // Gán ID để làm key cho LazyColumn
                    }
                }
                Log.d("NotificationVM", "Loaded ${_notifications.value.size} notifications.")

            } catch (e: Exception) {
                Log.e("NotificationVM", "Error loading notifications", e)
            }
        }
    }

    fun onNotificationClicked(notification: Notification, navController: NavController) {
        // Đánh dấu là đã đọc
        markAsRead(notification.notificationId)

        // Điều hướng dựa trên loại thông báo
        when (notification.type) {
            "like", "comment" -> {
                notification.postId?.let { postId ->
                    navController.navigate("${Routes.PostDetail}/$postId")
                }
            }
            "follow" -> {
                notification.actorId?.let { actorId ->
                    navController.navigate("${Routes.OtherProfile}/$actorId")
                }
            }
            "schedule_reminder" -> {
                // Đối với lời nhắc, điều hướng đến màn hình Lịch
                navController.navigate(Routes.Schedule)
            }
            // Thêm các trường hợp khác nếu cần
        }
    }

    private fun markAsRead(notificationId: String) {
        if (notificationId.isBlank()) return

        viewModelScope.launch {
            try {
                db.collection("notifications").document(notificationId)
                    .update("isRead", true)
                    .await()

                // Cập nhật lại UI để mất chấm đỏ
                _notifications.value = _notifications.value.map {
                    if (it.notificationId == notificationId) {
                        it.copy(isRead = true)
                    } else {
                        it
                    }
                }
            } catch (e: Exception) {
                Log.e("NotificationVM", "Error marking notification as read", e)
            }
        }
    }
}
