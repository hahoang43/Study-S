package com.example.study_s.ui.screens.group

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.study_s.data.model.MessageModel
import com.example.study_s.viewmodel.ChatViewModel
import com.example.study_s.viewmodel.GroupViewModel
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatGroupScreen(
    navController: NavHostController,
    groupId: String,
    groupViewModel: GroupViewModel = viewModel(),
    chatViewModel: ChatViewModel = viewModel()
) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

    val group by groupViewModel.group.collectAsState()
    val messages by chatViewModel.messages.collectAsState()

    var showMenu by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(groupId) {
        groupViewModel.getGroupById(groupId)
        chatViewModel.listenToGroupMessages(groupId)
    }

    val isAdmin = group?.createdBy == currentUserId

    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text("Xác nhận rời nhóm") },
            text = { Text("Bạn có chắc chắn muốn rời khỏi nhóm '${group?.groupName}'? Bạn sẽ không thể xem lại các tin nhắn.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        groupViewModel.leaveGroup(groupId, currentUserId)
                        showLeaveDialog = false
                        navController.popBackStack()
                    }
                ) {
                    Text("Rời nhóm", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Xác nhận xóa nhóm") },
            text = { Text("Bạn có chắc chắn muốn xóa vĩnh viễn nhóm '${group?.groupName}'? Mọi dữ liệu sẽ bị mất.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        groupViewModel.deleteGroup(groupId)
                        showDeleteDialog = false
                        navController.popBackStack()
                    }
                ) {
                    Text("Xóa", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = "Group Icon",
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.LightGray).padding(8.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = group?.groupName ?: "Loading...", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.Black)
                            Text(text = "${group?.members?.size ?: 0} thành viên", fontSize = 14.sp, color = Color.Gray)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            if (isAdmin) {
                                DropdownMenuItem(
                                    text = { Text("Xóa nhóm", color = Color.Red) },
                                    onClick = {
                                        showDeleteDialog = true
                                        showMenu = false
                                    }
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text("Rời nhóm", color = Color.Red) },
                                    onClick = {
                                        showLeaveDialog = true
                                        showMenu = false
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            MessageInput(onSendMessage = { message ->
                chatViewModel.sendMessage(
                    groupId = groupId,
                    senderId = currentUserId,
                    content = message,
                    senderName = FirebaseAuth.getInstance().currentUser?.displayName ?: "Unknown"
                )
            })
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).background(Color(0xFFF5F5F5)).padding(horizontal = 8.dp),
            reverseLayout = true,
            verticalArrangement = Arrangement.Bottom
        ) {
            items(messages.reversed()) { message ->
                MessageItem(message = message, currentUserId = currentUserId)
            }
        }
    }
}

@Composable
fun MessageItem(message: MessageModel, currentUserId: String) {
    val isCurrentUser = message.senderId == currentUserId
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier.widthIn(max = 300.dp).background(
                color = if (isCurrentUser) Color(0xFF1A73E8) else Color.White,
                shape = RoundedCornerShape(16.dp)
            ).padding(12.dp)
        ) {
            Column {
                if (!isCurrentUser) {
                    Text(text = message.senderName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Text(text = message.content, fontSize = 16.sp, color = if (isCurrentUser) Color.White else Color.Black)
            }
        }
    }
}

@Composable
fun MessageInput(onSendMessage: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    Row(
        modifier = Modifier.fillMaxWidth().background(Color.White).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { /* TODO: Handle attachment click */ }) {
            Icon(Icons.Default.Attachment, contentDescription = "Attach File")
        }
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.weight(1f),
            placeholder = { Text("Type a message...") },
            shape = RoundedCornerShape(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
            onClick = {
                if (text.isNotBlank()) {
                    onSendMessage(text)
                    text = ""
                }
            },
            modifier = Modifier.size(48.dp).background(Color(0xFF1A73E8), CircleShape)
        ) {
            Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Send Message", tint = Color.White)
        }
    }
}
