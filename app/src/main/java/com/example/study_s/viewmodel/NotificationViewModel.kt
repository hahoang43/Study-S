// ĐƯỜNG DẪN: viewmodel/NotificationViewModel.kt

package com.example.study_s.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.study_s.data.model.Notification
import com.example.study_s.data.repository.NotificationRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NotificationViewModel : ViewModel() {
    private val repository = NotificationRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications = _notifications.asStateFlow()

    // ✅ BƯỚC 1: THÊM isLoading STATE VÀO ĐÂY
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    init {
        loadNotifications()
    }

    fun loadNotifications() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            // ✅ BƯỚC 2: CẬP NHẬT TRẠNG THÁI KHI TẢI DỮ LIỆU
            _isLoading.value = true // Bắt đầu tải -> true
            _notifications.value = repository.getNotificationsForUser(userId)
            _isLoading.value = false // Tải xong -> false
        }
    }
}
