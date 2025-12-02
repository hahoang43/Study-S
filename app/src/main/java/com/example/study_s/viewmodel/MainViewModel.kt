package com.example.study_s.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.study_s.data.repository.ChatListRepository
import com.example.study_s.data.repository.NotificationRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class MainViewModel : ViewModel() {

    private val notificationRepository = NotificationRepository()
    private val chatListRepository = ChatListRepository() // ✅ ADD REPOSITORY

    val unreadNotificationCount: StateFlow<Int> = notificationRepository.getNotificationsFlow()
        .map { notificationList ->
            notificationList.count { !it.isRead }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = 0
        )

    // ✅ ADD STATEFLOW FOR UNREAD MESSAGES
    val unreadMessageCount: StateFlow<Int> = chatListRepository.getUnreadMessagesCountFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = 0
        )
}
