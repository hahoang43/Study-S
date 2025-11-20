// ĐƯỜNG DẪN: viewmodel/AccountViewModelFactory.kt
// NỘI DUNG HOÀN CHỈNH

package com.example.study_s.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.study_s.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth

@Suppress("UNCHECKED_CAST")
class AccountViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AccountViewModel::class.java)) {
            // Hướng dẫn hệ thống cách tạo ra AccountViewModel
            // bằng cách cung cấp các dependencies cần thiết.
            return AccountViewModel(
                userRepository = UserRepository(),
                auth = FirebaseAuth.getInstance()
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
    