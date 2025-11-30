package com.example.study_s.viewmodel
import android.app.Application // <-- Import Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Factory (nhà máy) để tạo ra một thực thể của ChatViewModel.
 * Cần thiết vì ChatViewModel yêu cầu các tham số (context, chatId, targetUserId) trong hàm khởi tạo.
 */
class ChatViewModelFactory(
    private val application: Application, // <-- Change here
    private val chatId: String,
    private val targetUserId: String
) : ViewModelProvider.Factory {

    /**
     * Phương thức này sẽ được hệ thống gọi để tạo ViewModel.
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // Kiểm tra xem hệ thống có đang yêu cầu đúng ChatViewModel không
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            // Nếu đúng, tạo một ChatViewModel với các tham số đã nhận từ View
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(application, chatId, targetUserId) as T
        }
        // Nếu hệ thống yêu cầu một ViewModel khác mà Factory này không biết cách tạo, báo lỗi.
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
