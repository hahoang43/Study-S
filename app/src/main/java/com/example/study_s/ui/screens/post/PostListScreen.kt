package com.example.study_s.ui.screens.post

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.study_s.ui.screens.components.BottomNavBar

data class Post(
    val id: Int,
    val title: String,
    val subject: String,
    val uploader: String,
    val date: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostListScreen() {
    val posts = listOf(
        Post(1, "Tài liệu IoT ESP32", "Điện tử", "Danh", "01/11/2025"),
        Post(2, "Bài giảng MATLAB cơ bản", "Tự động hóa", "Chiến", "29/10/2025"),
        Post(3, "Cấu trúc cầu cạn Metro", "Kết cấu", "Châu", "25/10/2025")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Danh sách tài liệu", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { /* quay lại */ }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2196F3),
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = { BottomNavBar() }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .safeDrawingPadding()
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
                .padding(16.dp)
        ) {
            Text(
                "Tổng số tài liệu: ${posts.size}",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(posts) { post ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(post.title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("Môn học: ${post.subject}", color = Color.Gray)
                            Text("Người đăng: ${post.uploader}", color = Color.Gray)
                            Text("Ngày đăng: ${post.date}", color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewPostListScreen() {
    PostListScreen()
}
