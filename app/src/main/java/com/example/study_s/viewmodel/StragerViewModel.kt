package com.example.study_s.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.study_s.data.model.User
import com.example.study_s.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StragerViewModel : ViewModel() {

    private val userRepository: UserRepository = UserRepository()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _user = MutableStateFlow<User?>(null)
    val user = _user.asStateFlow()

    private val _isFollowing = MutableStateFlow(false)
    val isFollowing = _isFollowing.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun loadUserProfile(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            userRepository.getUserProfile(userId).onSuccess {
                _user.value = it
                checkFollowingStatus(userId)
            }.onFailure {
                // Xử lý lỗi
            }
            _isLoading.value = false
        }
    }

    private fun checkFollowingStatus(userId: String) {
        viewModelScope.launch {
            userRepository.isFollowing(userId).onSuccess {
                _isFollowing.value = it
            }
        }
    }

    fun toggleFollow(userId: String) {
        viewModelScope.launch {
            if (_isFollowing.value) {
                userRepository.unfollowUser(userId).onSuccess {
                    _isFollowing.value = false
                    loadUserProfile(userId) // Tải lại để cập nhật số người theo dõi
                }
            } else {
                userRepository.followUser(userId).onSuccess {
                    _isFollowing.value = true
                    loadUserProfile(userId) // Tải lại để cập nhật số người theo dõi
                }
            }
        }
    }
}
