package com.example.study_s.ui.screens.message

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.study_s.R
import com.example.study_s.data.model.ChatModel
import com.example.study_s.data.model.UserModel
import com.example.study_s.util.formatTimestamp
import com.example.study_s.viewmodel.ChatListViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageListScreen(navController: NavController, chatListViewModel: ChatListViewModel = viewModel()) {
    val chatsWithUsers by chatListViewModel.chats.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tin nhắn", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            items(chatsWithUsers) { (chat, user) ->
                MessageItem(
                    chat = chat,
                    user = user,
                    navController = navController,
                    chatListViewModel = chatListViewModel // Truyền ViewModel xuống
                )
            }
        }
    }
}

@Composable
fun MessageItem(
    chat: ChatModel,
    user: UserModel?,
    navController: NavController,
    chatListViewModel: ChatListViewModel // Nhận ViewModel để xử lý logic
) {
    val currentUserId = Firebase.auth.currentUser?.uid

    // 1. KIỂM TRA TRẠNG THÁI CHƯA ĐỌC
    val isUnread = currentUserId != null && // 1. Đảm bảo user đã đăng nhập
            chat.lastMessage != null &&
            chat.lastMessage.senderId != currentUserId &&
            // 2. Chỉ kiểm tra readBy khi user ID không null
            (chat.lastMessage.readBy[currentUserId] == false || chat.lastMessage.readBy[currentUserId] == null)

    // 2. CHỌN CÁC THUỘC TÍNH UI DỰA TRÊN TRẠNG THÁI `isUnread`
    val fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal
    val textColor = if (isUnread) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
    val backgroundColor = if (isUnread) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
    } else {
        Color.Transparent
    }

    val clickableModifier = if (user != null) {
        Modifier.clickable {
            // Khi click vào, đánh dấu cuộc trò chuyện đã đọc và điều hướng
            chatListViewModel.markChatAsRead(chat.id)
            navController.navigate("chat/${user.userId}")
        }
    } else {
        Modifier
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(clickableModifier)
            .background(backgroundColor) // Áp dụng màu nền
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box {
            AsyncImage(
                model = user?.avatarUrl ?: R.drawable.ic_profile,
                contentDescription = "Avatar",
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Cột chứa Tên và Tin nhắn
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user?.name ?: "User",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = chat.lastMessage?.content ?: "",
                color = textColor,
                fontWeight = fontWeight, // In đậm nếu chưa đọc
                fontSize = 14.sp,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Cột chứa Thời gian và Chấm báo hiệu
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.Center) {
            chat.lastMessage?.timestamp?.let {
                Text(
                    text = formatTimestamp(it),
                    color = textColor,
                    fontSize = 13.sp,
                    fontWeight = fontWeight // In đậm nếu chưa đọc
                )
            }
            if (isUnread) {
                Spacer(modifier = Modifier.height(4.dp))
                // Chấm tròn báo hiệu tin nhắn mới
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                )
            }
        }
    }
}
