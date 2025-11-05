package com.example.study_s.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.study_s.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.example.study_s.data.repository.UserRepository
import com.google.firebase.auth.FirebaseUser
sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}


class AuthViewModel(
    private val repo: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow<AuthState>(AuthState.Idle)
    val state: StateFlow<AuthState> = _state

    // Đăng ký tài khoản mới
    fun signUp(name: String, email: String, pass: String) {
        viewModelScope.launch {
            _state.value = AuthState.Loading

            // Gọi hàm signUp trong AuthRepository
            val result = repo.signUp(email, pass)

            result.fold(
                onSuccess = { firebaseUser ->
                    // NOTE 2: Lấy uid trực tiếp từ firebaseUser.
                    // Khi onSuccess được gọi, firebaseUser chắc chắn không null.
                    val uid = firebaseUser.uid

                    // Tạo hồ sơ người dùng trong Firestore
                    val profileResult = userRepository.createUserProfile(
                        userId = uid,
                        name = name,
                        email = email
                    )

                    profileResult.fold(
                        onSuccess = {
                            // NOTE 3: SỬA LỖI TẠI ĐÂY
                            // AuthState.Success được định nghĩa là một 'object',
                            // vì vậy nó được gọi mà không cần tham số.
                            _state.value = AuthState.Success
                        },
                        onFailure = { profileError ->
                            _state.value = AuthState.Error(
                                "Đăng ký thành công nhưng không thể tạo hồ sơ: ${profileError.message}"
                            )
                        }
                    )
                },
                onFailure = { authError ->
                    _state.value = AuthState.Error(
                        authError.message ?: "Lỗi đăng ký không xác định"
                    )
                }
            )
        }
    }

    /** Gọi đăng nhập Google (Firebase) */
    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            val result = repo.signInWithGoogle(idToken)
            _state.value = if (result.isSuccess) {
                AuthState.Success
            } else {
                AuthState.Error(result.exceptionOrNull()?.message ?: "Đăng nhập thất bại.")
            }
        }
    }

    fun signOut() {
        repo.signOut()
        _state.value = AuthState.Idle
    }
}
