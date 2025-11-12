package com.example.study_s.ui.screens.group

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
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
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val context = LocalContext.current

    val group by groupViewModel.group.collectAsState()
    val messages by groupChatViewModel.messages.collectAsState()
    val members by groupViewModel.members.collectAsState()
    val bannedMembers by groupViewModel.bannedMembers.collectAsState()
    val showRemovedToast by groupViewModel.showRemovedToast.collectAsState()

    var showMenu by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMembersDialog by remember { mutableStateOf(false) }
    var showBannedMembersDialog by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = groupChatViewModel) {
        groupChatViewModel.userRemoved.collectLatest {
            Toast.makeText(context, "Bạn đã bị xóa khỏi nhóm '${group?.groupName}'.", Toast.LENGTH_LONG).show()
            navController.popBackStack()
        }
    }

    LaunchedEffect(groupId) {
        groupViewModel.getGroupById(groupId)
        groupChatViewModel.listenToGroupMessages(groupId)
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
                    Text("Rời nhóm", color = MaterialTheme.colorScheme.error)
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
                    Text("Xóa", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    if (showMembersDialog) {
        group?.let {
            MembersDialog(
                group = it,
                members = members,
                isAdmin = isAdmin,
                onDismiss = { showMembersDialog = false },
                onRemoveMember = { memberId, memberName ->
                    val adminName = FirebaseAuth.getInstance().currentUser?.displayName ?: "Admin"
                    groupViewModel.removeMember(groupId, memberId, adminName, memberName)
                },
                onBanMember = { memberId ->
                    groupViewModel.banUser(groupId, memberId)
                }
            )
        }
    }

    if (showBannedMembersDialog) {
        group?.let {
            BannedMembersDialog(
                bannedMembers = bannedMembers,
                onDismiss = { showBannedMembersDialog = false },
                onUnbanMember = { memberId ->
                    groupViewModel.unbanUser(groupId, memberId)
                }
            )
        }
    }

    Scaffold(
        topBar = {
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
                            Text(text = group?.groupName ?: "Loading...", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Text(text = "${group?.members?.size ?: 0} thành viên", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            DropdownMenuItem(
                                text = { Text("Xem thành viên") },
                                onClick = {
                                    group?.members?.let { groupViewModel.loadMemberDetails(it) }
                                    showMembersDialog = true
                                    showMenu = false
                                }
                            )
                            if (isAdmin) {
                                DropdownMenuItem(
                                    text = { Text("Danh sách chặn") },
                                    onClick = {
                                        group?.bannedUsers?.let { groupViewModel.loadBannedMemberDetails(it) }
                                        showBannedMembersDialog = true
                                        showMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Xóa nhóm", color = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showDeleteDialog = true
                                        showMenu = false
                                    }
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text("Rời nhóm", color = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showLeaveDialog = true
                                        showMenu = false
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            MessageInput(onSendMessage = { message ->
                groupChatViewModel.sendMessage(
                    groupId = groupId,
                    senderId = currentUserId,
                    content = message,
                    senderName = FirebaseAuth.getInstance().currentUser?.displayName ?: "Unknown"
                )
            })
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).background(MaterialTheme.colorScheme.background).padding(horizontal = 8.dp),
            reverseLayout = true,
            verticalArrangement = Arrangement.Bottom
        ) {
            items(messages.reversed()) { message ->
                if (message.senderId == "system") {
                    SystemMessageItem(message = message)
                } else {
                    MessageItem(message = message, currentUserId = currentUserId)
                }
            }
        }
    }
}

@Composable
fun BannedMembersDialog(
    bannedMembers: List<User>,
    onDismiss: () -> Unit,
    onUnbanMember: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thành viên bị chặn (${bannedMembers.size})") },
        text = {
            LazyColumn {
                items(bannedMembers) { member ->
                    BannedMemberItem(
                        member = member,
                        onUnbanClick = { onUnbanMember(member.userId) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Đóng")
            }
        }
    )
}

@Composable
fun BannedMemberItem(
    member: User,
    onUnbanClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = "Avatar",
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = member.name, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
        TextButton(onClick = onUnbanClick) {
            Text("Gỡ chặn")
        }
    }
}

@Composable
fun MembersDialog(
    group: com.example.study_s.data.model.Group,
    members: List<User>,
    isAdmin: Boolean,
    onDismiss: () -> Unit,
    onRemoveMember: (String, String) -> Unit,
    onBanMember: (String) -> Unit
) {
    var memberToRemove by remember { mutableStateOf<User?>(null) }
    var memberToBan by remember { mutableStateOf<User?>(null) }

    memberToRemove?.let { user ->
        AlertDialog(
            onDismissRequest = { memberToRemove = null },
            title = { Text("Xác nhận xóa thành viên") },
            text = { Text("Bạn có chắc chắn muốn xóa ${user.name} khỏi nhóm?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRemoveMember(user.userId, user.name)
                        memberToRemove = null
                    }
                ) {
                    Text("Xóa", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { memberToRemove = null }) {
                    Text("Hủy")
                }
            }
        )
    }

    memberToBan?.let { user ->
        AlertDialog(
            onDismissRequest = { memberToBan = null },
            title = { Text("Xác nhận cấm thành viên") },
            text = { Text("Bạn có chắc chắn muốn cấm ${user.name} vĩnh viễn khỏi nhóm? Người này sẽ không thể tham gia lại.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onBanMember(user.userId)
                        memberToBan = null
                    }
                ) {
                    Text("Cấm", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { memberToBan = null }) {
                    Text("Hủy")
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thành viên nhóm (${members.size})") },
        text = {
            LazyColumn {
                items(members) { member ->
                    MemberItem(
                        member = member,
                        isCreator = member.userId == group.createdBy,
                        isAdmin = isAdmin,
                        onRemoveClick = { memberToRemove = member },
                        onBanClick = { memberToBan = member }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Đóng")
            }
        }
    )
}

@Composable
fun MemberItem(
    member: User,
    isCreator: Boolean,
    isAdmin: Boolean,
    onRemoveClick: () -> Unit,
    onBanClick: () -> Unit
) {
    var showMemberMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = "Avatar",
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = member.name, fontWeight = FontWeight.Bold)
            if (isCreator) {
                Text(text = "Quản trị viên", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            }
        }
        if (isAdmin && !isCreator) {
            Box {
                IconButton(onClick = { showMemberMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Tùy chọn thành viên")
                }
                DropdownMenu(expanded = showMemberMenu, onDismissRequest = { showMemberMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Xóa khỏi nhóm") },
                        onClick = {
                            onRemoveClick()
                            showMemberMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.PersonRemove,
                                contentDescription = "Xóa khỏi nhóm"
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Cấm thành viên", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            onBanClick()
                            showMemberMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = "Cấm thành viên",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    )
                }
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
            modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.primary, CircleShape)
        ) {
            Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Send Message", tint = MaterialTheme.colorScheme.onPrimary)
        }
    }
}
