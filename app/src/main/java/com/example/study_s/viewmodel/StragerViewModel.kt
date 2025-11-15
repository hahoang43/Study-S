// ĐƯỜNG DẪN: com/example/study_s/viewmodel/StragerViewModel.kt

package com.example.study_s.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
// DÒNG IMPORT QUAN TRỌNG NHẤT
import com.example.study_s.data.model.Notification
import com.example.study_s.data.model.PostModel
import com.example.study_s.data.model.User
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
    private val _user = MutableStateFlow<User?>(null)
    val user = _user.asStateFlow()

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
                    _user.value = userData
                    checkFollowingStatus(userId)
                }
            loadUserPosts(userId)
            _isLoading.value = false
        }
    }

    fun toggleFollow(userId: String) {
        viewModelScope.launch {
            if (_isFollowing.value) {
                userRepository.unfollowUser(userId).onSuccess {
                    _isFollowing.value = false
                    loadUserProfile(userId)
                }
            } else {
                userRepository.followUser(userId).onSuccess {
                    _isFollowing.value = true
                    loadUserProfile(userId)
                    sendFollowNotification(userId)
                }
            }
        }
    }

    fun loadUserPosts(userId: String) {
        db.collection("posts")
            .whereEqualTo("authorId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("LoadUserPosts", "Listen failed.", e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(PostModel::class.java)?.copy(postId = doc.id)
                    }
                    _posts.value = list
                } else {
                    _posts.value = emptyList()
                }
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

    private fun sendFollowNotification(userToNotifyId: String) {
        viewModelScope.launch {
            val currentUserId = auth.currentUser?.uid ?: return@launch
            val follower = userRepository.getUserProfile(currentUserId).getOrNull()

            // DÒNG CODE SỬ DỤNG "Notification"
            val notification = Notification(
                userId = userToNotifyId,
                actorId = currentUserId,
                actorName = follower?.name ?: "Một người nào đó",
                actorAvatarUrl = follower?.avatarUrl,
                type = "follow",
                message = "đã bắt đầu theo dõi bạn."
            )
            notificationRepository.saveNotificationToFirestore(notification)

            val userToNotify = userRepository.getUserProfile(userToNotifyId).getOrNull()
            userToNotify?.fcmToken?.let { token ->
                if (token.isNotEmpty()) {
                    val title = "Bạn có lượt theo dõi mới!"
                    val body = "${follower?.name ?: "Một người nào đó"} đã bắt đầu theo dõi bạn."
                    notificationRepository.sendPushNotification(token, title, body)
                }
            }
        }
    }
}
