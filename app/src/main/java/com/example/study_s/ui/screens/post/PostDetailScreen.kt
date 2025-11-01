package com.example.study_s.ui.screens.post

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen() {
    val post = Post(
        id = 1,
        title = "Tài liệu IoT ESP32",
        subject = "Điện tử",
        uploader = "Danh",
        date = "01/11/2025"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chi tiết tài liệu", fontWeight = FontWeight.Bold) },
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
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(post.title, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Môn học: ${post.subject}", fontSize = 15.sp, color = Color.Gray)
            Text("Người đăng: ${post.uploader}", fontSize = 15.sp, color = Color.Gray)
            Text("Ngày đăng: ${post.date}", fontSize = 15.sp, color = Color.Gray)
            Spacer(Modifier.height(16.dp))
            Text(
                "📄 Nội dung: Đây là tài liệu hướng dẫn chi tiết cách lập trình và giao tiếp ESP32 " +
                        "trong các ứng dụng IoT thực tế. Bao gồm cấu trúc mạch, lập trình Wi-Fi, " +
                        "và truyền dữ liệu cảm biến lên hệ thống giám sát.",
                fontSize = 15.sp
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewPostDetailScreen() {
    PostDetailScreen()
}
