package com.example.study_s.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.study_s.data.model.MessageModel
import com.example.study_s.data.repository.GroupChatRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GroupChatViewModel(private val groupChatRepository: GroupChatRepository = GroupChatRepository()) : ViewModel() {

    private val _messages = MutableStateFlow<List<MessageModel>>(emptyList())
    val messages: StateFlow<List<MessageModel>> = _messages

    private val _userRemoved = MutableSharedFlow<String>()
    val userRemoved = _userRemoved.asSharedFlow()

    fun listenToGroupMessages(groupId: String) {
        viewModelScope.launch {
            groupChatRepository.getGroupMessages(groupId).collect {
                _messages.value = it
            }
        }
    }

    fun sendMessage(groupId: String, senderId: String, content: String, senderName: String) {
        viewModelScope.launch {
            val message = MessageModel(
                senderId = senderId,
                content = content,
                senderName = senderName,
                timestamp = System.currentTimeMillis()
            )
            groupChatRepository.sendGroupMessage(groupId, message)
        }
    }

    fun notifyUserRemoved(removedUserId: String) {
        viewModelScope.launch {
            _userRemoved.emit(removedUserId)
        }
    }
}
