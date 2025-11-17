package com.example.study_s.ui.screens.post

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.example.study_s.data.model.PostModel
import com.example.study_s.viewmodel.PostViewModel

@Composable
fun EditPostScreen(
    navController: NavController,
    viewModel: PostViewModel,
    postId: String
) {
    // Sử dụng một State để giữ dữ liệu bài đăng sẽ được tải
    var postToEdit by remember { mutableStateOf<PostModel?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // LaunchedEffect sẽ chạy một lần khi EditPostScreen được tạo
    // Nó sẽ yêu cầu ViewModel tải dữ liệu của bài đăng dựa trên postId
    LaunchedEffect(key1 = postId) {
        postToEdit = viewModel.getPostById(postId) // Giả sử ViewModel có hàm này
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
    } else {
        // Khi đã có dữ liệu, gọi PostScreen và truyền dữ liệu vào
        // Nếu không tìm thấy bài đăng, có thể xử lý lỗi ở đây
        postToEdit?.let { post ->
            PostScreen(
                navController = navController,
                viewModel = viewModel,
                postToEdit = post // Truyền bài đăng cần chỉnh sửa vào PostScreen
            )
        }
    }
}
