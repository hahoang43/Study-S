package com.example.study_s.ui.screens.message

import android.Manifest
import android.app.Application
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.example.study_s.data.model.MessageModel
import com.example.study_s.data.model.UserModel
import com.example.study_s.util.createImageFile
import com.example.study_s.viewmodel.ChatViewModel
import com.example.study_s.viewmodel.ChatViewModelFactory
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.CharacterIterator
import java.text.StringCharacterIterator

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class, ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(
    chatId: String,
    targetUserId: String,
    navController: NavController
) {
    val context = LocalContext.current
    val chatViewModel: ChatViewModel = viewModel(
        factory = ChatViewModelFactory(context.applicationContext as Application, chatId, targetUserId)
    )

    val messages by chatViewModel.messages.collectAsState()
    val targetUser by chatViewModel.targetUser.collectAsState()
    val uploadState by chatViewModel.uploadState.collectAsState()

    val currentUserId = Firebase.auth.currentUser?.uid
    val lazyListState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    var showOptionsMenu by remember { mutableStateOf(false) }
    var showDeleteChatDialog by remember { mutableStateOf(false) }
    var showBlockUserDialog by remember { mutableStateOf(false) }

    var messageToAction by remember { mutableStateOf<MessageModel?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingText by remember { mutableStateOf("") }

    val isBlockedByTarget by chatViewModel.isBlockedByTarget.collectAsState()
    val haveIBlockedTarget by chatViewModel.haveIBlockedTarget.collectAsState()
    // --- Launchers for permissions and activity results ---
    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess ->
        if (isSuccess) {
            imageUri?.let { chatViewModel.sendFile(it, "image") }
        }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            val uri = createImageFile(context)
            if (uri != null) {
                imageUri = uri
                cameraLauncher.launch(uri)
            }
        }
    }
    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { chatViewModel.sendFile(it, "image") }
    }
    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { chatViewModel.sendFile(it, "file") }
    }

    LaunchedEffect(messages) {
        if (messages.isNotEmpty()) {
            lazyListState.animateScrollToItem(messages.size - 1)
        }
    }

    // --- Dialogs ---
    if (showDeleteChatDialog) {
        AlertDialog(            onDismissRequest = { showDeleteChatDialog = false },
            title = { Text("Xóa cuộc trò chuyện") },
            text = { Text("Bạn có chắc chắn muốn xóa toàn bộ cuộc trò chuyện này không? Hành động này không thể hoàn tác.") },
            confirmButton = {
                Button(
                    onClick = {
                        chatViewModel.deleteChat()
                        showDeleteChatDialog = false
                        navController.popBackStack() // Quay về màn hình trước sau khi xóa
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Xóa") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteChatDialog = false }) { Text("Hủy") }
            }
        )    }
    if (showBlockUserDialog) {
        AlertDialog(
            onDismissRequest = { showBlockUserDialog = false },
            title = { Text("Chặn người dùng") },
            text = { Text("Bạn có chắc chắn muốn chặn ${targetUser?.name ?: "người này"} không? Bạn sẽ không thể gửi hoặc nhận tin nhắn từ họ nữa.") },
            confirmButton = {
                Button(
                    onClick = {
                        chatViewModel.blockUser()
                        showBlockUserDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Chặn") }
            },
            dismissButton = {
                TextButton(onClick = { showBlockUserDialog = false }) { Text("Hủy") }
            }
        )
    }
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                messageToAction = null // Reset khi hủy
            },
            title = { Text("Xóa tin nhắn") },
            text = { Text("Bạn có chắc chắn muốn xóa tin nhắn này không?") },
            confirmButton = {
                Button(
                    onClick = {
                        // Gọi hàm deleteMessage trong ViewModel với ID của tin nhắn đã chọn
                        messageToAction?.id?.let { chatViewModel.deleteMessage(it) }
                        showDeleteDialog = false
                        messageToAction = null // Reset sau khi thực hiện
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Xóa") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    messageToAction = null // Reset khi hủy
                }) { Text("Hủy") }
            }
        )    }
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = {
                showEditDialog = false
                messageToAction = null // Reset khi hủy
            },
            title = { Text("Sửa tin nhắn") },
            text = {
                // TextField để người dùng nhập nội dung mới
                OutlinedTextField(
                    value = editingText,
                    onValueChange = { editingText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nội dung mới") }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Gọi hàm editMessage với ID và nội dung mới
                        messageToAction?.id?.let { chatViewModel.editMessage(it, editingText) }
                        showEditDialog = false
                        messageToAction = null // Reset sau khi thực hiện
                    },
                    enabled = editingText.isNotBlank() // Chỉ cho phép sửa khi có nội dung
                ) { Text("Lưu") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showEditDialog = false
                    messageToAction = null // Reset khi hủy
                }) { Text("Hủy") }
            }
        )    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            navController.navigate("profile/$targetUserId")
                        }
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(model = targetUser?.avatarUrl),
                            contentDescription = "Avatar",
                            modifier = Modifier.size(40.dp).clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = targetUser?.name ?: "Đang tải...", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showOptionsMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Tùy chọn")
                        }
                        DropdownMenu(
                            expanded = showOptionsMenu,
                            onDismissRequest = { showOptionsMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Xóa cuộc trò chuyện") },
                                onClick = {
                                    showDeleteChatDialog = true
                                    showOptionsMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if(haveIBlockedTarget) "Bỏ chặn người này" else "Chặn người này") },
                                onClick = {
                                    if(haveIBlockedTarget) {
                                        chatViewModel.unblockUser() // Bạn cần thêm hàm này vào ViewModel
                                    } else {
                                        showBlockUserDialog = true
                                    }
                                    showOptionsMenu = false
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        // ✅ Cấu trúc `bottomBar` đã được sửa lại
        bottomBar = {
            Column {
                if (uploadState is ChatViewModel.UploadState.Uploading) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
                when {
                    isBlockedByTarget -> {
                        BlockedMessageBar(message = "Bạn không thể trả lời cuộc trò chuyện này.")
                    }
                    haveIBlockedTarget -> {
                        BlockedMessageBar(
                            message = "Bạn đã chặn ${targetUser?.name ?: "người này"}.",
                            showUnblockButton = true,
                            onUnblock = {
                                chatViewModel.unblockUser() // Bạn cần thêm hàm này vào ViewModel
                            }
                        )
                    }
                    else -> {
                        // Di chuyển MessageInputBar vào đây
                        MessageInputBar(
                            onSendMessage = { text -> chatViewModel.sendMessage(text) },
                            uploadState = uploadState,
                            focusManager = focusManager,
                            imagePickerLauncher = { imagePickerLauncher.launch("image/*") },
                            cameraLauncher = {
                                when {
                                    cameraPermissionState.status.isGranted -> {
                                        val uri = createImageFile(context)
                                        if (uri != null) {
                                            imageUri = uri
                                            cameraLauncher.launch(uri)
                                        }
                                    }
                                    cameraPermissionState.status.shouldShowRationale -> {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                    else -> {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                }
                            },
                            filePickerLauncher = { filePickerLauncher.launch("*/*") }
                        )
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .clickable { focusManager.clearFocus() },
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsIndexed(messages) { index, message ->
                val isCurrentUser = message.senderId == currentUserId
                val previousMessage = messages.getOrNull(index - 1)
                val showAvatar = !isCurrentUser && (previousMessage == null || previousMessage.senderId != message.senderId)

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = if (isCurrentUser) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Box {
                        MessageBubble(
                            message = message,
                            isCurrentUser = isCurrentUser,
                            showAvatar = showAvatar,
                            avatarUrl = if (showAvatar) targetUser?.avatarUrl else null,
                            onLongPress = { messageToAction = message },
                            onClick = {}
                        )

                        DropdownMenu(
                            expanded = messageToAction == message,
                            onDismissRequest = { messageToAction = null }
                        ) {
                            val fileName = message.fileName
                            if (fileName != null && (message.type == "file" || message.type == "image")) {
                                DropdownMenuItem(
                                    text = { Text("Tải về") },
                                    leadingIcon = { Icon(Icons.Default.Download, "Tải về") },
                                    onClick = {
                                        scope.launch {
                                            downloadFile(context, message.content, fileName)
                                        }
                                        messageToAction = null
                                    }
                                )
                            }
                            if (isCurrentUser) {
                                if (message.type == "text") {
                                    DropdownMenuItem(
                                        text = { Text("Sửa") },
                                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = "Sửa") },
                                        onClick = {
                                            editingText = messageToAction?.content ?: ""
                                            showEditDialog = true
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Xóa") },
                                    leadingIcon = { Icon(Icons.Default.DeleteOutline, "Xóa") },
                                    onClick = { showDeleteDialog = true }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ✅ Tách thanh nhập liệu ra thành một Composable riêng
@Composable
fun MessageInputBar(
    onSendMessage: (String) -> Unit,
    uploadState: ChatViewModel.UploadState,
    focusManager: FocusManager,
    imagePickerLauncher: () -> Unit,
    cameraLauncher: () -> Unit,
    filePickerLauncher: () -> Unit
) {
    var messageText by remember { mutableStateOf("") }
    var showAttachmentMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                IconButton(onClick = { showAttachmentMenu = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Đính kèm")
                }
                DropdownMenu(
                    expanded = showAttachmentMenu,
                    onDismissRequest = { showAttachmentMenu = false }
                ) {
                    DropdownMenuItem(text = { Text("Gửi ảnh") }, leadingIcon = { Icon(Icons.Default.Image, "Ảnh") }, onClick = { imagePickerLauncher(); showAttachmentMenu = false })
                    DropdownMenuItem(text = { Text("Chụp ảnh") }, leadingIcon = { Icon(Icons.Default.CameraAlt, "Chụp ảnh") }, onClick = { cameraLauncher(); showAttachmentMenu = false })
                    DropdownMenuItem(text = { Text("Gửi tệp") }, leadingIcon = { Icon(Icons.Default.AttachFile, "Tệp") }, onClick = { filePickerLauncher(); showAttachmentMenu = false })
                }
            }

            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Nhập tin nhắn...") },
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = MaterialTheme.colorScheme.surface,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.surface
                )
            )

            IconButton(
                onClick = {
                    if (messageText.isNotBlank()) {
                        onSendMessage(messageText)
                        messageText = ""
                        focusManager.clearFocus()
                    }
                },
                enabled = messageText.isNotBlank() && uploadState !is ChatViewModel.UploadState.Uploading
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Gửi")
            }
        }
    }
}


// Các Composable `MessageBubble`, `BlockedMessageBar` và hàm `humanReadableByteCountSI` giữ nguyên như cũ...
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: MessageModel,
    isCurrentUser: Boolean,
    showAvatar: Boolean,
    avatarUrl: String?,
    onLongPress: () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            )
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        if (showAvatar) {
            Image(
                painter = rememberAsyncImagePainter(model = avatarUrl),
                contentDescription = "Avatar",
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )
        } else if (!isCurrentUser) {
            Spacer(modifier = Modifier.width(32.dp))
        }

        if (showAvatar) Spacer(modifier = Modifier.width(8.dp))

        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isCurrentUser) 16.dp else 0.dp,
                bottomEnd = if (isCurrentUser) 0.dp else 16.dp
            ),
            color = if (isCurrentUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            when (message.type) {
                "text" -> {
                    Text(
                        text = message.content,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                        color = if (isCurrentUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                "image" -> {
                    AsyncImage(
                        model = message.content,
                        contentDescription = "Ảnh đã gửi",
                        modifier = Modifier
                            .sizeIn(maxHeight = 300.dp, maxWidth = 250.dp)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
                "file" -> {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.InsertDriveFile,
                            contentDescription = "File",
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = message.fileName ?: "Tệp đính kèm",
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrentUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            message.fileSize?.let {
                                Text(
                                    text = humanReadableByteCountSI(it),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = (if (isCurrentUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant).copy(
                                        alpha = 0.7f
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun BlockedMessageBar(
    message: String,
    showUnblockButton: Boolean = false,
    onUnblock: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        if (showUnblockButton) {
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(onClick = onUnblock) {
                Text("Bỏ chặn")
            }
        }
    }
}

private fun downloadFile(context: Context, url: String, fileName: String) {
    try {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(fileName)
            .setDescription("Đang tải xuống...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        downloadManager.enqueue(request)
        Toast.makeText(context, "Bắt đầu tải xuống...", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Tải xuống thất bại: ${e.message}", Toast.LENGTH_LONG).show()
        e.printStackTrace()
    }
}

fun humanReadableByteCountSI(bytes: Long): String {
    if (-1000 < bytes && bytes < 1000) {
        return "$bytes B"
    }
    val ci: CharacterIterator = StringCharacterIterator("kMGTPE")
    var tempBytes = bytes
    while (tempBytes <= -999950 || tempBytes >= 999950) {
        tempBytes /= 1000
        ci.next()
    }
    return String.format("%.1f %cB", tempBytes / 1000.0, ci.current())
}
