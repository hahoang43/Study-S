package com.example.study_s.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.study_s.data.repository.PostRepository
import com.example.study_s.data.model.PostModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    // Lấy và đặt bài viết được chọn
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
}
