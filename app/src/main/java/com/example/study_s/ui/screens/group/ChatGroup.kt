package com.example.study_s.ui.screens.group

import android.Manifest
import android.app.DownloadManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.study_s.data.model.Group
import com.example.study_s.data.model.MessageModel
import com.example.study_s.data.model.User
import com.example.study_s.ui.navigation.Routes
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
    val pendingMembers by groupViewModel.pendingMembers.collectAsState()
    val isUploading by groupChatViewModel.isUploading.collectAsState()

    val listState = rememberLazyListState()

    LaunchedEffect(key1 = groupChatViewModel) {
        groupChatViewModel.userRemoved.collectLatest { removedUserId ->
            if (removedUserId == currentUserId) {
                Toast.makeText(context, "Bạn đã bị xóa khỏi nhóm.", Toast.LENGTH_LONG).show()
                navController.popBackStack()
            }
        }
        groupChatViewModel.uploadSuccess.collectLatest { success ->
            val message = if (success) "Tải lên thành công!" else "Tải lên thất bại."
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(messages) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    val refreshData = {
        if (currentUserId != null) {
            groupViewModel.getGroupById(groupId)
        }
    }

    LaunchedEffect(groupId) {
        refreshData()
        if (currentUserId != null) {
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
                bannedMembers = bannedMembers,
                pendingMembers = pendingMembers,
                refreshData = refreshData
            )
        },
        bottomBar = {
            if (isMember) {
                MessageInput(
                    onSendMessage = { message ->
                        if (currentUserId != null) {
                            groupChatViewModel.sendMessage(
                                groupId = groupId,
                                senderId = currentUserId,
                                content = message,
                                senderName = FirebaseAuth.getInstance().currentUser?.displayName ?: "Unknown"
                            )
                        }
                    },
                    groupChatViewModel = groupChatViewModel,
                    groupId = groupId
                )
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
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    reverseLayout = true,
                    state = listState
                ) {
                    items(messages) { message ->
                        if (message.senderId == "system") {
                            SystemMessageItem(message = message)
                        } else if (currentUserId != null) {
                            MessageItem(
                                message = message,
                                currentUserId = currentUserId,
                                navController = navController,
                                groupChatViewModel = groupChatViewModel,
                                groupId = groupId
                            )
                        }
                    }
                }
            } else {
                if (group != null) {
                    var requestSent by remember { mutableStateOf(false) }
                    val isPending = group?.pendingMembers?.contains(currentUserId) == true

                    JoinGroupOverlay(
                        groupName = group!!.groupName,
                        isPending = isPending || requestSent,
                        onJoinClick = {
                            if (currentUserId != null) {
                                groupViewModel.joinGroup(groupId, currentUserId)
                                requestSent = true
                                Toast.makeText(context, "Đã gửi yêu cầu tham gia.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
            if (isUploading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
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
fun JoinGroupOverlay(groupName: String, isPending: Boolean, onJoinClick: () -> Unit) {
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
            text = if (isPending) "Yêu cầu đã được gửi" else "Bạn cần tham gia nhóm",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = if (isPending) "Vui lòng chờ duyệt" else "\"$groupName\"",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = if (isPending) "Bạn sẽ được thông báo khi yêu cầu được chấp nhận." else "để có thể xem tin nhắn và trò chuyện cùng mọi người.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onJoinClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = !isPending
        ) {
            Text(if (isPending) "Đã gửi yêu cầu tham gia" else "Gửi yêu cầu tham gia", fontSize = 16.sp)
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
    bannedMembers: List<User>,
    pendingMembers: List<User>,
    refreshData: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMembersDialog by remember { mutableStateOf(false) }
    var showBannedMembersDialog by remember { mutableStateOf(false) }
    var showPendingRequestsDialog by remember { mutableStateOf(false) }
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
                refreshData()
            },
            onBanMember = { memberId ->
                groupViewModel.banUser(group.groupId, memberId)
                refreshData()
            },
            navController = navController
        )
    }

    if (showBannedMembersDialog && group != null) {
        BannedMembersDialog(
            bannedMembers = bannedMembers,
            onDismiss = { showBannedMembersDialog = false },
            onUnbanMember = { memberId ->
                groupViewModel.unbanUser(group.groupId, memberId)
                refreshData()
            }
        )
    }

    if (showPendingRequestsDialog && group != null) {
        PendingRequestsDialog(
            pendingMembers = pendingMembers,
            onDismiss = { showPendingRequestsDialog = false },
            onApprove = { userId, userName ->
                groupViewModel.approveJoinRequest(group.groupId, userId, group.groupName, userName)
                refreshData()
            },
            onReject = { userId ->
                groupViewModel.rejectJoinRequest(group.groupId, userId)
                refreshData()
            }
        )
    }

    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                                text = { Text("Yêu cầu tham gia") },
                                onClick = {
                                    group?.pendingMembers?.let { groupViewModel.loadPendingMemberDetails(it) }
                                    showPendingRequestsDialog = true
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Filled.GroupAdd, contentDescription = "Yêu cầu tham gia") }
                            )
                            DropdownMenuItem(
                                text = { Text("Danh sách chặn") },
                                onClick = {
                                    group?.bannedUsers?.let { groupViewModel.loadBannedMemberDetails(it) }
                                    showBannedMembersDialog = true
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Filled.Block, contentDescription = null) }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Xóa nhóm", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showDeleteDialog = true
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                            )
                        } else {
                            HorizontalDivider()
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
fun PendingRequestsDialog(
    pendingMembers: List<User>,
    onDismiss: () -> Unit,
    onApprove: (String, String) -> Unit,
    onReject: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Yêu cầu tham gia (${pendingMembers.size})") },
        text = {
            if (pendingMembers.isEmpty()) {
                Text("Không có yêu cầu nào.", modifier = Modifier.padding(16.dp), textAlign = TextAlign.Center)
            } else {
                LazyColumn {
                    items(pendingMembers) { member ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(member.name, modifier = Modifier.weight(1f))
                            Row {
                                TextButton(onClick = { onApprove(member.userId, member.name) }) {
                                    Text("Duyệt")
                                }
                                TextButton(onClick = { onReject(member.userId) }) {
                                    Text("Từ chối", color = MaterialTheme.colorScheme.error)
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(member.name)
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
fun MessageInput(
    onSendMessage: (String) -> Unit,
    groupChatViewModel: GroupChatViewModel,
    groupId: String
) {
    var text by remember { mutableStateOf("") }
    val context = LocalContext.current
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val currentUserDisplayName = FirebaseAuth.getInstance().currentUser?.displayName ?: "Unknown"

    var showPermissionRationale by remember { mutableStateOf(false) }
    var showAttachmentMenu by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            if (currentUserId != null) {
                groupChatViewModel.sendFile(context, groupId, currentUserId, currentUserDisplayName, it, "image")
            }
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            if (currentUserId != null) {
                groupChatViewModel.sendFile(context, groupId, currentUserId, currentUserDisplayName, it, "file")
            }
        }
    }

    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(context, "Quyền đã được cấp. Vui lòng thử lại.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Bạn đã từ chối quyền truy cập.", Toast.LENGTH_SHORT).show()
        }
    }

    if (showPermissionRationale) {
        AlertDialog(
            onDismissRequest = { showPermissionRationale = false },
            icon = { Icon(Icons.Default.FolderShared, contentDescription = null) },
            title = { Text("Yêu cầu quyền truy cập") },
            text = { Text("Để có thể chọn ảnh và tệp, Study-S cần quyền truy cập vào bộ nhớ của bạn. Bạn đồng ý chứ?") },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionRationale = false
                    requestPermissionLauncher.launch(permission)
                }) {
                    Text("Tiếp tục")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionRationale = false }) { Text("Hủy") }
            }
        )
    }

    fun launchPicker(type: String) {
        when (ContextCompat.checkSelfPermission(context, permission)) {
            PackageManager.PERMISSION_GRANTED -> {
                if (type == "image") {
                    imagePickerLauncher.launch("image/*")
                } else {
                    filePickerLauncher.launch("*/*")
                }
            }
            else -> {
                showPermissionRationale = true
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            IconButton(onClick = { showAttachmentMenu = true }) {
                Icon(
                    imageVector = Icons.Default.AttachFile,
                    contentDescription = "Attach File"
                )
            }
            DropdownMenu(
                expanded = showAttachmentMenu,
                onDismissRequest = { showAttachmentMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Tải ảnh lên") },
                    onClick = {
                        showAttachmentMenu = false
                        launchPicker("image")
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Upload Image"
                        )
                    }
                )
                DropdownMenuItem(
                    text = { Text("Tải tệp lên") },
                    onClick = {
                        showAttachmentMenu = false
                        launchPicker("file")
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                            contentDescription = "Upload File"
                        )
                    }
                )
            }
        }
        TextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.weight(1f),
            placeholder = { Text("Nhập tin nhắn...") },
            shape = RoundedCornerShape(24.dp)
        )
        IconButton(onClick = {
            if (text.isNotBlank()) {
                onSendMessage(text)
                text = ""
            }
        }) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send Message",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageItem(
    message: MessageModel,
    currentUserId: String,
    navController: NavHostController,
    groupChatViewModel: GroupChatViewModel,
    groupId: String
) {
    val isCurrentUser = message.senderId == currentUserId
    val arrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    if (showEditDialog) {
        EditMessageDialog(
            message = message,
            onDismiss = { showEditDialog = false },
            onConfirm = { newContent ->
                groupChatViewModel.editMessage(groupId, message.id, newContent)
                showEditDialog = false
            }
        )
    }

    Row(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp), horizontalArrangement = arrangement) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(
                    color = if (isCurrentUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(12.dp)
        ) {
            Column {
                if (!isCurrentUser) {
                    Text(
                        text = message.senderName,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontSize = 14.sp,
                        modifier = Modifier.clickable { navController.navigate("${Routes.Profile}?userId=${message.senderId}") }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                when (message.type) {
                    "text" -> {
                        Text(
                            text = message.content,
                            color = if (isCurrentUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                            fontSize = 16.sp,
                            modifier = Modifier.pointerInput(Unit) {
                                detectTapGestures(
                                    onLongPress = {
                                        if (isCurrentUser) {
                                            showMenu = true
                                        }
                                    }
                                )
                            }
                        )
                    }
                    "image" -> {
                        message.fileUrl?.let { url ->
                            AsyncImage(
                                model = url,
                                contentDescription = "Sent image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onLongPress = {
                                                if (isCurrentUser) {
                                                    showMenu = true
                                                }
                                            }
                                        )
                                    },
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                    "file" -> {
                        message.fileUrl?.let { url ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.combinedClickable(
                                    onClick = {
                                        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                        val request = DownloadManager.Request(url.toUri())
                                            .setTitle(message.content)
                                            .setDescription("Downloading")
                                            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, message.content)
                                        downloadManager.enqueue(request)
                                        Toast.makeText(context, "Bắt đầu tải xuống...", Toast.LENGTH_SHORT).show()
                                    },
                                    onLongClick = {
                                        showMenu = true
                                    }
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                                    contentDescription = "File icon",
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = message.content,
                                    color = if (isCurrentUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                if (message.type == "text" && isCurrentUser) {
                    DropdownMenuItem(
                        text = { Text("Chỉnh sửa") },
                        onClick = {
                            showEditDialog = true
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = "Edit Message") }
                    )
                }
                if (isCurrentUser) {
                    DropdownMenuItem(
                        text = { Text("Xóa") },
                        onClick = {
                            groupChatViewModel.deleteMessage(groupId, message.id)
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = "Delete Message") }
                    )
                }
            }
        }
    }
}

@Composable
fun EditMessageDialog(message: MessageModel, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(message.content) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chỉnh sửa tin nhắn") },
        text = {
            TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) {
                Text("Lưu")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

@Composable
fun SystemMessageItem(message: MessageModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = message.content,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
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
    onBanMember: (String) -> Unit,
    navController: NavHostController
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thành viên (${members.size})") },
        text = {
            LazyColumn {
                items(members) { member ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // User info
                        Column(modifier = Modifier
                            .weight(1f)
                            .clickable {
                                navController.navigate("${Routes.Profile}?userId=${member.userId}")
                            }
                        ) {
                            Text(text = member.name, fontWeight = FontWeight.Bold)
                            if (group.createdBy == member.userId) {
                                Text(text = "Admin", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }

                        // Admin actions
                        if (isAdmin && member.userId != currentUserId) {
                            Row {
                                IconButton(onClick = { onRemoveMember(member.userId, member.name) }) {
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
