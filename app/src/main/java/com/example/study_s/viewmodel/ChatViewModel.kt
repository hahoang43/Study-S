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
class ChatViewModel(application: Application,
                    private val chatId: String,
                    private val targetUserId: String) : AndroidViewModel(application) {

    // ✅ SỬA 1: Khởi tạo ChatRepository với ApplicationContext.
    // Giờ đây ChatRepository sẽ đảm nhiệm cả việc upload file.
    private val chatRepository: ChatRepository = ChatRepository(application.applicationContext)
    private val userRepository: UserRepository = UserRepository()

    private val _messages = MutableStateFlow<List<MessageModel>>(emptyList())
    val messages: StateFlow<List<MessageModel>> = _messages.asStateFlow()

    private val _chatId = MutableStateFlow<String?>(null)

    private val _targetUser = MutableStateFlow<UserModel?>(null)
    val targetUser: StateFlow<UserModel?> = _targetUser.asStateFlow()
    // In ChatViewModel.kt
    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState: StateFlow<UploadState> = _uploadState.asStateFlow()
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
            // Bỏ qua kiểm tra checkMutualFollow
            chatRepository.getOrCreateChat(targetUserId).onSuccess { chatId ->
                _chatId.value = chatId
                chatRepository.getMessages(chatId).collect {
                    _messages.value = it
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
    sealed class UploadState {
        object Idle : UploadState() // Trạng thái nghỉ
        object Uploading : UploadState() // Đang tải lên
        data class Error(val message: String) : UploadState() // Có lỗi
    }
    // ✅ SỬA 2: Hàm sendFile bây giờ gọi trực tiếp từ chatRepository
    fun sendFile(fileUri: Uri, type: String) {
        val currentChatId = _chatId.value ?: return // Make sure you use _chatId.value

        viewModelScope.launch {_uploadState.value = UploadState.Uploading // Bắt đầu tải
            val result = chatRepository.uploadFile(fileUri)

            result.onSuccess { uploadResult ->
                val message = MessageModel(
                    senderId = userRepository.getCurrentUserId() ?: "",
                    content = uploadResult.url, // URL để tải
                    type = type,
                    // ✅ LƯU ĐÚNG TÊN FILE VÀ KÍCH THƯỚC
                    fileName = uploadResult.originalFilename, // Tên file gốc
                    fileSize = uploadResult.bytes // Kích thước file
                )
                // Pass the actual chatId string and the message object
                chatRepository.sendMessage(currentChatId, message)
                _uploadState.value = UploadState.Idle // Tải xong, quay về trạng thái nghỉ
            }.onFailure { exception ->
                // Xử lý lỗi, ví dụ: hiển thị thông báo
                println("File upload failed: ${exception.message}")
                _uploadState.value = UploadState.Error(exception.message ?: "Lỗi không xác định")
            }
        }
    }
}
