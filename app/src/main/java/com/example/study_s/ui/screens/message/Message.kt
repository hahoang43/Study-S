package com.example.study_s.ui.screens.message

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    var inSelectionMode by remember { mutableStateOf(false) }
    var selectedChatIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    fun exitSelectionMode() {
        inSelectionMode = false
        selectedChatIds = emptySet()
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Xóa cuộc trò chuyện?") },
            text = { Text("Bạn có chắc chắn muốn xóa ${selectedChatIds.size} cuộc trò chuyện đã chọn không? Hành động này không thể hoàn tác.") },
            confirmButton = {
                TextButton(onClick = {
                    chatListViewModel.deleteChats(selectedChatIds)
                    showDeleteConfirmation = false
                    exitSelectionMode()
                }) {
                    Text("Xóa")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            if (inSelectionMode) {
                TopAppBar(
                    title = { Text("${selectedChatIds.size} đã chọn") },
                    navigationIcon = {
                        IconButton(onClick = { exitSelectionMode() }) {
                            Icon(Icons.Default.Close, contentDescription = "Đóng")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showDeleteConfirmation = true },
                            enabled = selectedChatIds.isNotEmpty()
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Xóa")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            } else {
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
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            items(chatsWithUsers, key = { it.first.id }) { (chat, user) ->
                MessageItem(
                    chat = chat,
                    user = user,
                    navController = navController,
                    chatListViewModel = chatListViewModel,
                    isSelected = chat.id in selectedChatIds,
                    inSelectionMode = inSelectionMode,
                    onToggleSelection = {
                        selectedChatIds = if (chat.id in selectedChatIds) {
                            selectedChatIds - chat.id
                        } else {
                            selectedChatIds + chat.id
                        }.also {
                            if (it.isEmpty()) {
                                inSelectionMode = false
                            }
                        }
                    },
                    onStartSelection = {
                        if (!inSelectionMode) {
                            inSelectionMode = true
                            selectedChatIds = setOf(chat.id)
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageItem(
    chat: ChatModel,
    user: UserModel?,
    navController: NavController,
    chatListViewModel: ChatListViewModel,
    isSelected: Boolean,
    inSelectionMode: Boolean,
    onToggleSelection: () -> Unit,
    onStartSelection: () -> Unit
) {
    val currentUserId = Firebase.auth.currentUser?.uid
    val isUnread = currentUserId != null &&
            chat.lastMessage != null &&
            chat.lastMessage.senderId != currentUserId &&
            (chat.lastMessage.readBy[currentUserId] == false || chat.lastMessage.readBy[currentUserId] == null)
    // ✅ BƯỚC 1: XÁC ĐỊNH NGƯỜI GỬI LÀ BẠN HAY NGƯỜI KHÁC
    val isSentByMe = chat.lastMessage?.senderId == currentUserId

    // ✅ BƯỚC 2: TẠO NỘI DUNG HIỂN THỊ ĐỘNG
    val lastMessageContent = when (chat.lastMessage?.type) {
        "image" -> if (isSentByMe) "Bạn đã gửi một ảnh" else "${user?.name ?: "Họ"} đã gửi một ảnh"
        "file" -> if (isSentByMe) "Bạn đã gửi một tệp tin" else "${user?.name ?: "Họ"} đã gửi một tệp tin"
        else -> chat.lastMessage?.content ?: "" // Mặc định hiển thị nội dung text
    }

    val fontWeight = if (isUnread && !isSelected) FontWeight.Bold else FontWeight.Normal
    val textColor = if (isUnread && !isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
    val dynamicBackgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        // Giữ lại màu nền cho tin nhắn chưa đọc, nhưng bạn có thể thay đổi nếu muốn
        isUnread -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) 
        else -> MaterialTheme.colorScheme.surface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (inSelectionMode) {
                        onToggleSelection()
                    } else {
                        if (user != null) {
                            chatListViewModel.markChatAsRead(chat.id)
                            navController.navigate("chat/${user.userId}")
                        }
                    }
                },
                onLongClick = onStartSelection
            )
            .background(dynamicBackgroundColor)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            AsyncImage(
                model = user?.avatarUrl ?: R.drawable.ic_profile,
                contentDescription = "Avatar",
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user?.name ?: "User",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = lastMessageContent,
                color = textColor,
                fontWeight = fontWeight,
                fontSize = 14.sp,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.Center) {
            chat.lastMessage?.timestamp?.let {
                Text(
                    text = formatTimestamp(it),
                    color = textColor,
                    fontSize = 13.sp,
                    fontWeight = fontWeight
                )
            }
            if (isUnread && !isSelected) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                )
            }
        }
    }
}
