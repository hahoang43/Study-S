package com.example.study_s.ui.screens.group

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.study_s.data.model.MessageModel
import com.example.study_s.data.model.User
import com.example.study_s.viewmodel.GroupChatViewModel
import com.example.study_s.viewmodel.GroupViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatGroupScreen(
    navController: NavHostController,
    groupId: String,
    groupViewModel: GroupViewModel = viewModel(),
    groupChatViewModel: GroupChatViewModel = viewModel()
) {
    val currentUserId = remember { FirebaseAuth.getInstance().currentUser?.uid }
    val context = LocalContext.current

    val group by groupViewModel.group.collectAsState()
    val messages by groupChatViewModel.messages.collectAsState()

    // Lấy thông tin nhóm và tin nhắn khi màn hình được mở
    LaunchedEffect(groupId) {
        if (currentUserId != null) {
            groupViewModel.getGroupById(groupId)
            groupChatViewModel.listenToGroupMessages(groupId)
        }
    }

    val isMember = group?.members?.contains(currentUserId) == true
    val isAdmin = group?.createdBy == currentUserId

    Scaffold(
        topBar = {
            // TopBar luôn hiển thị thông tin nhóm
            GroupChatTopBar(
                group = group,
                navController = navController,
                isAdmin = isAdmin,
                isMember = isMember, // Truyền isMember vào
                groupViewModel = groupViewModel
            )
        },
        // Chỉ hiển thị BottomBar (ô nhập liệu) khi đã là thành viên
        bottomBar = {
            if (isMember) {
                MessageInput(onSendMessage = { message ->
                    if (currentUserId != null) {
                        groupChatViewModel.sendMessage(
                            groupId = groupId,
                            senderId = currentUserId,
                            content = message,
                            senderName = FirebaseAuth.getInstance().currentUser?.displayName ?: "Unknown"
                        )
                    }
                })
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // =========================================================================
            // ✅ PHẦN LOGIC HIỂN THỊ CHÍNH
            // =========================================================================
            if (isMember) {
                // NẾU ĐÃ LÀ THÀNH VIÊN: HIỂN THỊ GIAO DIỆN CHAT
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                    reverseLayout = true
                ) {
                    // Dùng items(messages) thay vì reversed() để hiệu năng tốt hơn
                    items(messages) { message ->
                        if (message.senderId == "system") {
                            SystemMessageItem(message = message)
                        } else if (currentUserId != null) {
                            MessageItem(message = message, currentUserId = currentUserId)
                        }
                    }
                }
            } else {
                // NẾU CHƯA LÀ THÀNH VIÊN: HIỂN THỊ LỚP PHỦ YÊU CẦU THAM GIA
                // Chỉ hiển thị khi đã tải xong thông tin nhóm để tránh màn hình trắng
                if (group != null) {
                    JoinGroupOverlay(
                        groupName = group!!.groupName,
                        onJoinClick = {
                            if (currentUserId != null) {
                                groupViewModel.joinGroupAndRefresh(groupId, currentUserId)
                                Toast.makeText(context, "Đang xử lý...", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                } else {
                    // Trong lúc chờ tải thông tin nhóm, hiển thị vòng xoay loading
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
            // =========================================================================
        }
    }
}

// =========================================================================
// ✅ COMPOSABLE MỚI: Lớp phủ yêu cầu tham gia
// =========================================================================
@Composable
fun JoinGroupOverlay(groupName: String, onJoinClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Lock Icon",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Bạn cần tham gia nhóm",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "\"$groupName\"",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "để có thể xem tin nhắn và trò chuyện cùng mọi người.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onJoinClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("THAM GIA NGAY", fontSize = 16.sp)
        }
    }
}

// =========================================================================
// ✅ SỬA LẠI TOPAPPBAR ĐỂ ẨN NÚT "..." KHI CHƯA PHẢI THÀNH VIÊN
// =========================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupChatTopBar(
    group: com.example.study_s.data.model.Group?,
    navController: NavHostController,
    isAdmin: Boolean,
    isMember: Boolean,
    groupViewModel: GroupViewModel
) {
    var showMenu by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMembersDialog by remember { mutableStateOf(false) }
    var showBannedMembersDialog by remember { mutableStateOf(false) }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    // Hiển thị các dialog (giữ nguyên logic cũ của bạn)
    // ...

    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Group,
                    contentDescription = "Group Icon",
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = group?.groupName ?: "Đang tải...", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    if (group != null) {
                        Text(text = "${group.members.size} thành viên", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            // Chỉ hiển thị nút "..." khi người dùng đã là thành viên
            if (isMember) {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                    }
                    // DropdownMenu và logic bên trong giữ nguyên như code cũ của bạn
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        // ... (Thêm lại các DropdownMenuItem từ code cũ của bạn vào đây)
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
    )
}


// ----- CÁC COMPOSABLE KHÁC GIỮ NGUYÊN -----
// Dán lại các Composable không thay đổi từ file cũ của bạn vào đây để tránh lỗi
// BannedMembersDialog, BannedMemberItem, MembersDialog, MemberItem,
// MessageItem, SystemMessageItem, MessageInput

@Composable
fun MessageItem(message: MessageModel, currentUserId: String) {
    val isCurrentUser = message.senderId == currentUserId
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier.widthIn(max = 300.dp).background(
                color = if (isCurrentUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp)
            ).padding(12.dp)
        ) {
            Column {
                if (!isCurrentUser) {
                    Text(text = message.senderName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Text(text = message.content, fontSize = 16.sp, color = if (isCurrentUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
fun SystemMessageItem(message: MessageModel) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = message.content,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun MessageInput(onSendMessage: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    Row(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { /* TODO: Handle attachment click */ }) {
            Icon(Icons.Default.Attachment, contentDescription = "Attach File", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.weight(1f),
            placeholder = { Text("Nhắn tin...") },
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
            modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.primary, CircleShape)
        ) {
            Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Send Message", tint = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

