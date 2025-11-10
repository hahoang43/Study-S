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

import com.example.study_s.data.repository.AuthRepository
// Trạng thái của giao diện
sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Success(val user: User) : ProfileUiState
    data class Error(val message: String) : ProfileUiState

}
// SỬA LẠI CHO RÕ RÀNG HƠN: Dùng Success và Error thay vì Result chung chung
sealed interface ProfileActionState {
    object Idle : ProfileActionState
    object Loading : ProfileActionState
    data class Success(val message: String) : ProfileActionState
    data class Failure(val message: String) : ProfileActionState
}
class ProfileViewModel(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
    ) : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    // Trạng thái cho màn hình Profile và EditProfile
    var profileUiState: ProfileUiState by mutableStateOf(ProfileUiState.Loading)
        private set

    // Trạng thái cho màn hình người lạ (StrangerScreen)
    var stragerProfileUiState: ProfileUiState by mutableStateOf(ProfileUiState.Loading)
        private set
    // State để theo dõi kết quả của hành động "Đổi mật khẩu"
    var actionState: ProfileActionState by mutableStateOf(ProfileActionState.Idle)
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
    fun onResetPasswordClick() {
        // Lấy email của người dùng hiện tại
        val email = auth.currentUser?.email
        if (email.isNullOrEmpty()) {
            // Nếu không có email, không thể gửi. Thông báo lỗi ngay lập tức.
            actionState = ProfileActionState.Failure("Không tìm thấy email người dùng.")
            return
        }

        // =========================================================================
        // == BẮT ĐẦU PHẦN CODE QUAN TRỌNG NHẤT ==
        // Đây là phần xử lý kết quả trả về từ Firebase
        // =========================================================================

        // Cập nhật trạng thái sang Loading để giao diện hiển thị vòng xoay
        actionState = ProfileActionState.Loading

        // Gửi yêu cầu và lắng nghe kết quả
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // TÁC VỤ THÀNH CÔNG: Cập nhật state thành Success
                    actionState =
                        ProfileActionState.Success("Email đổi mật khẩu đã được gửi. Vui lòng kiểm tra hòm thư của bạn.")
                } else {
                    // TÁC VỤ THẤT BẠI: Cập nhật state thành Failure với thông báo lỗi
                    val errorMessage = task.exception?.message ?: "Đã xảy ra lỗi không xác định."
                    actionState = ProfileActionState.Failure("Lỗi: $errorMessage")
                }
            }

    }
    // XÓA TỪ ĐÂY
    // ===== DÁN CODE ĐỔI MẬT KHẨU VÀO ĐÚNG CHỖ NÀY =====
    fun changePassword(oldPassword: String, newPassword: String) {
        actionState = ProfileActionState.Loading

        val user = auth.currentUser
        if (user?.email == null) {
            actionState = ProfileActionState.Failure("Không tìm thấy thông tin người dùng. Vui lòng đăng nhập lại.")
            return
        }

        // Bước quan trọng: Xác thực lại người dùng bằng mật khẩu cũ
        val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(user.email!!, oldPassword)

        user.reauthenticate(credential)
            .addOnCompleteListener { reauthTask ->
                if (reauthTask.isSuccessful) {
                    // Nếu xác thực lại thành công, tiến hành cập nhật mật khẩu mới
                    user.updatePassword(newPassword)
                        .addOnCompleteListener { updateTask ->
                            if (updateTask.isSuccessful) {
                                actionState = ProfileActionState.Success("Đổi mật khẩu thành công!")
                            } else {
                                actionState = ProfileActionState.Failure(updateTask.exception?.message ?: "Lỗi cập nhật mật khẩu.")
                            }
                        }
                } else {
                    // Thất bại khi xác thực lại (thường do sai mật khẩu cũ)
                    if (reauthTask.exception?.message?.contains("WRONG_PASSWORD") == true ||
                        reauthTask.exception?.message?.contains("INVALID_LOGIN_CREDENTIALS") == true) {
                        actionState = ProfileActionState.Failure("Mật khẩu cũ không chính xác.")
                    } else {
                        actionState = ProfileActionState.Failure(reauthTask.exception?.message ?: "Lỗi xác thực.")
                    }
                }
            }
    }
    // ===== KẾT THÚC PHẦN CODE DÁN VÀO =====
    // XÓA ĐẾN HẾT DÒNG NÀY


    /**
     * Dọn dẹp trạng thái của hành động sau khi đã hiển thị thông báo.
     */
    fun resetActionState() {
        actionState = ProfileActionState.Idle
    }

    /**
     * Xử lý đăng xuất.
     */
    fun signOut() {
        authRepository.signOut()
    }
}
