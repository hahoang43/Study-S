package com.example.study_s.ui.screens.post

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.study_s.ui.screens.components.PostItem
import com.example.study_s.viewmodel.PostViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedPostsScreen(
    navController: NavController,
    viewModel: PostViewModel = viewModel()
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
                // ✅ SỬA 1: Sử dụng màu từ MaterialTheme
                // Tự động chọn màu nền cho TopAppBar dựa trên theme (sáng/tối)
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                // ✅ SỬA 2: Sử dụng màu nền chính của màn hình từ MaterialTheme
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (savedPosts.isEmpty()) {
                item {
                    Text(
                        text = "Bạn chưa lưu bài viết nào.",
                        modifier = Modifier.padding(16.dp),
                        // ✅ SỬA 3: Sử dụng màu chữ và style từ MaterialTheme
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(savedPosts) { post ->
                    PostItem(
                        navController = navController,
                        post = post,
                        viewModel = viewModel,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}
