package com.example.study_s.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.study_s.data.model.ChatModel
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

    private val _chats = MutableStateFlow<List<ChatModel>>(emptyList())
    private val _users = MutableStateFlow<Map<String, UserModel>>(emptyMap())

    val chats: StateFlow<List<Pair<ChatModel, UserModel?>>> =
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

    fun deleteChats(chatIds: Set<String>) {
        viewModelScope.launch {
            try {
                chatListRepository.deleteChats(chatIds)
            } catch (e: Exception) {
                println("Failed to delete chats: ${e.message}")
            }
        }
    }

    /**
     * Yêu cầu Repository đánh dấu cuộc trò chuyện đã được đọc.
     * @param chatId ID của cuộc trò chuyện cần cập nhật.
     */
    fun markChatAsRead(chatId: String) {
        // Không cần kiểm tra userId ở đây vì Repository đã xử lý
        viewModelScope.launch {
            try {
                // Lỗi "Unresolved reference" sẽ xảy ra ở dòng này
                // nếu hàm markChatAsRead chưa tồn tại trong ChatListRepository
                chatListRepository.markChatAsRead(chatId)

                // Bạn không cần làm gì thêm ở đây, vì Firestore listener
                // trong getChats() sẽ tự động nhận thấy sự thay đổi và cập nhật UI.
            } catch (e: Exception) {
                // Xử lý lỗi nếu cần, ví dụ: log lỗi
                println("Failed to mark chat as read: ${e.message}")
            }
        }
    }
}
