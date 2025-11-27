package com.example.study_s.viewmodel

import android.util.Log
import androidx.core.util.remove
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
import com.google.firebase.firestore.ListenerRegistration
import com.example.study_s.data.repository.NotificationRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotificationViewModel : ViewModel() {
    private val repository = NotificationRepository()
    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = repository.getNotificationsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )
    val unreadNotificationCount: StateFlow<Int> = notifications
        .map { notificationList ->
            notificationList.count { !it.isRead } // Đếm các thông báo có isRead = false
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = 0 // Giá trị ban đầu là 0
        )


    // Biến để giữ "trình lắng nghe" của Firestore, giúp chúng ta có thể hủy nó đi
    private var notificationListener: ListenerRegistration? = null


    init {
        // ✅ THAY ĐỔI: Thay vì load 1 lần, chúng ta bắt đầu "lắng nghe"
        listenForNotifications()
    }

    // ✅ HÀM MỚI: DÙNG addSnapshotListener ĐỂ LẮNG NGHE DỮ LIỆU REAL-TIME
    private fun listenForNotifications() {
        if (userId == null) {
            Log.w("NotificationVM", "User ID is null. Cannot listen for notifications.")
            return
        }

        // Hủy trình lắng nghe cũ đi nếu nó đang tồn tại, để tránh việc lắng nghe nhiều lần
        notificationListener?.remove()

        // Query giống hệt như hàm loadNotifications() cũ của bạn
        val query = db.collection("notifications")
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)

        // Đây là trái tim của giải pháp
        notificationListener = query.addSnapshotListener { snapshot, error ->
            // Xử lý nếu có lỗi từ Firestore
            if (error != null) {
                Log.e("NotificationVM", "Listen for notifications failed.", error)
                return@addSnapshotListener
            }

            // Nếu snapshot tồn tại (có dữ liệu trả về)
            if (snapshot != null) {
                // Chuyển đổi dữ liệu và cập nhật StateFlow
                // Dùng lại logic gán ID của bạn, rất tốt!
                val notificationList = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Notification::class.java)?.apply {
                        this.notificationId = doc.id
                    }
                }
                _notifications.value = notificationList
                Log.d("NotificationVM", "Real-time update! Notifications count: ${notificationList.size}")
            }
        }
    }


    // ✅ BƯỚC 3: SỬA LẠI HÀM onNotificationClicked
    fun onNotificationClicked(notification: Notification, navController: NavController) {
        if (!notification.isRead) {
            // Bây giờ hàm này sẽ gọi hàm `markAsRead` đã được sửa đổi
            markAsRead(notification.notificationId)
        }
        // Logic điều hướng giữ nguyên
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
                navController.navigate(Routes.Schedule)
            }
        }
    }

    // ✅ BƯỚC 4: SỬA LẠI HOÀN TOÀN HÀM markAsRead
    // Hàm này giờ sẽ gọi đến Repository
    private fun markAsRead(notificationId: String) {
        if (notificationId.isBlank()) return

        viewModelScope.launch {
            // GỌI HÀM CỦA REPOSITORY
            repository.markAsRead(notificationId)
            // Chúng ta không cần làm gì thêm ở đây, vì `listenForNotifications`
            // sẽ tự động nhận thấy sự thay đổi và cập nhật UI.
        }
    }

    // Hàm onCleared() giữ nguyên, đã rất tốt
    override fun onCleared() {
        super.onCleared()
        notificationListener?.remove()
        Log.d("NotificationVM", "ViewModel cleared and listener removed.")
    }
}