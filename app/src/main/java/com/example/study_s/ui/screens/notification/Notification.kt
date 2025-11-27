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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.rememberAsyncImagePainter
import com.example.study_s.data.model.Notification
import com.example.study_s.R // ✅ ĐẢM BẢO BẠN ĐÃ IMPORT DÒNG NÀY
import com.example.study_s.ui.screens.components.BottomNavBar
import com.example.study_s.ui.screens.components.TopBar
import com.example.study_s.viewmodel.NotificationViewModel
import com.example.study_s.viewmodel.MainViewModel
// Composable chính của màn hình
@Composable
fun NotificationScreen(
    navController: NavController,
    viewModel: NotificationViewModel = viewModel(),
    mainViewModel: MainViewModel
) {
    val notifications by viewModel.notifications.collectAsState()
    // Lấy route hiện tại để truyền vào BottomNavBar

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val unreadCount by viewModel.unreadNotificationCount.collectAsState()
    // ✅ SỬ DỤNG SCAFFOLD ĐỂ CHỨA TOPBAR, BOTTOMBAR VÀ NỘI DUNG
    Scaffold(
        topBar = {
            TopBar(
                onChatClick = { /*...*/ },
                onSearchClick = { /*...*/ },
                // Chuyển hướng đến chính màn hình này khi nhấn chuông
                onNotificationClick = { navController.navigate("notification") },
                // TRUYỀN SỐ LƯỢNG VÀO TOPBAR
                notificationCount = unreadCount
            )
        },
        bottomBar = {
            // Gọi BottomNavBar bạn đã cung cấp
            BottomNavBar(
                navController = navController,
                currentRoute = currentRoute
            )
        }
    ) { innerPadding -> // `innerPadding` là khoảng trống an toàn do Scaffold cung cấp
        // LazyColumn chứa danh sách thông báo sẽ nằm ở đây
        LazyColumn(
            // Áp dụng `innerPadding` để nội dung không bị TopBar và BottomBar che mất
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Thêm tiêu đề "Hoạt động" vào đầu danh sách
            item {
                Text(
                    text = "Hoạt động",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                )
            }

            // Hiển thị danh sách thông báo hoặc thông báo trống
            if (notifications.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Chưa có hoạt động nào")
                    }
                }
            } else {
                items(notifications, key = { it.notificationId }) { notification ->
                    NotificationItem( // Dùng lại NotificationItem bạn đã có
                        notification = notification,
                        onItemClick = {
                            viewModel.onNotificationClicked(notification, navController)
                        }
                    )
                    // Thêm đường kẻ ngang giữa các mục
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
            }
        }
    }
}

// Composable cho mỗi mục thông báo (giữ nguyên như file bạn đã gửi)
@Composable
fun NotificationItem(
    notification: Notification,
    onItemClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!notification.isRead) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
            Spacer(Modifier.width(8.dp))
        } else {
            Spacer(Modifier.width(16.dp))
        }

        Image(
            painter = rememberAsyncImagePainter(
                model = notification.actorAvatarUrl,
                placeholder = painterResource(id = R.drawable.ic_profile),
                error = painterResource(id = R.drawable.ic_profile)
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
                when (notification.type) {
                    "schedule_reminder" -> {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, fontSize = 15.sp)) {
                            append("🔔 Lời nhắc từ Study_S")
                        }
                        withStyle(style = SpanStyle(fontSize = 15.sp)) {
                            append("\n${notification.message}")
                        }
                    }
                    "like", "comment", "follow" -> {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, fontSize = 15.sp)) {
                            append(notification.actorName ?: "Ai đó")
                        }
                        withStyle(style = SpanStyle(fontSize = 15.sp)) {
                            append(" ")
                            append(notification.message)
                        }
                    }
                    else -> {
                        append(notification.message)
                    }
                }
            },
            modifier = Modifier.weight(1f),
            lineHeight = 20.sp
        )

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
