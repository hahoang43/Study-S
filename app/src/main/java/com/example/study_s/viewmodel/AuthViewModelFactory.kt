package com.example.study_s.viewmodel

// File: com/example/study_s/viewmodel/AuthViewModelFactory.kt


import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.study_s.data.repository.AuthRepository
import com.example.study_s.data.repository.UserRepository

/**
 * Đây là "nhà máy" sản xuất AuthViewModel.
 * Nhiệm vụ của nó là tạo ra AuthRepository và UserRepository,
 * sau đó dùng chúng để khởi tạo AuthViewModel.
 */
class AuthViewModelFactory : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // Kiểm tra xem lớp được yêu cầu có phải là AuthViewModel không
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {

            // Tạo ra các repository cần thiết
            val authRepository = AuthRepository()
            val userRepository = UserRepository()

            // Dùng các repository đó để tạo ra AuthViewModel
            // và trả về dưới dạng T (kiểu dữ liệu chung)
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(authRepository, userRepository) as T
        }

        // Nếu lớp được yêu cầu không phải là AuthViewModel, báo lỗi
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
