package com.example.study_s.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.study_s.data.model.FollowUserDataModel
import com.example.study_s.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FollowListViewModel : ViewModel() {
    private val userRepository = UserRepository()

    private val _userList = MutableStateFlow<List<FollowUserDataModel>>(emptyList())
    val userList = _userList.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun loadUserList(userId: String, listType: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = if (listType == "followers") {
                userRepository.getFollowers(userId)
            } else {
                userRepository.getFollowing(userId)
            }

            result.onSuccess {
                _userList.value = it
            }.onFailure {
                // Xử lý lỗi, có thể hiển thị Toast hoặc thông báo trên màn hình
                _userList.value = emptyList()
            }
            _isLoading.value = false
        }
    }
}
