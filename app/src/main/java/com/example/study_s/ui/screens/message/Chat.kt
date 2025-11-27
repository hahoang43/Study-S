package com.example.study_s.ui.screens.message
// Thêm các import này vào đầu file Chat.kt
import com.example.study_s.util.downloadFile
import com.example.study_s.util.isFileDownloaded
import com.example.study_s.util.openFile
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import android.app.Application
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilePresent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.example.study_s.data.model.Message
import com.example.study_s.viewmodel.ChatViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.CharacterIterator
import java.text.StringCharacterIterator
import androidx.lifecycle.ViewModelProvider
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel

class ChatViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(navController: NavController, targetUserId: String, chatViewModel: ChatViewModel = viewModel()) {
    // Lấy application context
    val context = LocalContext.current.applicationContext as Application
    // Sử dụng factory để tạo ViewModel
    val chatViewModel: ChatViewModel = viewModel(factory = ChatViewModelFactory(context))

    val messages by chatViewModel.messages.collectAsState()
    val targetUser by chatViewModel.targetUser.collectAsState(initial = null)
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    // States for message actions
    var messageToAction by remember { mutableStateOf<Message?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var editingText by remember { mutableStateOf("") }
    val lazyListState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let { chatViewModel.sendFile(it) }
        }
    )

    LaunchedEffect(targetUserId) {
        chatViewModel.loadChat(targetUserId)
        chatViewModel.loadTargetUserData(targetUserId)
    }

    LaunchedEffect(messages) {
        if (messages.isNotEmpty()) {
            lazyListState.animateScrollToItem(messages.size - 1)
        }
    }


    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Sửa tin nhắn") },
            text = {
                OutlinedTextField(
                    value = editingText,
                    onValueChange = { editingText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nội dung tin nhắn") }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        messageToAction?.let {
                            chatViewModel.editMessage(it.id, editingText)
                        }
                        showEditDialog = false
                        messageToAction = null
                    },
                    enabled = editingText.isNotBlank()
                ) {
                    Text("Lưu")
                }
            },
            dismissButton = {
                Button(onClick = { showEditDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Xóa tin nhắn") },
            text = { Text("Bạn có chắc chắn muốn xóa tin nhắn này không?") },
            confirmButton = {
                TextButton(
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


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = rememberAsyncImagePainter(model = targetUser?.avatarUrl ?: ""),
                            contentDescription = "Avatar",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = targetUser?.name ?: "Đang tải...", fontWeight = FontWeight.Bold)
                        }
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            var messageText by remember { mutableStateOf("") }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { filePickerLauncher.launch("*/*") }) {
                    Icon(imageVector = Icons.Default.AttachFile, contentDescription = "Attach file")
                }
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Nhập tin nhắn...") },
                    shape = RoundedCornerShape(24.dp)
                )
                IconButton(
                    onClick = {
                        if (messageText.isNotBlank() && currentUserId != null) {
                            chatViewModel.sendMessage(messageText)
                            messageText = ""
                            focusManager.clearFocus()
                        }
                    },
                    enabled = messageText.isNotBlank()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send"
                    )
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

                // ✅ THAY ĐỔI LỚN Ở ĐÂY
                // Box giờ đây chỉ bọc DropdownMenu và nội dung của nó, không còn fillMaxWidth nữa
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    // Căn chỉnh nội dung của Box sang phải hoặc trái
                    contentAlignment = if (isCurrentUser) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    // Box này hoạt động như một "anchor" (điểm neo) cho DropdownMenu
                    Box {
                        MessageBubble(
                            message = message,
                            isCurrentUser = isCurrentUser,
                            showAvatar = showAvatar,
                            avatarUrl = if (showAvatar) targetUser?.avatarUrl else null,
                            onLongPress = {
                                // Chỉ cho phép hành động nếu là người dùng hiện tại
                                if (isCurrentUser) {
                                    messageToAction = message
                                }
                            },
                            onClick = {
                                // Xử lý sự kiện click cho tin nhắn tệp
                                if (message.type != "text" && message.fileName != null) {
                                    if (isFileDownloaded(message.fileName)) {
                                        openFile(context, message.fileName)
                                    }
                                    // Nếu muốn tự động tải khi nhấn vào mà chưa có file,
                                    // bạn có thể thêm logic downloadFile ở đây.
                                }
                            }
                        )

                        DropdownMenu(
                            expanded = messageToAction == message,
                            onDismissRequest = { messageToAction = null }
                        ) {
                            // Nếu là tin nhắn văn bản
                            if (message.type == "text") {
                                DropdownMenuItem(
                                    text = { Text("Sửa") },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = "Sửa") },
                                    onClick = {
                                        editingText = messageToAction?.content ?: ""
                                        showEditDialog = true
                                    }
                                )
                            } else { // Nếu là tin nhắn tệp (ảnh, video, file)
                                val fileName = message.fileName
                                if (fileName != null) {
                                    if (isFileDownloaded(fileName)) {
                                        DropdownMenuItem(
                                            text = { Text("Mở tệp") },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.FolderOpen,
                                                    contentDescription = "Mở"
                                                )
                                            },
                                            onClick = {
                                                openFile(context, fileName)
                                                messageToAction = null
                                            }
                                        )
                                    } else {
                                        DropdownMenuItem(
                                            text = { Text("Tải về") },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.Download,
                                                    contentDescription = "Tải về"
                                                )
                                            },
                                            onClick = {
                                                // ✅ Sử dụng scope để gọi suspend function
                                                scope.launch {
                                                    downloadFile(
                                                        context,
                                                        message.content,
                                                        fileName,
                                                        null // Bạn có thể truyền MimeType ở đây nếu có
                                                    )
                                                }
                                                messageToAction = null
                                            }
                                        )
                                    }
                                }
                            }

                            DropdownMenuItem(
                                text = { Text("Xóa") },
                                leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = "Delete") },
                                onClick = {
                                    showDeleteDialog = true
                                }
                            )
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
    message: Message,
    isCurrentUser: Boolean,
    showAvatar: Boolean,
    avatarUrl: String?,
    onLongPress: () -> Unit,
    onClick: () -> Unit // Thêm tham số onClick
) {
    // Row giờ đây không cần fillMaxWidth nữa vì Box cha đã xử lý việc căn lề
    Row(
        modifier = Modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            )
            .padding(vertical = 2.dp),
        // Không cần horizontalArrangement ở đây nữa
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
            Spacer(modifier = Modifier.width(40.dp))
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
                            .size(200.dp)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                "file" -> {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.FilePresent, contentDescription = "File")
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = message.fileName ?: "Tệp đính kèm",
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrentUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
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
