package com.example.study_s.ui.screens.post // Hoặc package bất kỳ bạn đã đặt file này

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.study_s.ui.screens.components.PostItem // ✅ IMPORT QUAN TRỌNG
import com.example.study_s.viewmodel.PostViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedPostsScreen(
    navController: NavController,
    viewModel: PostViewModel = viewModel() // Dùng chung PostViewModel
) {
    val savedPosts by viewModel.savedPosts.collectAsState()

    // Tải danh sách bài viết đã lưu khi màn hình này xuất hiện
    LaunchedEffect(Unit) {
        viewModel.loadSavedPosts()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bài viết đã lưu", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(Color(0xFFF0F2F5)), // Nền xám nhạt
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (savedPosts.isEmpty()) {
                item {
                    Text(
                        "Bạn chưa lưu bài viết nào.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                items(savedPosts) { post ->
                    // ✅ TÁI SỬ DỤNG HOÀN TOÀN 'PostItem'
                    PostItem(
                        navController = navController,
                        post = post,
                        viewModel = viewModel,
                        // Modifier padding này áp dụng cho Card bên trong PostItem
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}