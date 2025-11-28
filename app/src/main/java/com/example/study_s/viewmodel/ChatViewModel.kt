package com.example.study_s.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.study_s.data.model.MessageModel
import com.example.study_s.data.model.UserModel
import com.example.study_s.data.repository.ChatRepository
import com.example.study_s.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Kế thừa từ AndroidViewModel để có thể sử dụng Application Context
class ChatViewModel(application: Application) : AndroidViewModel(application) {

    // ✅ SỬA 1: Khởi tạo ChatRepository với ApplicationContext.
    // Giờ đây ChatRepository sẽ đảm nhiệm cả việc upload file.
    private val chatRepository: ChatRepository = ChatRepository(application.applicationContext)
    private val userRepository: UserRepository = UserRepository()

    private val _messages = MutableStateFlow<List<MessageModel>>(emptyList())
    val messages: StateFlow<List<MessageModel>> = _messages.asStateFlow()

    private val _chatId = MutableStateFlow<String?>(null)
    val chatId: StateFlow<String?> = _chatId.asStateFlow()

    private val _targetUser = MutableStateFlow<UserModel?>(null)
    val targetUser: StateFlow<UserModel?> = _targetUser.asStateFlow()

    fun loadTargetUserData(userId: String) {
        viewModelScope.launch {
            userRepository.getUserProfile(userId).onSuccess { userProfile ->
                _targetUser.value = userProfile
            }.onFailure {
                _targetUser.value = null
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
                    println("Cannot message user: not a mutual follow.")
                }
            }
        }
    }

    fun sendMessage(text: String) {
        val currentChatId = _chatId.value ?: return
        val currentUserId = userRepository.getCurrentUserId() ?: return

        viewModelScope.launch {
            val message = MessageModel(
                senderId = currentUserId,
                content = text,
                type = "text"
            )
            chatRepository.sendMessage(currentChatId, message)
        }
    }

    fun deleteMessage(messageId: String) {
        val currentChatId = _chatId.value ?: return
        viewModelScope.launch {
            chatRepository.deleteMessage(currentChatId, messageId)
        }
    }

    fun editMessage(messageId: String, newContent: String) {
        val currentChatId = _chatId.value ?: return
        viewModelScope.launch {
            chatRepository.editMessage(currentChatId, messageId, newContent)
        }
    }

    fun deleteChat() {
        val currentChatId = _chatId.value ?: return
        viewModelScope.launch {
            chatRepository.deleteChat(currentChatId)
        }
    }

    // ✅ SỬA 2: Hàm sendFile bây giờ gọi trực tiếp từ chatRepository
    fun sendFile(fileUri: Uri) {
        val currentChatId = _chatId.value ?: return
        viewModelScope.launch {
            // 1. Upload file bằng ChatRepository
            val uploadResult = chatRepository.uploadFile(fileUri)

            uploadResult.onSuccess { cloudinaryResult ->
                // 2. Tạo đối tượng Message từ kết quả upload
                val messageType = when (cloudinaryResult.resourceType) {
                    "image" -> "image"
                    "video" -> "video"
                    else -> "file"
                }

                val message = MessageModel(
                    content = cloudinaryResult.url,
                    type = messageType,
                    fileName = cloudinaryResult.originalFilename,
                    fileSize = cloudinaryResult.bytes
                )

                // 3. Gửi tin nhắn file bằng ChatRepository
                chatRepository.sendMessage(currentChatId, message)

            }.onFailure { exception ->
                // Xử lý lỗi upload
                println("File upload failed: ${exception.message}")
            }
        }
    }
}
