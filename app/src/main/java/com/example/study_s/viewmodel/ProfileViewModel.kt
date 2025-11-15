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

// Lớp đại diện cho các trạng thái có thể có của giao diện người dùng hồ sơ
sealed interface ProfileUiState {
    data class Success(val user: User) : ProfileUiState
    data class Error(val message: String) : ProfileUiState
    object Loading : ProfileUiState
}

// Lớp đại diện cho các trạng thái hành động (ví dụ: thay đổi mật khẩu)
sealed interface ProfileActionState {
    object Idle : ProfileActionState
    object Loading : ProfileActionState
    data class Success(val message: String) : ProfileActionState
    data class Failure(val message: String) : ProfileActionState
}

class ProfileViewModel : ViewModel() {

    private val userRepository: UserRepository = UserRepository()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    val currentUserId: String? get() = auth.currentUser?.uid
    var profileUiState: ProfileUiState by mutableStateOf(ProfileUiState.Loading)
        private set

    var actionState: ProfileActionState by mutableStateOf(ProfileActionState.Idle)
        private set

    fun loadCurrentUserProfile() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            profileUiState = ProfileUiState.Error("Người dùng chưa đăng nhập")
            return
        }

        viewModelScope.launch {
            profileUiState = ProfileUiState.Loading
            userRepository.getUserProfile(userId).onSuccess {
                if (it != null) {
                    profileUiState = ProfileUiState.Success(it)
                } else {
                    profileUiState = ProfileUiState.Error("Không thể tải hồ sơ người dùng")
                }
            }.onFailure {
                profileUiState = ProfileUiState.Error(it.message ?: "Lỗi không xác định")
            }
        }
    }

    // Hàm mới để cập nhật hồ sơ người dùng
    fun updateUserProfile(name: String, bio: String, newAvatarUri: Uri?, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            userRepository.updateUserProfile(name, bio, newAvatarUri).onSuccess {
                // Tải lại hồ sơ sau khi cập nhật thành công
                loadCurrentUserProfile()
                onResult(true, null)
            }.onFailure {
                onResult(false, it.message)
            }
        }
    }

    fun changePassword(oldPassword: String, newPassword: String) {
        viewModelScope.launch {
            actionState = ProfileActionState.Loading
            // TODO: Implement password change logic in UserRepository
            // For now, we'll just simulate a success
            actionState = ProfileActionState.Success("Mật khẩu đã được thay đổi thành công")
        }
    }

    fun resetActionState() {
        actionState = ProfileActionState.Idle
    }
}
