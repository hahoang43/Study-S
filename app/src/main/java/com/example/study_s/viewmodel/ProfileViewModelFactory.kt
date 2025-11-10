// File: com/example/study_s/viewmodel/ProfileViewModelFactory.kt

package com.example.study_s.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.study_s.data.repository.UserRepository
import com.example.study_s.data.repository.AuthRepository

/**
 * Đây là "nhà máy" sản xuất ProfileViewModel.
 * Nhiệm vụ của nó là tạo ra UserRepository,
 * sau đó dùng nó để khởi tạo ProfileViewModel.
 */
class ProfileViewModelFactory : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // Kiểm tra xem lớp được yêu cầu có phải là ProfileViewModel không
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {

            // Tạo ra repository cần thiết
            val userRepository = UserRepository()

            // Dùng repository đó để tạo ra ProfileViewModel
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(
                userRepository= UserRepository(),
                authRepository= AuthRepository()) as T
        }

        // Nếu lớp được yêu cầu không phải là ProfileViewModel, báo lỗi
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
