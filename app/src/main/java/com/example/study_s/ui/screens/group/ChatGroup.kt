package com.example.study_s.ui.screens.group

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.study_s.data.model.Group
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
    val members by groupViewModel.members.collectAsState()
    val bannedMembers by groupViewModel.bannedMembers.collectAsState()

    // Listen for user removal events
    LaunchedEffect(key1 = groupChatViewModel) {
        groupChatViewModel.userRemoved.collectLatest { removedUserId ->
            if (removedUserId == currentUserId) {
                Toast.makeText(context, "Bạn đã bị xóa khỏi nhóm.", Toast.LENGTH_LONG).show()
                navController.popBackStack()
            }
        }
    }

    // Fetch group and message details when the screen is opened
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
            GroupChatTopBar(
                group = group,
                navController = navController,
                isAdmin = isAdmin,
                isMember = isMember,
                groupViewModel = groupViewModel,
                members = members,
                bannedMembers = bannedMembers
            )
        },
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
            if (group?.bannedUsers?.contains(currentUserId) == true) {
                BannedFromGroupOverlay()
            } else if (isMember) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                    reverseLayout = true
                ) {
                    items(messages) { message ->
                        if (message.senderId == "system") {
                            SystemMessageItem(message = message)
                        } else if (currentUserId != null) {
                            MessageItem(message = message, currentUserId = currentUserId)
                        }
                    }
                }
            } else {
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
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

@Composable
fun BannedFromGroupOverlay() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.Block,
            contentDescription = "Banned Icon",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Bạn đã bị cấm",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "khỏi nhóm này và không thể tham gia.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}

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
            imageVector = Icons.Filled.Lock,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupChatTopBar(
    group: Group?,
    navController: NavHostController,
    isAdmin: Boolean,
    isMember: Boolean,
    groupViewModel: GroupViewModel,
    members: List<User>,
    bannedMembers: List<User>
) {
    var showMenu by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMembersDialog by remember { mutableStateOf(false) }
    var showBannedMembersDialog by remember { mutableStateOf(false) }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text("Xác nhận rời nhóm") },
            text = { Text("Bạn có chắc chắn muốn rời khỏi nhóm này không?") },
            confirmButton = {
                TextButton(onClick = {
                    group?.let { groupViewModel.leaveGroup(it.groupId, currentUserId) }
                    showLeaveDialog = false
                    navController.popBackStack()
                }) {
                    Text("Rời nhóm", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) { Text("Hủy") }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Xác nhận xóa nhóm") },
            text = { Text("Hành động này không thể hoàn tác. Tất cả tin nhắn sẽ bị xóa vĩnh viễn. Bạn có chắc chắn muốn xóa nhóm này không?") },
            confirmButton = {
                TextButton(onClick = {
                    group?.let { groupViewModel.deleteGroup(it.groupId) }
                    showDeleteDialog = false
                    navController.popBackStack()
                }) {
                    Text("Xóa", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Hủy") }
            }
        )
    }

    if (showMembersDialog && group != null) {
        MembersDialog(
            group = group,
            members = members,
            isAdmin = isAdmin,
            currentUserId = currentUserId,
            onDismiss = { showMembersDialog = false },
            onRemoveMember = { memberId, memberName ->
                group.let {  groupViewModel.removeMember(it.groupId, memberId, it.groupName, memberName) }
            },
            onBanMember = { memberId ->
                groupViewModel.banUser(group.groupId, memberId)
            }
        )
    }

    if (showBannedMembersDialog && group != null) {
        BannedMembersDialog(
            bannedMembers = bannedMembers,
            onDismiss = { showBannedMembersDialog = false },
            onUnbanMember = { memberId ->
                groupViewModel.unbanUser(group.groupId, memberId)
            }
        )
    }

    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Group,
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
            if (isMember) {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More Options")
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
                            },
                            leadingIcon = { Icon(Icons.Filled.People, contentDescription = null) }
                        )

                        if (isAdmin) {
                            DropdownMenuItem(
                                text = { Text("Danh sách chặn") },
                                onClick = {
                                    group?.bannedUsers?.let { groupViewModel.loadBannedMemberDetails(it) }
                                    showBannedMembersDialog = true
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Filled.Block, contentDescription = null) }
                            )
                            Divider()
                            DropdownMenuItem(
                                text = { Text("Xóa nhóm", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showDeleteDialog = true
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                            )
                        } else {
                            Divider()
                            DropdownMenuItem(
                                text = { Text("Rời nhóm", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showLeaveDialog = true
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                            )
                        }
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
    )
}

@Composable
fun BannedMembersDialog(bannedMembers: List<User>, onDismiss: () -> Unit, onUnbanMember: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thành viên bị chặn (${bannedMembers.size})") },
        text = {
            if (bannedMembers.isEmpty()) {
                Text("Không có thành viên nào bị chặn.", modifier = Modifier.padding(16.dp), textAlign = TextAlign.Center)
            } else {
                LazyColumn {
                    items(bannedMembers) { member ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(member.name ?: "Unknown User")
                            TextButton(onClick = { onUnbanMember(member.userId) }) {
                                Text("Bỏ chặn")
                            }
                        }
                    }
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
fun MessageInput(onSendMessage: (String) -> Unit) {
    var text by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.weight(1f),
            placeholder = { Text("Nhập tin nhắn...") },
            shape = RoundedCornerShape(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(onClick = { onSendMessage(text); text = "" }) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
        }
    }
}

@Composable
fun MessageItem(message: MessageModel, currentUserId: String) {
    val isCurrentUser = message.senderId == currentUserId

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .background(
                    color = if (isCurrentUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(8.dp)
        ) {
            Text(text = message.senderName, fontWeight = FontWeight.Bold)
            Text(text = message.content)
        }
    }
}

@Composable
fun SystemMessageItem(message: MessageModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = message.content,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun MembersDialog(
    group: Group,
    members: List<User>,
    isAdmin: Boolean,
    currentUserId: String,
    onDismiss: () -> Unit,
    onRemoveMember: (String, String) -> Unit,
    onBanMember: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thành viên (${members.size})") },
        text = {
            LazyColumn {
                items(members) { member ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // User info
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = member.name ?: "Unknown", fontWeight = FontWeight.Bold)
                            if (group.createdBy == member.userId) {
                                Text(text = "Admin", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }

                        // Admin actions
                        if (isAdmin && member.userId != currentUserId) {
                            Row {
                                IconButton(onClick = { onRemoveMember(member.userId, member.name ?: "") }) {
                                    Icon(Icons.Filled.PersonRemove, contentDescription = "Remove member")
                                }
                                IconButton(onClick = { onBanMember(member.userId) }) {
                                    Icon(Icons.Filled.Block, contentDescription = "Ban member")
                                }
                            }
                        }
                    }
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
