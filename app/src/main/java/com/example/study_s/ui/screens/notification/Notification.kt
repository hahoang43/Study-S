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
import com.example.study_s.R
import com.example.study_s.data.model.Notification
import com.example.study_s.ui.screens.components.BottomNavBar
import com.example.study_s.ui.screens.components.TopBar
import com.example.study_s.viewmodel.MainViewModel
import com.example.study_s.viewmodel.NotificationViewModel
import androidx.compose.runtime.mutableStateOf   // 👈 THÊM
import androidx.compose.runtime.remember      // 👈 THÊM
import androidx.compose.runtime.setValue
@Composable
fun NotificationScreen(
    navController: NavController,
    viewModel: NotificationViewModel = viewModel(),
    mainViewModel: MainViewModel
) {
    val notifications by viewModel.notifications.collectAsState()
    val unreadCount by viewModel.unreadNotificationCount.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        topBar = {
            TopBar(
                onChatClick = { /* ... */ },
                onSearchClick = { /* ... */ },
                onNotificationClick = { navController.navigate("notification") },
                notificationCount = unreadCount
            )
        },
        bottomBar = {
            BottomNavBar(
                navController = navController,
                currentRoute = currentRoute
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            item {
                Text(
                    text = "Hoạt động",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                )
            }

            if (notifications.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillParentMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Chưa có hoạt động nào")
                    }
                }
            } else {
                items(
                    items = notifications,
                    key = { it.notificationId }
                ) { notification ->
                    NotificationItem(
                        notification = notification,
                        onItemClick = {
                            viewModel.onNotificationClicked(notification, navController)
                        }
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}


@Composable
fun NotificationItem(
    notification: Notification,
    onItemClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick() }
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
                placeholder = painterResource(id = R.drawable.logo_study),
                error = painterResource(id = R.drawable.logo_study)
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
                    // GOM 2 LOẠI THÔNG BÁO HỆ THỐNG VÀO CHUNG MỘT CHỖ
                    "schedule_reminder", "SYSTEM_ADMIN" -> {
                        withStyle(
                            style = SpanStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        ) {
                            // ✅ DÙNG TITLE TỪ NOTIFICATION, NẾU KHÔNG CÓ THÌ MỚI DÙNG MẶC ĐỊNH
                            append("🔔 ${notification.title ?: "Thông báo từ Study_S"}")
                        }
                        withStyle(style = SpanStyle(fontSize = 15.sp)) {
                            // ✅ DÙNG BODY TỪ NOTIFICATION
                            append("\n${notification.body ?: notification.message}")
                        }
                    }

                    "like", "comment", "follow" -> {
                        withStyle(
                            style = SpanStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        ) {
                            append(notification.actorName ?: "Ai đó")
                        }
                        withStyle(style = SpanStyle(fontSize = 15.sp)) {
                            append(" ")
                            append(notification.message)
                        }
                    }

                    else -> append(notification.message)
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
