package com.example.study_s.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.study_s.data.model.CommentModel // <-- THÊM
import com.example.study_s.data.repository.PostRepository
import com.example.study_s.data.model.PostModel
import com.example.study_s.data.model.User
import com.google.firebase.auth.FirebaseAuth // <-- THÊM
import com.google.firebase.firestore.FirebaseFirestore // <-- 2. IMPORT
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update // <-- 3. IMPORT
import kotlinx.coroutines.launch

class PostViewModel(
    private val repository: PostRepository = PostRepository()
) : ViewModel() {

    // Danh sách bài viết
    private val _posts = MutableStateFlow<List<PostModel>>(emptyList())
    val posts = _posts.asStateFlow()

    // Bài viết được chọn để xem chi tiết
    private val _selectedPost = MutableStateFlow<PostModel?>(null)
    val selectedPost = _selectedPost.asStateFlow()

    // 💬 MỚI: Danh sách bình luận
    private val _comments = MutableStateFlow<List<CommentModel>>(emptyList())
    val comments = _comments.asStateFlow()

    // 🙋‍♂️ MỚI: Lấy user ID hiện tại
    private val currentUserId: String?
        get() = FirebaseAuth.getInstance().currentUser?.uid
    private val _userCache = MutableStateFlow<Map<String, User>>(emptyMap())
    val userCache = _userCache.asStateFlow()
    // Tải danh sách bài đăng từ Firestore
    fun loadPosts() {
        viewModelScope.launch {
            try {
                _posts.value = repository.getAllPosts()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Tạo bài đăng mới
    fun createNewPost(post: PostModel) {
        viewModelScope.launch {
            try {
                repository.createPost(post)
                loadPosts() // Tải lại danh sách sau khi tạo
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // 📦 MỚI: Lấy chi tiết bài đăng VÀ bình luận
    fun selectPostAndLoadComments(postId: String) {
        viewModelScope.launch {
            try {
                _selectedPost.value = repository.getPostById(postId)
                _comments.value = repository.getCommentsForPost(postId) // Tải comment
            } catch (e: Exception) {
                e.printStackTrace()
                _selectedPost.value = null
                _comments.value = emptyList()
            }
        }
    }

    // 🩷 MỚI: Xử lý Like/Unlike
    fun toggleLike(postId: String) {
        val userId = currentUserId ?: return // Cần user id
        viewModelScope.launch {
            try {
                repository.toggleLike(postId, userId)
                // Cập nhật lại state của post
                reloadStates(postId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // 💬 MỚI: Thêm bình luận
    fun addComment(postId: String, content: String) {
        val userId = currentUserId ?: return
        if (content.isBlank()) return

        val comment = CommentModel(
            postId = postId,
            authorId = userId,
            content = content
        )

        viewModelScope.launch {
            try {
                repository.addComment(postId, comment)
                // Tải lại comment và post (để update count)
                reloadStates(postId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // 🔄 MỚI: Hàm private helper để refresh data
    private fun reloadStates(postId: String) {
        viewModelScope.launch {
            // Tải lại post chi tiết (nếu đang xem)
            if (_selectedPost.value?.postId == postId) {
                _selectedPost.value = repository.getPostById(postId)
                _comments.value = repository.getCommentsForPost(postId)
            }
            // Tải lại list posts (để cập nhật count ở HomeScreen)
            loadPosts()
        }
    }

    // Sửa hàm cũ (chỉ dùng nếu không cần load comment)
    fun selectPost(postId: String) {
        viewModelScope.launch {
            try {
                _selectedPost.value = repository.getPostById(postId)
            } catch (e: Exception) {
                e.printStackTrace()
                _selectedPost.value = null
            }
        }
    }
    // 5. HÀM MỚI: TẢI THÔNG TIN NGƯỜI DÙNG VÀ LƯU VÀO CACHE
    fun fetchUser(userId: String) {
        if (userId.isBlank() || _userCache.value.containsKey(userId)) {
            return
        }

        viewModelScope.launch {
            try {
                FirebaseFirestore.getInstance().collection("users").document(userId)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document != null && document.exists()) {
                            // SỬA: UserModel -> User
                            val user = document.toObject(User::class.java)?.copy(userId = document.id)
                            if (user != null) {
                                _userCache.update { currentCache ->
                                    currentCache + (userId to user)
                                }
                            }
                        } else {
                            // SỬA: UserModel -> User, username -> name
                            _userCache.update { currentCache ->
                                currentCache + (userId to User(userId = userId, name = "Người dùng ẩn danh"))
                            }
                        }
                    }
                    .addOnFailureListener {
                        // SỬA: UserModel -> User, username -> name
                        _userCache.update { currentCache ->
                            currentCache + (userId to User(userId = userId, name = "Lỗi tải tên"))
                        }
                    }
            } catch (e: Exception) {
                e.printStackTrace()
                // SỬA: UserModel -> User, username -> name
                _userCache.update { currentCache ->
                    currentCache + (userId to User(userId = userId, name = "Lỗi tải tên"))
                }
            }
        }
    }
}