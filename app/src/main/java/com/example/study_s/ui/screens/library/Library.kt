package com.example.study_s.ui.screens.library

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.study_s.data.model.LibraryFile
import com.example.study_s.ui.navigation.Routes
import com.example.study_s.ui.screens.components.BottomNavBar
import com.example.study_s.viewmodel.LibraryViewModel
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    navController: NavController,
    libraryViewModel: LibraryViewModel = viewModel()
) {
    val files by libraryViewModel.files.collectAsState()
    val isRefreshing by libraryViewModel.isRefreshing.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val firebaseAuth = FirebaseAuth.getInstance()
    var currentUserId by remember { mutableStateOf(firebaseAuth.currentUser?.uid) }

    val context = LocalContext.current

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUserId = auth.currentUser?.uid
        }
        firebaseAuth.addAuthStateListener(listener)
        onDispose { firebaseAuth.removeAuthStateListener(listener) }
    }

    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Tệp của tôi", "Khám phá")
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(currentUserId) {
        if (currentUserId != null) {
            libraryViewModel.loadAllFiles()
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute != null) {
                BottomNavBar(navController = navController, currentRoute = currentRoute)
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { libraryViewModel.loadAllFiles() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp)
            ) {
                LibraryTopBar(
                    onAddClick = { navController.navigate(Routes.UploadFile) }
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Tìm kiếm tài liệu...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                TabRow(selectedTabIndex = selectedTabIndex) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = {
                                Text(
                                    title,
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val filteredFiles = files.filter { it.fileName.contains(searchQuery, ignoreCase = true) }

                when (selectedTabIndex) {
                    0 -> MyFilesContent(
                        files = filteredFiles.filter { it.uploaderId == currentUserId },
                        viewModel = libraryViewModel,
                        currentUserId = currentUserId,
                        onFileClick = { file -> downloadFile(context, file.fileUrl, file.fileName) }
                    )
                    1 -> DiscoverContent(
                        files = filteredFiles,
                        onFileClick = { file -> downloadFile(context, file.fileUrl, file.fileName) }
                    )
                }
            }
        }
    }
}

fun downloadFile(context: Context, url: String, fileName: String) {
    try {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(fileName)
            .setDescription("Đang tải xuống...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        downloadManager.enqueue(request)
        Toast.makeText(context, "Bắt đầu tải xuống $fileName", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Lỗi tải xuống: ${e.message}", Toast.LENGTH_LONG).show()
    }
}


@Composable
fun LibraryTopBar(onAddClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Thư viện",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        IconButton(
            onClick = onAddClick,
            modifier = Modifier.size(40.dp),
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add File"
            )
        }
    }
}

@Composable
fun MyFilesContent(
    files: List<LibraryFile>,
    viewModel: LibraryViewModel,
    currentUserId: String?,
    onFileClick: (LibraryFile) -> Unit
) {
    var fileToDelete by remember { mutableStateOf<LibraryFile?>(null) }
    var fileToEdit by remember { mutableStateOf<LibraryFile?>(null) }

    fileToDelete?.let { file ->
        DeleteConfirmationDialog(
            onConfirm = {
                viewModel.deleteFile(file)
                fileToDelete = null
            },
            onDismiss = { fileToDelete = null }
        )
    }

    fileToEdit?.let { file ->
        EditFileNameDialog(
            file = file,
            onConfirm = { newName ->
                viewModel.updateFileName(file.id, newName)
                fileToEdit = null
            },
            onDismiss = { fileToEdit = null }
        )
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(items = files, key = { it.id }) { file ->
            FileListItem(
                file = file,
                onFileClick = onFileClick,
                isUploader = file.uploaderId == currentUserId,
                onEditClick = { fileToEdit = file },
                onDeleteClick = { fileToDelete = file }
            )
        }
    }
}

@Composable
fun DiscoverContent(files: List<LibraryFile>, onFileClick: (LibraryFile) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(files) { file ->
            FileListItem(
                file = file, 
                onFileClick = onFileClick,
                isUploader = false, // Cannot edit/delete in discover
                onEditClick = {}, 
                onDeleteClick = {}
            )
        }
    }
}

@Composable
fun DeleteConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Xác nhận xóa") },
        text = { Text("Bạn có chắc chắn muốn xóa tệp này không?") },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Xóa") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}

@Composable
fun EditFileNameDialog(
    file: LibraryFile,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newName by remember(file.fileName) { mutableStateOf(file.fileName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chỉnh sửa tên tệp") },
        text = {
            TextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("Tên tệp mới") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(newName) },
                enabled = newName.isNotBlank()
            ) {
                Text("Lưu")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}

@Composable
fun FileListItem(
    file: LibraryFile,
    onFileClick: (LibraryFile) -> Unit,
    isUploader: Boolean,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        onClick = { onFileClick(file) }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                FileIcon(mimeType = file.mimeType, fileUrl = file.fileUrl)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = file.fileName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tải lên bởi: ${file.uploaderName}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (isUploader) {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Tùy chọn")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Chỉnh sửa") },
                            onClick = {
                                onEditClick()
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = "Chỉnh sửa") }
                        )
                        DropdownMenuItem(
                            text = { Text("Xóa") },
                            onClick = {
                                onDeleteClick()
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = "Xóa") }
                        )
                    }
                }
            } else {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Download",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun FileIcon(mimeType: String, fileUrl: String) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (mimeType.startsWith("image/")) {
            AsyncImage(
                model = fileUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val icon = when (mimeType) {
                "application/pdf" -> Icons.Default.PictureAsPdf
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> Icons.Default.Description
                "application/vnd.ms-powerpoint",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation" -> Icons.Default.Slideshow
                else -> Icons.Default.Description
            }
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(32.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewLibraryScreen() {
    LibraryScreen(navController = rememberNavController())
}

@Preview(showBackground = true)
@Composable
fun PreviewFileListItem() {
    val file = LibraryFile(
        id = "1",
        fileName = "Bài tập giải tích.pdf",
        fileUrl = "https://example.com/file.pdf",
        mimeType = "application/pdf",
        uploaderName = "Nguyễn Văn A",
        uploaderId = "123"
    )
    FileListItem(file = file, onFileClick = {}, isUploader = true, onEditClick = {}, onDeleteClick = {})
}
