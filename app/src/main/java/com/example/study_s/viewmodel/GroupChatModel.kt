package com.example.study_s.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.study_s.data.model.MessageModel
import com.example.study_s.data.repository.GroupChatRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {

    private val chatRepository = GroupChatRepository()

    private val _messages = MutableStateFlow<List<MessageModel>>(emptyList())
    val messages: StateFlow<List<MessageModel>> = _messages

    fun listenToGroupMessages(groupId: String) {
        viewModelScope.launch {
            callbackFlow {
                val listener = chatRepository.getGroupMessages(groupId) { messages ->
                    trySend(messages)
                }
                awaitClose { listener.remove() }
            }.collect { newMessages ->
                _messages.value = newMessages
            }
        }
    }

    fun sendMessage(groupId: String, senderId: String, content: String, senderName: String) {
        viewModelScope.launch {
            val message = MessageModel(
                senderId = senderId,
                content = content,
                senderName = senderName
            )
            chatRepository.sendMessage(groupId, message)
        }
    }
}
