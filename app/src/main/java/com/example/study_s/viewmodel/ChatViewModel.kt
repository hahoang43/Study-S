package com.example.study_s.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.study_s.data.model.MessageModel
import com.example.study_s.data.model.UserModel
import com.example.study_s.data.repository.ChatRepository
import com.example.study_s.data.repository.UserRepository
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ChatViewModel(
    application: Application,
    private val chatId: String,
    private val targetUserId: String
) : AndroidViewModel(application) {

    private val chatRepository: ChatRepository = ChatRepository(application.applicationContext)
    private val userRepository: UserRepository = UserRepository()

    private val _messages = MutableStateFlow<List<MessageModel>>(emptyList())
    val messages: StateFlow<List<MessageModel>> = _messages.asStateFlow()

    private val _chatId = MutableStateFlow<String?>(null)

    private val _targetUser = MutableStateFlow<UserModel?>(null)
    val targetUser: StateFlow<UserModel?> = _targetUser.asStateFlow()

    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState: StateFlow<UploadState> = _uploadState.asStateFlow()

    private val _isBlockedByTarget = MutableStateFlow(false)
    val isBlockedByTarget: StateFlow<Boolean> = _isBlockedByTarget.asStateFlow()

    private val _haveIBlockedTarget = MutableStateFlow(false)
    val haveIBlockedTarget: StateFlow<Boolean> = _haveIBlockedTarget.asStateFlow()

    init {
        loadInitialStates()
        loadChatAndFilterMessages() // Đổi tên hàm cho rõ nghĩa
    }

    private fun loadInitialStates() {
        val currentUserId = userRepository.getCurrentUserId() ?: return

        viewModelScope.launch {
            userRepository.getUserProfileFlow(targetUserId).collect { targetUser ->
                _targetUser.value = targetUser
                _isBlockedByTarget.value = targetUser?.blockedUsers?.contains(currentUserId) == true
            }
        }

        viewModelScope.launch {
            userRepository.getUserProfileFlow(currentUserId).collect { currentUser ->
                _haveIBlockedTarget.value = currentUser?.blockedUsers?.contains(targetUserId) == true
            }
        }
    }

    // ✅ SỬA 2: SỬA LẠI HOÀN TOÀN HÀM NÀY ĐỂ LỌC TIN NHẮN
    private fun loadChatAndFilterMessages() {
        val currentUserId = userRepository.getCurrentUserId() ?: return

        viewModelScope.launch {
            chatRepository.getOrCreateChat(targetUserId).onSuccess { chatId ->
                _chatId.value = chatId

                // Lấy luồng thông tin của người dùng hiện tại (để có danh sách chặn)
                val myProfileFlow = userRepository.getUserProfileFlow(currentUserId)
                // Lấy luồng tin nhắn gốc
                val messagesFlow = chatRepository.getMessages(chatId)

                // Kết hợp 2 luồng: khi có tin nhắn mới HOẶC danh sách chặn thay đổi -> lọc lại
                myProfileFlow.combine(messagesFlow) { currentUser, messages ->
                    val blockedList = currentUser?.blockedUsers ?: emptyList()
                    messages.filter { message -> message.senderId !in blockedList }
                }.collect { filteredMessages ->
                    _messages.value = filteredMessages
                }
            }
        }
    }

    fun sendMessage(text: String) {
        val currentUserId = userRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            chatRepository.getOrCreateChat(targetUserId).onSuccess { actualChatId ->
                val message = MessageModel(
                    senderId = currentUserId,
                    content = text,
                    type = "text"
                )
                chatRepository.sendMessage(actualChatId, message)
            }
        }
    }

    // ✅ SỬA 3: ĐẢM BẢO CÁC HÀM NÀY NẰM NGOÀI sendMessage và CÙNG CẤP VỚI NÓ

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            chatRepository.getOrCreateChat(targetUserId).onSuccess { actualChatId ->
                chatRepository.deleteMessage(actualChatId, messageId)
            }
        }
    }

    fun editMessage(messageId: String, newContent: String) {
        viewModelScope.launch {
            chatRepository.getOrCreateChat(targetUserId).onSuccess { actualChatId ->
                chatRepository.editMessage(actualChatId, messageId, newContent)
            }
        }
    }

    fun deleteChat() {
        viewModelScope.launch {
            chatRepository.getOrCreateChat(targetUserId).onSuccess { actualChatId ->
                chatRepository.deleteChat(actualChatId)
            }
        }
    }

    // ✅ SỬA 1: SỬA LẠI HÀM sendFile ĐỂ KHÔNG DÙNG _chatId.value
    fun sendFile(fileUri: Uri, type: String) {
        viewModelScope.launch {
            _uploadState.value = UploadState.Uploading
            // Lấy chatId một cách an toàn, không dùng _chatId.value
            chatRepository.getOrCreateChat(targetUserId).onSuccess { actualChatId ->
                val result = chatRepository.uploadFile(fileUri)
                result.onSuccess { uploadResult ->
                    val message = MessageModel(
                        senderId = userRepository.getCurrentUserId() ?: "",
                        content = uploadResult.url,
                        type = type,
                        fileName = uploadResult.originalFilename,
                        fileSize = uploadResult.bytes
                    )
                    chatRepository.sendMessage(actualChatId, message)
                    _uploadState.value = UploadState.Idle
                }.onFailure { exception ->
                    _uploadState.value = UploadState.Error(exception.message ?: "Lỗi không xác định")
                }
            }.onFailure {
                _uploadState.value = UploadState.Error("Không thể lấy thông tin cuộc trò chuyện.")
            }
        }
    }

    fun blockUser() {
        val currentUserId = userRepository.getCurrentUserId() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentUserDocRef = userRepository.getUserDocumentRef(currentUserId)
                currentUserDocRef.update("blockedUsers", FieldValue.arrayUnion(targetUserId)).await()
                withContext(Dispatchers.Main) { /* Optional UI update */ }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun unblockUser() {
        val currentUserId = userRepository.getCurrentUserId() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentUserDocRef = userRepository.getUserDocumentRef(currentUserId)
                currentUserDocRef.update("blockedUsers", FieldValue.arrayRemove(targetUserId)).await()
                withContext(Dispatchers.Main) {
                    println("User ${targetUserId} unblocked successfully.")
                }
            } catch (e: Exception) {
                println("Error unblocking user: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    sealed class UploadState {
        object Idle : UploadState()
        object Uploading : UploadState()
        data class Error(val message: String) : UploadState()
    }
}
