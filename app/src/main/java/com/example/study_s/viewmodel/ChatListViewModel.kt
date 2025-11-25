
package com.example.study_s.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.study_s.data.model.Chat
import com.example.study_s.data.model.UserModel
import com.example.study_s.data.repository.ChatListRepository
import com.example.study_s.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatListViewModel(
    private val chatListRepository: ChatListRepository = ChatListRepository(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    private val _users = MutableStateFlow<Map<String, UserModel>>(emptyMap())

    val chats: StateFlow<List<Pair<Chat, UserModel?>>> = 
        _chats.combine(_users) { chats, users ->
            chats.map { chat ->
                val otherUserId = chat.members.firstOrNull { it != userRepository.getCurrentUserId() }
                Pair(chat, otherUserId?.let { users[it] })
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    init {
        loadChats()
    }

    private fun loadChats() {
        viewModelScope.launch {
            chatListRepository.getChats().collect {
                _chats.value = it
                it.forEach { chat ->
                    val otherUserId = chat.members.firstOrNull { it != userRepository.getCurrentUserId() }
                    if (otherUserId != null) {
                        loadUserData(otherUserId)
                    }
                }
            }
        }
    }

    private fun loadUserData(userId: String) {
        viewModelScope.launch {
            userRepository.getUserProfile(userId).onSuccess {
                if (it != null) {
                    _users.value = _users.value + (userId to it)
                }
            }
        }
    }
}
