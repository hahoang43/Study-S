// ĐƯỜNG DẪN: ui/screens/notification/NotificationScreen.kt
// NỘI DUNG HOÀN CHỈNH - PHIÊN BẢN CUỐI CÙNG

package com.example.study_s.ui.screens.notification

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.study_s.data.model.Notification
import com.example.study_s.viewmodel.NotificationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    navController: NavController, // ✅ 1. NHẬN NavController
    viewModel: NotificationViewModel = viewModel()
) {
    val notifications by viewModel.notifications.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Hoạt động", fontWeight = FontWeight.Bold) })
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            if (notifications.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Chưa có hoạt động nào")
                    }
                }
            } else {
                items(notifications, key = { it.notificationId }) { notification ->
                    NotificationItem(
                        notification = notification,
                        // ✅ 2. TRUYỀN HÀNH ĐỘNG CLICK VÀO ITEM
                        onItemClick = {
                            viewModel.onNotificationClicked(notification, navController)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationItem(
    notification: Notification,
    onItemClick: () -> Unit // ✅ 3. NHẬN HÀNH ĐỘNG CLICK
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClick) // Áp dụng hành động click
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ✅ 4. HIỂN THỊ CHẤM ĐỎ DỰA TRÊN `isRead`
        if (!notification.isRead) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(Color.Red, CircleShape)
            )
            Spacer(Modifier.width(8.dp))
        } else {
            // Thêm spacer để các item đã đọc và chưa đọc thẳng hàng
            Spacer(Modifier.width(16.dp))
        }

        Image(
            painter = rememberAsyncImagePainter(
                model = notification.actorAvatarUrl ?: "https://i.pravatar.cc/150",
                placeholder = rememberAsyncImagePainter("https://i.pravatar.cc/150")
            ),
            contentDescription = "Avatar",
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(Modifier.width(12.dp))

        Text(
            text = buildAnnotatedString {
                // ✅ THÊM LOGIC "WHEN"
                when (notification.type) {
                    "schedule_reminder" -> {
                        // Hiển thị đặc biệt cho lời nhắc lịch học
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, fontSize = 15.sp)) {
                            append("🔔 Lời nhắc từ Study_S") // Hoặc notification.actorName
                        }
                        withStyle(style = SpanStyle(fontSize = 15.sp)) {
                            append("\n${notification.message}") // Thêm xuống dòng cho rõ ràng
                        }
                    }
                    "like", "comment", "follow" -> {
                        // Logic cũ của bạn cho các thông báo từ người dùng
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, fontSize = 15.sp)) {
                            append(notification.actorName ?: "Ai đó")
                        }
                        withStyle(style = SpanStyle(fontSize = 15.sp)) {
                            append(" ")
                            append(notification.message)
                        }
                    }
                    else -> {
                        // Hiển thị mặc định nếu có loại thông báo lạ
                        append(notification.message)
                    }
                }
            },
            modifier = Modifier.weight(1f),
            lineHeight = 20.sp // Giúp văn bản có 2 dòng hiển thị đẹp hơn
        )

        // Thumbnail ảnh bài viết (nếu có)
        notification.postImageUrl?.let {
            Spacer(Modifier.width(12.dp))
            Image(
                painter = rememberAsyncImagePainter(model = it),
                contentDescription = "Post thumbnail",
                modifier = Modifier
                    .size(48.dp)
                    .clip(MaterialTheme.shapes.small),
                contentScale = ContentScale.Crop
            )
        }
    }
}
