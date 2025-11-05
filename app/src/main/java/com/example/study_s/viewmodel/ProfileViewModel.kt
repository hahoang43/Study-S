// file: ui/screens/profile/ProfileViewModel.kt
package com.example.study_s.viewmodel

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.study_s.data.model.User
import com.example.study_s.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

// Trạng thái của giao diện
sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Success(val user: User) : ProfileUiState
    data class Error(val message: String) : ProfileUiState
}

class ProfileViewModel(private val userRepository: UserRepository) : ViewModel() {

    // Trạng thái cho màn hình Profile và EditProfile
    var profileUiState: ProfileUiState by mutableStateOf(ProfileUiState.Loading)
        private set

    // Trạng thái cho màn hình người lạ (StrangerScreen)
    var stragerProfileUiState: ProfileUiState by mutableStateOf(ProfileUiState.Loading)
        private set

    init {
        // Tải hồ sơ người dùng hiện tại khi ViewModel được tạo
        loadCurrentUserProfile()
    }

    // Tải hồ sơ của người dùng đang đăng nhập
    fun loadCurrentUserProfile() {
        viewModelScope.launch {
            profileUiState = ProfileUiState.Loading
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId == null) {
                profileUiState = ProfileUiState.Error("Không tìm thấy người dùng.")
                return@launch
            }
            val result = userRepository.getUserProfile(userId)
            profileUiState = result.fold(
                onSuccess = { user ->
                    if (user != null) ProfileUiState.Success(user)
                    else ProfileUiState.Error("Không thể tải hồ sơ.")
                },
                onFailure = { ProfileUiState.Error(it.message ?: "Lỗi không xác định") }
            )
        }
    }

    // Tải hồ sơ của người dùng khác
    fun loadStragerProfile(userId: String) {
        viewModelScope.launch {
            stragerProfileUiState = ProfileUiState.Loading
            val result = userRepository.getUserProfile(userId)
            stragerProfileUiState = result.fold(
                onSuccess = { user ->
                    if (user != null) ProfileUiState.Success(user)
                    else ProfileUiState.Error("Không thể tải hồ sơ.")
                },
                onFailure = { ProfileUiState.Error(it.message ?: "Lỗi không xác định") }
            )
        }
    }

    // Cập nhật hồ sơ
    fun updateUserProfile(name: String, bio: String, avatarUri: Uri?, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = userRepository.updateUserProfile(name, bio, avatarUri)
            result.fold(
                onSuccess = {
                    onResult(true, null) // Thành công
                    loadCurrentUserProfile() // Tải lại dữ liệu sau khi cập nhật
                },
                onFailure = {
                    onResult(false, it.message) // Thất bại
                }
            )
        }
    }
}
