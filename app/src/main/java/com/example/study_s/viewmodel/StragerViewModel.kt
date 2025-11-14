package com.example.study_s.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.study_s.data.model.PostModel
import com.example.study_s.data.model.User
import com.example.study_s.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StragerViewModel : ViewModel() {

    private val userRepository = UserRepository()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    // ===== USER INFO =====
    private val _user = MutableStateFlow<User?>(null)
    val user = _user.asStateFlow()

    private val _isFollowing = MutableStateFlow(false)
    val isFollowing = _isFollowing.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    // ===== POSTS OF THAT USER =====
    private val _posts = MutableStateFlow<List<PostModel>>(emptyList())
    val posts = _posts.asStateFlow()

    // =========================
    //   LOAD USER PROFILE
    // =========================
    fun loadUserProfile(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true

            userRepository.getUserProfile(userId)
                .onSuccess { userData ->
                    _user.value = userData
                    checkFollowingStatus(userId)
                }
                .onFailure {
                    // có thể log lỗi nếu cần
                }

            // load bài viết (realtime)
            loadUserPosts(userId)

            _isLoading.value = false
        }
    }

    // =========================
    //   CHECK FOLLOW STATUS
    // =========================
    private fun checkFollowingStatus(userId: String) {
        viewModelScope.launch {
            userRepository.isFollowing(userId).onSuccess { isFollow ->
                _isFollowing.value = isFollow
            }
        }
    }

    // =========================
    //     FOLLOW / UNFOLLOW
    // =========================
    fun toggleFollow(userId: String) {
        viewModelScope.launch {
            if (_isFollowing.value) {
                // đang follow → unfollow
                userRepository.unfollowUser(userId).onSuccess {
                    _isFollowing.value = false
                    loadUserProfile(userId)   // reload follower count
                }
            } else {
                // chưa follow → follow
                userRepository.followUser(userId).onSuccess {
                    _isFollowing.value = true
                    loadUserProfile(userId)
                }
            }
        }
    }

    // =========================
    //   LOAD POSTS OF USER (REALTIME)
    // =========================
    fun loadUserPosts(userId: String) {
        db.collection("posts")
            .whereEqualTo("authorId", userId)
            .addSnapshotListener { snapshot, _ ->

                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(PostModel::class.java)?.copy(postId = doc.id)
                } ?: emptyList()

                // ===========================
                // SORT BÀI MỚI -> CŨ
                // ===========================
                val sortedList = list.sortedByDescending { post ->
                    post.timestamp?.toDate()?.time ?: 0L
                }

                _posts.value = sortedList
            }
    }
}
