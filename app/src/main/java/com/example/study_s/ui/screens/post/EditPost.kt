
package com.example.study_s.ui.screens.post

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.study_s.data.model.PostModel
// ✅ BƯỚC 1: THÊM CÁC IMPORT CẦN THIẾT
import com.example.study_s.viewmodel.AuthViewModel
import com.example.study_s.viewmodel.AuthViewModelFactory
import com.example.study_s.viewmodel.PostViewModel
import kotlinx.coroutines.flow.first

@Composable
fun EditPostScreen(
    navController: NavController,
    postViewModel: PostViewModel,
    postId: String,
    // Thêm AuthViewModel vào tham số
    authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory())
) {
    var postToEdit by remember { mutableStateOf<PostModel?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    // ✅ BƯỚC 2: KHAI BÁO BIẾN `errorMessage`
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // LaunchedEffect sẽ chạy một lần khi postId thay đổi
    // Nó sẽ yêu cầu ViewModel tải dữ liệu của bài đăng
    LaunchedEffect(key1 = postId) {
        isLoading = true

        // ✅ BƯỚC 3: CẢI THIỆN LOGIC LẤY BÀI VIẾT
        // Dùng `first()` để đảm bảo chúng ta chờ cho đến khi StateFlow có dữ liệu
        // (ít nhất là danh sách rỗng), thay vì `firstOrNull()` có thể trả về null ngay
        val postList = postViewModel.posts.first()
        val post = postList.find { it.postId == postId }

        if (post != null) {
            postToEdit = post
        } else {
            // Xử lý trường hợp không tìm thấy bài đăng
            errorMessage = "Không tìm thấy bài đăng hoặc đã bị xóa."
        }
        isLoading = false
    }

    if (isLoading) {
        // Hiển thị vòng xoay loading trong khi chờ dữ liệu
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (errorMessage != null) {
        // Hiển thị thông báo lỗi nếu không tìm thấy bài đăng
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = errorMessage!!)
        }
    } else if (postToEdit != null) {
        // Khi đã có dữ liệu, gọi PostScreen và truyền đầy đủ tham số
        PostScreen(
            navController = navController,
            postViewModel = postViewModel,
            authViewModel = authViewModel,
            postToEdit = postToEdit // Truyền bài đăng cần chỉnh sửa
        )
    }
}
