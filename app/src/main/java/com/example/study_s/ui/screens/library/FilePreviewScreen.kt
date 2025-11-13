package com.example.study_s.ui.screens.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavController
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilePreviewScreen(navController: NavController, fileUrl: String?, fileName: String?) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = fileName ?: "Xem trước tài liệu",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (fileUrl != null) {
                // Xác định loại file dựa trên tên
                when {
                    // Nếu là file ảnh (jpg, png, webp,...) thì hiển thị ảnh
                    fileName?.endsWith(".jpg", ignoreCase = true) == true ||
                            fileName?.endsWith(".jpeg", ignoreCase = true) == true ||
                            fileName?.endsWith(".png", ignoreCase = true) == true ||
                            fileName?.endsWith(".webp", ignoreCase = true) == true -> {
                        AsyncImage(
                            model = fileUrl,
                            contentDescription = fileName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                    // Nếu là PDF hoặc các loại file khác, tạm thời hiển thị thông báo
                    // (Bạn có thể tích hợp thư viện xem PDF ở đây sau này)
                    else -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Không thể hiển thị loại file này trực tiếp.")
                            Text(text = "Tên file: $fileName")
                        }
                    }
                }
            } else {
                Text("Không có URL của file để hiển thị.")
            }
        }
    }
}
