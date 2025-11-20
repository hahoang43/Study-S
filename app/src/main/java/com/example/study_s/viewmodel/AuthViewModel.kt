package com.example.study_s.viewmodel
import kotlinx.coroutines.tasks.await

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
import android.util.Log
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
    // 1. Tạo StateFlow để cung cấp thông tin người dùng cho các màn hình khác
    private val _currentUser = MutableStateFlow(repo.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser

    // ✅✅✅ THAY THẾ TOÀN BỘ HÀM CŨ BẰNG HÀM NÀY ✅✅✅
    /**
     * Hàm này được SettingScreen gọi để làm mới thông tin.
     * Nó trực tiếp sử dụng FirebaseAuth để tải lại dữ liệu từ server
     * và cập nhật StateFlow.
     */
    fun reloadUserData() {
        viewModelScope.launch {
            val user = repo.currentUser // Lấy người dùng hiện tại từ repository

            // BƯỚC 1: KIỂM TRA BẢO VỆ
            // Nếu không có người dùng nào đăng nhập, phát tín hiệu đăng xuất
            // để tự động đẩy người dùng về màn hình Login.
            if (user == null) {
                Log.w("AuthViewModel", "Không có người dùng đăng nhập. Phát tín hiệu OnSignOut.")
                _event.emit(AuthEvent.OnSignOut)
                return@launch
            }

            // BƯỚC 2: TẢI LẠI DỮ LIỆU TỪ SERVER
            // Nếu có người dùng, yêu cầu Firebase làm mới thông tin của họ.
            try {
                user.reload().await() // Chờ cho đến khi việc tải lại hoàn tất
                // Sau khi reload, repo.currentUser sẽ tự động được cập nhật
                val freshUser = repo.currentUser
                _currentUser.value = freshUser // Cập nhật StateFlow với thông tin mới nhất
                Log.d("AuthViewModel", "User data reloaded for ${freshUser?.displayName}")
            } catch (e: Exception) {
                // Xử lý các lỗi có thể xảy ra khi reload (ví dụ: mất mạng)
                Log.e("AuthViewModel", "Lỗi khi tải lại dữ liệu người dùng", e)
            }
        }
    }

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

    fun linkPasswordToCurrentUser(password: String) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            // Gọi hàm linkPassword từ AuthRepository
            val result = repo.linkPassword(password)
            result.fold(
                onSuccess = {
                    // Liên kết thành công, hồ sơ đã tồn tại, chỉ cần báo thành công
                    _state.value = AuthState.Success
                },
                onFailure = { error ->
                    _state.value = AuthState.Error(error.message ?: "Không thể liên kết mật khẩu.")
                }
            )
        }
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

