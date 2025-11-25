// ĐƯỜNG DẪN: com/example/study_s/viewmodel/StragerViewModel.kt
// NỘI DUNG HOÀN CHỈNH, ĐÃ SỬA LỖI

package com.example.study_s.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
// KHÔNG CẦN IMPORT Notification nữa, ViewModel không nên biết về nó
import com.example.study_s.data.model.PostModel
import com.example.study_s.data.model.UserModel
import com.example.study_s.data.repository.NotificationRepository
import com.example.study_s.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StragerViewModel : ViewModel() {

    // --- KHỞI TẠO ---
    private val userRepository = UserRepository()
    private val notificationRepository = NotificationRepository()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    // --- STATE CHO THÔNG TIN USER ---
    private val _userModel = MutableStateFlow<UserModel?>(null)
    val user = _userModel.asStateFlow()

    private val _isFollowing = MutableStateFlow(false)
    val isFollowing = _isFollowing.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    // --- STATE CHO DANH SÁCH BÀI VIẾT ---
    private val _posts = MutableStateFlow<List<PostModel>>(emptyList())
    val posts = _posts.asStateFlow()


    // =========================
    //   CHỨC NĂNG CHÍNH
    // =========================

    fun loadUserProfile(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            userRepository.getUserProfile(userId)
                .onSuccess { userData ->
                    _userModel.value = userData
                    checkFollowingStatus(userId)
                }
            loadUserPosts(userId)
            _isLoading.value = false
        }
    }

    fun toggleFollow(userIdToFollow: String) {
        viewModelScope.launch {
            if (_isFollowing.value) {
                // --- UNFOLLOW ---
                userRepository.unfollowUser(userIdToFollow).onSuccess {
                    _isFollowing.value = false
                    // Tải lại để cập nhật số follower
                    loadUserProfile(userIdToFollow)
                }
            } else {
                // --- FOLLOW ---
                userRepository.followUser(userIdToFollow).onSuccess {
                    _isFollowing.value = true
                    // Tải lại để cập nhật số follower
                    loadUserProfile(userIdToFollow)
                    // Gửi thông báo
                    sendFollowNotification(userIdToFollow)
                }
            }
        }
    }

    private fun loadUserPosts(userId: String) {
        db.collection("posts")
            .whereEqualTo("authorId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("LoadUserPosts", "Listen failed.", e)
                    return@addSnapshotListener
                }
                _posts.value = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(PostModel::class.java)?.copy(postId = doc.id)
                } ?: emptyList()
            }
    }

    // =========================
    //   CÁC HÀM HỖ TRỢ (PRIVATE)
    // =========================

    private fun checkFollowingStatus(userId: String) {
        viewModelScope.launch {
            userRepository.isFollowing(userId).onSuccess { isFollow ->
                _isFollowing.value = isFollow
            }
        }
    }

    // ✅ SỬA LẠI HOÀN TOÀN HÀM NÀY CHO SẠCH SẼ
    private fun sendFollowNotification(userToNotifyId: String) {
        viewModelScope.launch {
            // 1. Lấy thông tin người đi follow (actor)
            val currentUserId = auth.currentUser?.uid ?: return@launch
            val actor = userRepository.getUserProfile(currentUserId).getOrNull() ?: return@launch

            // 2. Lấy thông tin người được follow (người nhận thông báo)
            val userToNotify = userRepository.getUserProfile(userToNotifyId).getOrNull() ?: return@launch

            // 3. Ủy quyền toàn bộ việc gửi thông báo cho Repository
            notificationRepository.sendFollowNotification(actor, userToNotify)
        }
    }
}
