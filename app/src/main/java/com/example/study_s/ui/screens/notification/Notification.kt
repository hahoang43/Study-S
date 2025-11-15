// ĐƯỜNG DẪN: ui/screens/notification/NotificationScreen.kt

package com.example.study_s.ui.screens.notification

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.isEmpty
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.study_s.R
import com.example.study_s.data.model.Notification
import com.example.study_s.ui.theme.Study_STheme
import com.example.study_s.viewmodel.NotificationViewModel

@Composable
fun NotificationScreen(
    notificationViewModel: NotificationViewModel = viewModel()
) {
    // Lấy danh sách thông báo THẬT từ StateFlow của ViewModel
    val notifications by notificationViewModel.notifications.collectAsState()
    // Lấy trạng thái loading từ ViewModel
    val isLoading by notificationViewModel.isLoading.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Thanh tiêu đề trên cùng
            TopAppBar()

            // Phần nội dung chính
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Hoạt động",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )

                // Hiển thị vòng xoay loading khi đang tải dữ liệu
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                // Hiển thị thông báo khi danh sách rỗng sau khi đã tải xong
                else if (notifications.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Chưa có hoạt động nào.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                }
                // Hiển thị danh sách thông báo
                else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(notifications) { notification ->
                            NotificationItemView(notification = notification)
                            Divider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 1.dp)
                        }
                    }
                }
            }
        }
    }
}

// Composable cho một mục thông báo đơn lẻ
@Composable
fun NotificationItemView(notification: Notification) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Dấu chấm đỏ cho thông báo chưa đọc
        if (!notification.isRead) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color.Red)
            )
            Spacer(modifier = Modifier.width(10.dp))
        } else {
            // Thêm khoảng trống để căn chỉnh nếu thông báo đã đọc
            Spacer(modifier = Modifier.width(18.dp))
        }

        // Avatar của người gửi
        AsyncImage(
            model = notification.actorAvatarUrl, // Tải ảnh từ URL
            contentDescription = "Avatar của ${notification.actorName}",
            placeholder = painterResource(id = R.drawable.avatar), // Ảnh mặc định
            error = painterResource(id = R.drawable.avatar), // Ảnh khi lỗi
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Nội dung thông báo
        Column(modifier = Modifier.weight(1f)) {
            // Sử dụng AnnotatedString để in đậm tên người gửi
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(notification.actorName ?: "Ai đó")
                    }
                    append(" ${notification.message}")
                },
                fontSize = 14.sp,
                lineHeight = 18.sp // Tăng khoảng cách dòng cho dễ đọc
            )
            // Có thể thêm thời gian ở đây nếu muốn
            // Text(text = formatTime(notification.createdAt), fontSize = 12.sp, color = Color.Gray)
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Ảnh của bài viết liên quan (nếu có)
        if (notification.postImageUrl != null) {
            AsyncImage(
                model = notification.postImageUrl,
                contentDescription = "Ảnh bài viết",
                placeholder = painterResource(id = R.drawable.group),
                error = painterResource(id = R.drawable.group),
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(6.dp)),
                contentScale = ContentScale.Crop
            )
        }
    }
}

// Composable cho thanh tiêu đề
@Composable
fun TopAppBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = { /* TODO: Mở menu */ }) {
            Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu")
        }
        Text(text = "Study-S", fontWeight = FontWeight.Bold, fontSize = 22.sp)
        IconButton(onClick = { /* TODO: Tới trang Profile */ }) {
            // Thay thế bằng avatar người dùng thật
            Image(
                painter = painterResource(id = R.drawable.avatar),
                contentDescription = "Avatar",
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewNotificationScreen() {
    Study_STheme {
        NotificationScreen()
    }
}
