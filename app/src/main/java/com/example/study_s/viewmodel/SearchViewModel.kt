package com.example.study_s.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope

// 1. Import đầy đủ các model và repository cần thiết
import com.example.study_s.data.model.Group
import com.example.study_s.data.model.LibraryFile
import com.example.study_s.data.model.PostModel
import com.example.study_s.data.model.User
import com.example.study_s.data.repository.GroupRepository
import com.example.study_s.data.repository.LibraryRepository
import com.example.study_s.data.repository.PostRepository
import com.example.study_s.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// 2. Nâng cấp `SearchState` để chứa tất cả các loại kết quả
data class SearchState(
    val users: List<User> = emptyList(),
    val posts: List<PostModel> = emptyList(),
    val groups: List<Group> = emptyList(),          // <-- Thêm trường này
    val files: List<LibraryFile> = emptyList(),     // <-- Thêm trường này
    val isLoading: Boolean = false,
    val error: String? = null
)

// 3. Nâng cấp `SearchViewModel` để yêu cầu cả 4 repository
class SearchViewModel(
    private val userRepository: UserRepository,
    private val postRepository: PostRepository,
    private val groupRepository: GroupRepository,       // <-- Thêm tham số này
    private val libraryRepository: LibraryRepository  // <-- Thêm tham số này
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("Người dùng") // Mặc định là "Người dùng"
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _searchState = MutableStateFlow(SearchState())
    val searchState = _searchState.asStateFlow()

    // Hàm này không đổi, nhận thay đổi từ ô tìm kiếm
    fun onQueryChange(query: String) {
        _searchQuery.value = query
        // Nếu người dùng xóa hết chữ, reset kết quả ngay lập tức
        if (query.isBlank()) {
            _searchState.value = SearchState()
        }
    }

    // Hàm này được gọi khi người dùng nhấn vào một chip (FilterChip)
    fun onCategoryChange(category: String) {
        _selectedCategory.value = category
        // Thực hiện lại tìm kiếm ngay khi đổi danh mục (nếu có chữ trong ô tìm kiếm)
        if (_searchQuery.value.isNotBlank()) {
            performSearch()
        } else {
            // Nếu chưa có chữ, chỉ cần reset kết quả cho sạch
            _searchState.value = SearchState()
        }
    }

    // 4. Nâng cấp `performSearch` để tìm kiếm đúng danh mục đang được chọn
    fun performSearch() {
        val query = _searchQuery.value
        // Không tìm kiếm nếu ô nhập trống
        if (query.isBlank()) return

        viewModelScope.launch {
            _searchState.value = SearchState(isLoading = true) // Bắt đầu loading, xóa kết quả cũ
            try {
                // Dùng `when` để gọi đúng hàm tìm kiếm của repository tương ứng
                when (_selectedCategory.value) {
                    "Người dùng" -> {
                        val result = userRepository.searchUsers(query)
                        _searchState.value = SearchState(users = result)
                    }
                    "Bài viết" -> {
                        val result = postRepository.searchPosts(query)
                        _searchState.value = SearchState(posts = result)
                    }
                    "Nhóm" -> {
                        val result = groupRepository.searchGroups(query)
                        _searchState.value = SearchState(groups = result)
                    }
                    "Tài liệu" -> {
                        val result = libraryRepository.searchFiles(query)
                        _searchState.value = SearchState(files = result)
                    }
                }
            } catch (e: Exception) {
                // Ghi lại lỗi và cập nhật UI để người dùng biết
                Log.e("SearchViewModel", "Error during search in category '${_selectedCategory.value}'", e)
                _searchState.value = SearchState(error = "Lỗi tìm kiếm: ${e.message}")
            }
        }
    }
}

// 5. Nâng cấp `SearchViewModelFactory` để cung cấp đủ 4 repository
class SearchViewModelFactory(
    private val userRepository: UserRepository,
    private val postRepository: PostRepository,
    private val groupRepository: GroupRepository,
    private val libraryRepository: LibraryRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
            // Trả về một SearchViewModel với đầy đủ 4 repository
            return SearchViewModel(userRepository, postRepository, groupRepository, libraryRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}