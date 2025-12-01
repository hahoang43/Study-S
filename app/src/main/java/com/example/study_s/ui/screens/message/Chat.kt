package com.example.study_s.ui.screens.message

import android.app.Application
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.example.study_s.data.model.MessageModel
import com.example.study_s.util.downloadFile
// import com.example.study_s.util.isFileDownloaded  // Không cần nữa
// import com.example.study_s.util.openFile // Không cần nữa
import com.example.study_s.viewmodel.ChatViewModel
import com.example.study_s.viewmodel.ChatViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.CharacterIterator
import java.text.StringCharacterIterator

@OptIn(ExperimentalMaterial3Api::class)
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

    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val lazyListState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    var showOptionsMenu by remember { mutableStateOf(false) }
    var showDeleteChatDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var messageToAction by remember { mutableStateOf<MessageModel?>(null) }
    var editingText by remember { mutableStateOf("") }
    var showAttachmentMenu by remember { mutableStateOf(false) }


    // Launcher để chọn ảnh
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { chatViewModel.sendFile(it, "image") }
    }

    // Launcher để chọn file bất kỳ
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { chatViewModel.sendFile(it, "file") }
    }
    LaunchedEffect(targetUserId) {
        chatViewModel.loadTargetUserData(targetUserId)
        chatViewModel.loadChat(targetUserId)
    }
    // Tự động cuộn xuống tin nhắn mới nhất
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            lazyListState.animateScrollToItem(messages.size - 1)
        }
    }

    // Dialog xác nhận xóa cuộc trò chuyện
    if (showDeleteChatDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteChatDialog = false },
            title = { Text("Xác nhận xóa") },
            text = { Text("Bạn có chắc chắn muốn xóa toàn bộ cuộc trò chuyện này không? Hành động này không thể hoàn tác.") },
            confirmButton = {
                Button(
                    onClick = {
                        chatViewModel.deleteChat() // Gọi hàm xóa trong ViewModel
                        showDeleteChatDialog = false
                        navController.popBackStack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Xóa") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteChatDialog = false }) { Text("Hủy") }
            }
        )
    }

    // Dialog xác nhận xóa tin nhắn
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Xác nhận xóa") },
            text = { Text("Bạn có muốn xóa tin nhắn này?") },
            confirmButton = {
                Button(
                    onClick = {
                        messageToAction?.let { chatViewModel.deleteMessage(it.id) }
                        showDeleteDialog = false
                        messageToAction = null
                    }
                ) { Text("Xóa") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Hủy") }
            }
        )
    }

    // Dialog sửa tin nhắn
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Sửa tin nhắn") },
            text = {
                OutlinedTextField(
                    value = editingText,
                    onValueChange = { editingText = it },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        messageToAction?.let { chatViewModel.editMessage(it.id, editingText) }
                        showEditDialog = false
                        messageToAction = null
                    }
                ) { Text("Lưu") }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("Hủy") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = rememberAsyncImagePainter(model = targetUser?.avatarUrl),
                            contentDescription = "Avatar",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = targetUser?.name ?: "Đang tải...",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
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
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            Column {
                // Hiển thị thanh tiến trình tải lên
                if (uploadState is ChatViewModel.UploadState.Uploading) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Đang gửi tệp...", style = MaterialTheme.typography.bodySmall)
                    }
                }

                // Thanh nhập liệu và các nút
                var messageText by remember { mutableStateOf("") }
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
                        // Nút đính kèm tệp
                        Box {
                            IconButton(onClick = { showAttachmentMenu = true }) {
                                Icon(Icons.Default.Add, contentDescription = "Đính kèm")
                            }
                            DropdownMenu(
                                expanded = showAttachmentMenu,
                                onDismissRequest = { showAttachmentMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Gửi ảnh") },
                                    leadingIcon = { Icon(Icons.Default.Image, "Ảnh") },
                                    onClick = {
                                        imagePickerLauncher.launch("image/*")
                                        showAttachmentMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Gửi tệp") },
                                    leadingIcon = { Icon(Icons.Default.AttachFile, "Tệp") },
                                    onClick = {
                                        filePickerLauncher.launch("*/*")
                                        showAttachmentMenu = false
                                    }
                                )
                            }
                        }

                        // Ô nhập liệu
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

                        // Nút gửi
                        IconButton(
                            onClick = {
                                if (messageText.isNotBlank()) {
                                    chatViewModel.sendMessage(messageText)
                                    messageText = ""
                                    focusManager.clearFocus()
                                }
                            },
                            enabled = messageText.isNotBlank() && uploadState !is ChatViewModel.UploadState.Uploading
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Gửi"
                            )
                        }
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
                            onLongPress = {
                                messageToAction = message
                            },
                            onClick = {}
                        )

                        // ✅ THAY ĐỔI: ĐƠN GIẢN HÓA MENU NGỮ CẢNH
                        DropdownMenu(
                            expanded = messageToAction == message,
                            onDismissRequest = { messageToAction = null }
                        ) {
                            val fileName = message.fileName
                            // Kiểm tra xem tin nhắn có phải là tệp hoặc ảnh không
                            if (fileName != null && (message.type == "file" || message.type == "image")) {
                                // Luôn hiển thị tùy chọn "Tải về"
                                DropdownMenuItem(
                                    text = { Text("Tải về") },
                                    leadingIcon = { Icon(Icons.Default.Download, "Tải về") },
                                    onClick = {
                                        scope.launch {
                                            downloadFile(context, message.content, fileName, null)
                                        }
                                        messageToAction = null
                                    }
                                )
                            }

                            // Chỉ hiển thị "Sửa" và "Xóa" cho tin nhắn của người dùng hiện tại
                            if (isCurrentUser) {
                                // Chỉ hiển thị "Sửa" cho tin nhắn văn bản
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

                                // Tùy chọn Xóa
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
