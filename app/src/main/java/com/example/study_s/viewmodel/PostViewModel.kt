// ĐƯỜNG DẪN: viewmodel/PostViewModel.kt
// NỘI DUNG HOÀN CHỈNH, ĐÃ SỬA LỖI CUỐI CÙNG

package com.example.study_s.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.study_s.data.model.CommentModel
import com.example.study_s.data.model.PostModel
import com.example.study_s.data.model.User
import com.example.study_s.data.repository.NotificationRepository
import com.example.study_s.data.repository.PostRepository
import com.example.study_s.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PostViewModel(
    private val postRepository: PostRepository = PostRepository(),
    private val userRepository: UserRepository = UserRepository(),
    private val notificationRepository: NotificationRepository = NotificationRepository()
) : ViewModel() {

    // --- Các StateFlow của bạn (giữ nguyên) ---
    private val _posts = MutableStateFlow<List<PostModel>>(emptyList())
    val posts = _posts.asStateFlow()

    private val _selectedPost = MutableStateFlow<PostModel?>(null)
    val selectedPost = _selectedPost.asStateFlow()

    private val _comments = MutableStateFlow<List<CommentModel>>(emptyList())
    val comments = _comments.asStateFlow()

    private val _userCache = MutableStateFlow<Map<String, User>>(emptyMap())
    val userCache = _userCache.asStateFlow()

    private val _savedPosts = MutableStateFlow<List<PostModel>>(emptyList())
    val savedPosts = _savedPosts.asStateFlow()

    private val currentUserId: String?
        get() = FirebaseAuth.getInstance().currentUser?.uid

    // --- CÁC HÀM CŨ (giữ nguyên logic gốc) ---
    fun loadPosts() { viewModelScope.launch { _posts.value = postRepository.getAllPosts() } }
    fun createNewPost(post: PostModel) { viewModelScope.launch { postRepository.createPost(post); loadPosts() } }
    fun selectPostAndLoadComments(postId: String) {
        viewModelScope.launch {
            _selectedPost.value = postRepository.getPostById(postId)
            _comments.value = postRepository.getCommentsForPost(postId)
        }
    }

    // --- CÁC HÀM ĐƯỢC NÂNG CẤP ĐỂ GỬI THÔNG BÁO ---

    // ✅ HÀM NÀY GIỜ ĐÃ ĐÚNG VÌ `toggleLike` TRẢ VỀ BOOLEAN
    fun toggleLike(postId: String) {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            val isLiked = postRepository.toggleLike(postId, userId)
            reloadStates(postId)

            if (isLiked) {
                val post = postRepository.getPostById(postId) ?: return@launch
                val actor = userRepository.getUserProfile(userId).getOrNull() ?: return@launch
                val postOwner = userRepository.getUserProfile(post.authorId).getOrNull() ?: return@launch
                notificationRepository.sendLikeNotification(post, actor, postOwner)
            }
        }
    }

    // ✅ SỬA LẠI HÀM NÀY CHO KHỚP VỚI CONSTRUCTOR CỦA CommentModel
    fun addComment(postId: String, content: String) {
        val userId = currentUserId ?: return
        if (content.isBlank()) return

        viewModelScope.launch {
            // Lấy thông tin người bình luận (actor)
            val actor = userRepository.getUserProfile(userId).getOrNull() ?: return@launch

            // Tạo đối tượng Comment - SỬA LẠI THEO CONSTRUCTOR CHUẨN
            val comment = CommentModel(
                postId = postId,
                authorId = actor.userId,
                content = content,
                authorName = actor.name,
                authorAvatar = actor.avatarUrl // Giả sử tên trường là authorAvatar
            )

            // Lưu comment vào Firestore
            postRepository.addComment(postId, comment)
            reloadStates(postId) // Cập nhật lại UI

            // Gửi thông báo sau khi lưu comment thành công
            val post = postRepository.getPostById(postId) ?: return@launch
            val postOwner = userRepository.getUserProfile(post.authorId).getOrNull() ?: return@launch
            notificationRepository.sendCommentNotification(post, actor, postOwner, content)
        }
    }


    // --- CÁC HÀM KHÁC (giữ nguyên) ---

    private fun reloadStates(postId: String) {
        viewModelScope.launch {
            if (_selectedPost.value?.postId == postId) {
                _selectedPost.value = postRepository.getPostById(postId)
                _comments.value = postRepository.getCommentsForPost(postId)
            }
            loadPosts()
        }
    }
    fun selectPost(postId: String) { viewModelScope.launch { _selectedPost.value = postRepository.getPostById(postId) } }
    fun fetchUser(userId: String) {
        if (userId.isBlank() || _userCache.value.containsKey(userId)) return
        viewModelScope.launch {
            val user = userRepository.getUserProfile(userId).getOrNull()
            if (user != null) { _userCache.update { it + (userId to user) } }
        }
    }
    fun toggleSavePost(postId: String) {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            postRepository.toggleSavePost(postId, userId)
            reloadStates(postId)
            loadSavedPosts()
        }
    }
    fun loadSavedPosts() {
        val userId = currentUserId ?: return
        viewModelScope.launch { _savedPosts.value = postRepository.getSavedPosts(userId) }
    }
}
