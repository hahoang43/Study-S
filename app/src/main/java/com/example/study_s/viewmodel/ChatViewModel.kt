package com.example.study_s.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.study_s.data.model.Message
import com.example.study_s.data.repository.ChatRepository
import com.example.study_s.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Date

class ChatViewModel(
    private val chatRepository: ChatRepository = ChatRepository(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private val _chatId = MutableStateFlow<String?>(null)
    val chatId: StateFlow<String?> = _chatId

    private val _targetUserName = MutableStateFlow("")
    val targetUserName: StateFlow<String> = _targetUserName

    fun loadTargetUserData(userId: String) {
        viewModelScope.launch {
            userRepository.getUserProfile(userId).onSuccess {
                _targetUserName.value = it?.name ?: "Không rõ"
            }
        }
    }

    fun loadChat(targetUserId: String) {
        viewModelScope.launch {
            userRepository.checkMutualFollow(targetUserId).onSuccess { isMutual ->
                if (isMutual) {
                    chatRepository.getOrCreateChat(targetUserId).onSuccess { chatId ->
                        _chatId.value = chatId
                        chatRepository.getMessages(chatId).collect {
                            _messages.value = it
                        }
                    }
                } else {
                    // Handle case where users don't mutually follow
                    // For now, we'll just log it. In a real app, you might show a message to the user.
                    println("Cannot message user: not a mutual follow.")
                }
            }
        }
    }

    fun sendMessage(text: String, senderId: String) {
        val currentChatId = _chatId.value ?: return
        viewModelScope.launch {
            val message = Message(
                senderId = senderId,
                content = text,
                timestamp = Date(System.currentTimeMillis())
            )
            chatRepository.sendMessage(currentChatId, message)
        }
    }
}
