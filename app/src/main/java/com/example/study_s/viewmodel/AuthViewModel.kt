package com.example.study_s.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.study_s.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.example.study_s.data.repository.UserRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

// Thêm một Sealed Class cho các sự kiện điều hướng một lần
sealed class AuthEvent {
    object OnSignOut : AuthEvent()
}

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

    // NOTE 1: Thêm SharedFlow để xử lý sự kiện đăng xuất
    private val _event = MutableSharedFlow<AuthEvent>()
    val event = _event.asSharedFlow()
    /**
     * HÀM CHUNG: Đồng bộ hồ sơ người dùng lên Firestore sau khi Firebase Auth thành công.
     */
    private suspend fun handleSuccessfulLogin(firebaseUser: FirebaseUser) {
        val profileResult = userRepository.upsertUserProfile(firebaseUser)
        if (profileResult.isSuccess) {
            // Chỉ chuyển sang Success khi cả Auth và Firestore đều ổn
            _state.value = AuthState.Success
        } else {
            _state.value = AuthState.Error("Đăng nhập Auth thành công nhưng không thể đồng bộ hồ sơ.")
        }
    }

    /**
     * Đăng nhập bằng Email và Password.
     */
    fun signInWithEmail(email: String, pass: String) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            val result = repo.signIn(email, pass)
            result.fold(
                onSuccess = { firebaseUser -> handleSuccessfulLogin(firebaseUser) },
                onFailure = { _state.value = AuthState.Error(it.message ?: "Đăng nhập thất bại.") }
            )
        }
    }
    /**
     * Đăng nhập bằng Google.
     */
    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            val result = repo.signInWithGoogle(idToken)
            result.fold(
                onSuccess = { firebaseUser -> handleSuccessfulLogin(firebaseUser) },
                onFailure = { _state.value = AuthState.Error(it.message ?: "Đăng nhập Google thất bại.") }
            )
        }
    }
    /**
     * Hàm chung để xử lý sau khi đăng nhập thành công.
     * Nó đảm bảo profile được tạo/đồng bộ trên Firestore.
     * Đây là bước quan trọng nhất để sửa lỗi profile không cập nhật.
     */
    fun signOut() {
        viewModelScope.launch {
            repo.signOut() // Gọi hàm đăng xuất từ Firebase
            _event.emit(AuthEvent.OnSignOut) // Phát tín hiệu "đã đăng xuất"
        }
    }
    /**
     * Reset trạng thái của ViewModel về Idle.
     * Được gọi từ UI sau khi đã xử lý xong một trạng thái (Success hoặc Error).
     */
    fun resetState() {
        _state.value = AuthState.Idle
    }

    /**
     * Đăng ký tài khoản mới.
     * Logic này cần đảm bảo `createUserProfile` được gọi.
     */
    fun signUp(name: String, email: String, pass: String) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            val result = repo.signUp(email, pass)
            result.fold(
                onSuccess = { firebaseUser ->
                    // Tạo hồ sơ người dùng trong Firestore
                    val profileResult = userRepository.createUserProfile(
                        userId = firebaseUser.uid,
                        name = name,
                        email = email
                    )
                    profileResult.fold(
                        onSuccess = { _state.value = AuthState.Success },
                        onFailure = { profileError ->
                            _state.value = AuthState.Error(
                                "Đăng ký thành công nhưng không thể tạo hồ sơ: ${profileError.message}"
                            )
                        }
                    )
                },
                onFailure = { authError ->
                    _state.value = AuthState.Error(authError.message ?: "Lỗi đăng ký không xác định")
                }
            )
        }
    }
}

