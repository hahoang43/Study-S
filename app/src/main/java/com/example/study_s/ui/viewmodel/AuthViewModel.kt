package com.example.study_s.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.study_s.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Giả sử bạn sẽ dùng ResultState để quản lý trạng thái UI
sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {

    private val authRepository = AuthRepository()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState = _authState.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.login(email, password)
            result.onSuccess {
                _authState.value = AuthState.Success
            }.onFailure {
                _authState.value = AuthState.Error(it.message ?: "Unknown error")
            }
        }
    }

    fun register(email: String, password: String, username: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.register(email, password, username)
            result.onSuccess {
                _authState.value = AuthState.Success
            }.onFailure {
                _authState.value = AuthState.Error(it.message ?: "Unknown error")
            }
        }
    }

    fun logout() {
        authRepository.logout()
    }
}
