// ĐƯỜNG DẪN: viewmodel/AccountViewModel.kt
// NỘI DUNG HOÀN CHỈNH

package com.example.study_s.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.study_s.data.repository.UserRepository
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Enum để quản lý các trạng thái của quá trình xóa
enum class AccountDeletionState {
    IDLE,                      // Trạng thái bình thường
    REQUIRES_REAUTH,         // Cần người dùng nhập mật khẩu
    DELETING,                  // Đang thực hiện xóa
    SUCCESS,                   // Xóa thành công
    ERROR,                     // Có lỗi xảy ra (lỗi chung)
    ERROR_WRONG_PASSWORD       // Lỗi sai mật khẩu
}

class AccountViewModel(
    private val userRepository: UserRepository = UserRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _deletionState = MutableStateFlow(AccountDeletionState.IDLE)
    val deletionState = _deletionState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    /**
     * Bắt đầu quy trình xóa tài khoản bằng cách yêu cầu xác thực lại.
     */
    fun startAccountDeletionProcess() {
        _deletionState.value = AccountDeletionState.REQUIRES_REAUTH
    }

    /**
     * Hàm quan trọng: Xác thực lại và tiến hành xóa nếu thành công.
     * @param password Mật khẩu người dùng nhập vào.
     */
    fun reauthenticateAndDeleteAccount(password: String) {
        val user = auth.currentUser
        if (user == null || user.email == null) {
            _errorMessage.value = "Không thể xác định người dùng."
            _deletionState.value = AccountDeletionState.ERROR
            return
        }

        _deletionState.value = AccountDeletionState.DELETING // Báo cho UI biết là đang xử lý

        val credential = EmailAuthProvider.getCredential(user.email!!, password)

        user.reauthenticate(credential)
            .addOnSuccessListener {
                Log.d("AccountViewModel", "Xác thực lại thành công. Bắt đầu xóa tài khoản.")
                performActualDeletion()
            }
            .addOnFailureListener { exception ->
                Log.e("AccountViewModel", "Lỗi khi xác thực lại", exception)
                when (exception) {
                    is FirebaseAuthInvalidCredentialsException -> {
                        _errorMessage.value = "Mật khẩu không chính xác."
                        _deletionState.value = AccountDeletionState.ERROR_WRONG_PASSWORD
                    }
                    else -> {
                        _errorMessage.value = "Đã xảy ra lỗi khi xác thực. Vui lòng thử lại."
                        _deletionState.value = AccountDeletionState.ERROR
                    }
                }
            }
    }

    /**
     * Gọi hàm xóa từ Repository.
     */
    private fun performActualDeletion() {
        viewModelScope.launch {
            val result = userRepository.deleteUserAccount()
            if (result.isSuccess) {
                _deletionState.value = AccountDeletionState.SUCCESS
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Lỗi không xác định khi xóa dữ liệu."
                _deletionState.value = AccountDeletionState.ERROR
            }
        }
    }

    /**
     * Reset lại trạng thái để đóng dialog và xóa thông báo lỗi.
     */
    fun resetDeletionState() {
        _deletionState.value = AccountDeletionState.IDLE
        _errorMessage.value = null
    }
}
