

package com.example.study_s.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.study_s.data.repository.AuthRepository
import com.example.study_s.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Sealed Class cho các sự kiện điều hướng một lần
sealed class AuthEvent {
    object OnSignOut : AuthEvent()
}

// Sealed Class cho các trạng thái của quá trình xác thực
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

    private val _event = MutableSharedFlow<AuthEvent>()
    val event = _event.asSharedFlow()

    private val _currentUser = MutableStateFlow(repo.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser

    fun reloadCurrentUser() {
        viewModelScope.launch {
            val user = repo.currentUser

            if (user == null) {
                Log.w("AuthViewModel", "Không có người dùng đăng nhập. Phát tín hiệu OnSignOut.")
                _event.emit(AuthEvent.OnSignOut)
                return@launch
            }

            try {
                // Yêu cầu Firebase làm mới thông tin từ server
                user.reload().await()
                // Lấy lại đối tượng user đã được cập nhật
                val freshUser = repo.currentUser
                // Cập nhật StateFlow để các màn hình khác nhận được
                _currentUser.value = freshUser
                Log.d("AuthViewModel", "User data reloaded for ${freshUser?.displayName}")
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Lỗi khi tải lại dữ liệu người dùng", e)
            }
        }
    }

    // --- CÁC HÀM KHÁC GIỮ NGUYÊN ---

    private suspend fun handleSuccessfulLogin(firebaseUser: FirebaseUser) {
        val profileResult = userRepository.upsertUserProfile(firebaseUser)
        if (profileResult.isSuccess) {
            _state.value = AuthState.Success
        } else {
            _state.value = AuthState.Error("Đăng nhập Auth thành công nhưng không thể đồng bộ hồ sơ.")
        }
    }

    fun signInWithEmail(email: String, pass: String) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            val result = repo.signIn(email, pass)
            result.fold(
                onSuccess = { firebaseUser -> handleSuccessfulLogin(firebaseUser) },
                onFailure = { error ->
                    // --- PHẦN VIỆT HÓA LỖI ĐĂNG NHẬP ---
                    val errorMessage = when (error) {
                        is FirebaseAuthInvalidUserException ->
                            "Tài khoản với email này không tồn tại."
                        is FirebaseAuthInvalidCredentialsException ->
                            "Sai mật khẩu. Vui lòng thử lại."
                        is FirebaseAuthException -> when (error.errorCode) {
                            "ERROR_INVALID_EMAIL" -> "Địa chỉ email không hợp lệ."
                            "ERROR_NETWORK_REQUEST_FAILED" -> "Lỗi kết nối mạng. Vui lòng kiểm tra lại."
                            else -> "Lỗi không xác định: ${error.errorCode}"
                        }
                        else -> error.message ?: "Đăng nhập thất bại."
                    }
                    _state.value = AuthState.Error(errorMessage)
                }
            )
        }
    }

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

    fun signOut() {
        viewModelScope.launch {
            repo.signOut()
            _event.emit(AuthEvent.OnSignOut)
        }
    }

    fun resetState() {
        _state.value = AuthState.Idle
    }

    fun linkPasswordToCurrentUser(password: String) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            val result = repo.linkPassword(password)
            result.fold(
                onSuccess = { _state.value = AuthState.Success },
                onFailure = { error ->
                    _state.value = AuthState.Error(error.message ?: "Không thể liên kết mật khẩu.")
                }
            )
        }
    }

    fun signUp(name: String, email: String, pass: String) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            val result = repo.signUp(email, pass)
            result.fold(
                onSuccess = { firebaseUser ->
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
                    val errorMessage = when (authError) {
                        is FirebaseAuthUserCollisionException ->
                            "Địa chỉ email này đã được sử dụng bởi một tài khoản khác."
                        is FirebaseAuthException -> when (authError.errorCode) {
                            "ERROR_WEAK_PASSWORD" -> "Mật khẩu quá yếu. Vui lòng sử dụng ít nhất 6 ký tự."
                            "ERROR_INVALID_EMAIL" -> "Địa chỉ email không hợp lệ."
                            "ERROR_NETWORK_REQUEST_FAILED" -> "Lỗi kết nối mạng. Vui lòng kiểm tra lại."
                            else -> "Lỗi không xác định: ${authError.errorCode}"
                        }
                        else -> authError.message ?: "Lỗi đăng ký không xác định"
                    }
                    _state.value = AuthState.Error(errorMessage)
                }
            )
        }
    }
}
