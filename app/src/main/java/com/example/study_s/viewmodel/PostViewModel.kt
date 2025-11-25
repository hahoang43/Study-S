package com.example.study_s.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.study_s.data.model.CommentModel
import com.example.study_s.data.model.PostModel
import com.example.study_s.data.model.UserModel
import com.example.study_s.data.repository.NotificationRepository
import com.example.study_s.data.repository.PostRepository
import com.example.study_s.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PostViewModel(
    private val postRepository: PostRepository = PostRepository(),
    private val userRepository: UserRepository = UserRepository(),
    private val notificationRepository: NotificationRepository = NotificationRepository()
) : ViewModel() {

    // --- StateFlows và các biến khác (giữ nguyên) ---
    private val _posts = MutableStateFlow<List<PostModel>>(emptyList())
    val posts = _posts.asStateFlow()

    private val _selectedPost = MutableStateFlow<PostModel?>(null)
    val selectedPost = _selectedPost.asStateFlow()

    private val _comments = MutableStateFlow<List<CommentModel>>(emptyList())
    val comments = _comments.asStateFlow()

    private val _userModelCache = MutableStateFlow<Map<String, UserModel>>(emptyMap())
    val userCache = _userModelCache.asStateFlow()

    private val _savedPosts = MutableStateFlow<List<PostModel>>(emptyList())
    val savedPosts = _savedPosts.asStateFlow()

    private val _errorEvent = MutableSharedFlow<String>()
    val errorEvent = _errorEvent.asSharedFlow()

    private val currentUserId: String?
        get() = FirebaseAuth.getInstance().currentUser?.uid

    private val _scrollToTopEvent = MutableSharedFlow<Unit>()
    val scrollToTopEvent = _scrollToTopEvent.asSharedFlow()


    fun addComment(postId: String, newComment: CommentModel) { // ✅ ĐỊNH NGHĨA HÀM CHUẨN
        // Lấy userId từ đối tượng newComment thay vì gọi lại FirebaseAuth
        val userId = newComment.authorId
        if (userId.isBlank()) {
            viewModelScope.launch { _errorEvent.emit("Lỗi xác thực người dùng.") }
            return
        }

        viewModelScope.launch {
            try {
                // Bước 1: Truyền thẳng đối tượng newComment xuống Repository
                // Repository sẽ chịu trách nhiệm thêm các trường server-side (như timestamp)
                // và lưu vào Firestore.
                postRepository.addComment(postId, newComment)

                // Bước 2: Tải lại toàn bộ trạng thái để UI cập nhật
                reloadStates(postId)

                // Bước 3 (Tùy chọn): Gửi thông báo
                val post = _selectedPost.value ?: return@launch
                val actor = userRepository.getUserProfile(userId).getOrNull()
                val postOwner = userRepository.getUserProfile(post.authorId).getOrNull()

                if (actor != null && postOwner != null) {
                    notificationRepository.sendCommentNotification(post, actor, postOwner, newComment.content)
                }

            } catch (e: Exception) {
                Log.e("PostViewModel", "Lỗi khi thêm bình luận: ${e.message}", e)
                _errorEvent.emit("Không thể thêm bình luận. Vui lòng thử lại.")
            }
        }
    }


    // --- HÀM SỬA BÌNH LUẬN (Đã thêm try-catch và gửi sự kiện lỗi) ---
    fun updateComment(postId: String, commentId: String, newContent: String) {
        // Kiểm tra để tránh gửi yêu cầu với ID rỗng
        if (commentId.isBlank()) {
            viewModelScope.launch { _errorEvent.emit("Lỗi: Không tìm thấy bình luận để sửa.") }
            return
        }

        viewModelScope.launch {
            try {
                postRepository.updateComment(postId, commentId, newContent)
                // Tải lại bình luận để cập nhật giao diện
                reloadStates(postId)
            } catch (e: Exception) {
                Log.e("PostViewModel", "Lỗi khi cập nhật bình luận: ${e.message}", e)
                if (e is com.google.firebase.firestore.FirebaseFirestoreException && e.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.NOT_FOUND) {
                    _errorEvent.emit("Bình luận này không còn tồn tại.")
                } else {
                    _errorEvent.emit("Không thể cập nhật bình luận.")
                }
            }
        }
    }


    // --- HÀM XÓA BÌNH LUẬN (Đã thêm try-catch và gửi sự kiện lỗi) ---
    fun deleteComment(postId: String, commentId: String) {
        // Kiểm tra để tránh gửi yêu cầu với ID rỗng
        if (commentId.isBlank()) {
            viewModelScope.launch { _errorEvent.emit("Lỗi: Không tìm thấy bình luận để xóa.") }
            return
        }

        viewModelScope.launch {
            try {
                postRepository.deleteComment(postId, commentId)
                // Tải lại bình luận và bài viết để cập nhật số lượng
                reloadStates(postId)
            } catch (e: Exception) {
                Log.e("PostViewModel", "Lỗi khi xóa bình luận: ${e.message}", e)
                if (e is com.google.firebase.firestore.FirebaseFirestoreException && e.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.NOT_FOUND) {
                    _errorEvent.emit("Bình luận này không còn tồn tại.")
                } else {
                    _errorEvent.emit("Không thể xóa bình luận.")
                }
            }
        }
    }


    // --- CÁC HÀM KHÁC (giữ nguyên, không cần sửa) ---

    private fun reloadStates(postId: String) {
        viewModelScope.launch {
            // Cập nhật post đang chọn và danh sách bình luận của nó
            _selectedPost.value = postRepository.getPostById(postId)
            _comments.value = postRepository.getCommentsForPost(postId)
            // Cập nhật lại danh sách bài đăng chính (để update like/comment count ở HomeScreen)
            loadPosts()
        }
    }

    fun loadPosts() { viewModelScope.launch { _posts.value = postRepository.getAllPosts() } }
    fun createNewPost(post: PostModel) { viewModelScope.launch { postRepository.createPost(post); loadPosts() } }
    fun selectPostAndLoadComments(postId: String) {
        if (postId.isBlank()) {
            Log.e("PostViewModel", "LỖI: selectPostAndLoadComments được gọi với postId rỗng!")
            viewModelScope.launch { _errorEvent.emit("Không thể tải bài viết, ID không hợp lệ.") }
            return
        }

        viewModelScope.launch {
            _selectedPost.value = postRepository.getPostById(postId)
            _comments.value = postRepository.getCommentsForPost(postId)
        }
    }
    fun toggleLike(postId: String) {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            try {
                // Bước 1: Gọi Repository và nhận lại bài post đã cập nhật
                val updatedPost = postRepository.toggleLike(postId, userId)

                // Bước 2: Kiểm tra xem có phải là hành động LIKE hay không
                if (updatedPost != null) {
                    val isNowLiked = updatedPost.likedBy.contains(userId)
                    if (isNowLiked) {
                        // Bước 3: Nếu là LIKE, lấy thông tin các bên và gọi NotificationRepository
                        val actor = userRepository.getUserProfile(userId).getOrNull()
                        val postOwner = userRepository.getUserProfile(updatedPost.authorId).getOrNull()

                        if (actor != null && postOwner != null) {
                            // Ra lệnh cho "nhà máy" NotificationRepository tạo thông báo "like"
                            notificationRepository.sendLikeNotification(updatedPost, actor, postOwner)
                        }
                    }
                }

                // Bước 4: Tải lại UI (luôn thực hiện dù là like hay unlike)
                reloadStates(postId)

            } catch (e: Exception) {
                Log.e("PostViewModel", "Lỗi khi toggle like: ${e.message}", e)
                _errorEvent.emit("Đã có lỗi xảy ra.")
            }
        }
    }
    fun selectPost(postId: String) { viewModelScope.launch { _selectedPost.value = postRepository.getPostById(postId) } }
    fun fetchUser(userId: String) {
        if (userId.isBlank() || _userModelCache.value.containsKey(userId)) return
        viewModelScope.launch {
            val user = userRepository.getUserProfile(userId).getOrNull()
            if (user != null) { _userModelCache.update { it + (userId to user) } }
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
    fun deletePost(postId: String) {
        viewModelScope.launch {
            try {
                postRepository.deletePost(postId)
                _posts.update { currentPosts ->
                    currentPosts.filterNot { it.postId == postId }
                }
            } catch (e: Exception) {
                Log.e("PostViewModel", "Lỗi khi xóa bài viết: ${e.message}", e)
            }
        }
    }
    fun updatePost(post: PostModel, onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                postRepository.updatePost(post)
                _posts.update { currentPosts ->
                    currentPosts.map { if (it.postId == post.postId) post else it }
                }
                onComplete()
            } catch (e: Exception) {
                Log.e("PostViewModel", "Lỗi khi cập nhật bài đăng: ${e.message}", e)
            }
        }
    }
    suspend fun getPostById(postId: String): PostModel? {
        return postRepository.getPostById(postId)
    }
    fun reloadPostsAndScrollToTop() {
        viewModelScope.launch {
            _posts.value = postRepository.getAllPosts()
            _scrollToTopEvent.emit(Unit)
        }
    }
}
