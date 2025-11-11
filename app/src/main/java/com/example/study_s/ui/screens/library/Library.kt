package com.example.study_s.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.rememberDismissState
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.study_s.ui.navigation.Routes // Đảm bảo bạn có Routes.UploadFile và Routes.FilePreview
import com.example.study_s.ui.screens.components.BottomNavBar
import com.example.study_s.viewmodel.LibraryViewModel
import com.google.firebase.auth.FirebaseAuth
import java.net.URLEncoder

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

    // State để quản lý tệp đang được chọn để tải xuống
    var fileToDownload by remember { mutableStateOf<LibraryFile?>(null) }
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

    // Hiển thị dialog xác nhận tải xuống nếu có tệp được chọn
    fileToDownload?.let { file ->
        DownloadConfirmationDialog(
            onConfirm = {
                val encodedUrl = URLEncoder.encode(file.fileUrl, "UTF-8")
                navController.navigate("${Routes.FilePreview}?fileUrl=${encodedUrl}&fileName=${file.fileName}")
                fileToDownload = null // Đóng dialog sau khi xác nhận
            },
            onDismiss = {
                fileToDownload = null // Đóng dialog khi hủy
            }
        )
    }

    Scaffold(
        bottomBar = {
            if (currentRoute != null) {
                BottomNavBar(navController = navController, currentRoute = currentRoute)
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        // FloatingActionButton được di chuyển lên trên
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
                    .background(color = Color.White)
                    .padding(horizontal = 16.dp)
            ) {
                // TopBar tùy chỉnh mới
                LibraryTopBar(
                    // SỬA Ở ĐÂY: Điều hướng đến màn hình TẢI LÊN TỆP
                    onAddClick = { navController.navigate(Routes.FilePreview) }
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
                        onFileClick = { file -> fileToDownload = file } // Cập nhật state khi click
                    )
                    1 -> DiscoverContent(
                        files = filteredFiles,
                        onFileClick = { file -> fileToDownload = file } // Cập nhật state khi click
                    )
                }
            }
        }
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
        // Thay đổi tại đây
        IconButton(
            onClick = onAddClick,
            modifier = Modifier.size(40.dp), // Bỏ .background()
            colors = IconButtonDefaults.iconButtonColors( // Thêm thuộc tính colors
                containerColor = MaterialTheme.colorScheme.primary, // Màu nền của nút
                contentColor = Color.White // Màu của icon bên trong
            )
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add File"
                // Không cần tint ở đây vì đã có contentColor
            )
        }
    }
}

// ... (Các composable còn lại không thay đổi)
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MyFilesContent(
    files: List<LibraryFile>,
    viewModel: LibraryViewModel,
    currentUserId: String?,
    onFileClick: (LibraryFile) -> Unit // Truyền lambda vào
) {
    var showDialog by remember { mutableStateOf<LibraryFile?>(null) }

    showDialog?.let { fileToDelete ->
        DeleteConfirmationDialog(
            onConfirm = {
                viewModel.deleteFile(fileToDelete)
                showDialog = null
            },
            onDismiss = {
                showDialog = null
            }
        )
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(items = files, key = { it.id }) { file ->
            val dismissState = rememberDismissState(
                confirmStateChange = {
                    if (it == DismissValue.DismissedToStart) {
                        if (file.uploaderId == currentUserId) {
                            showDialog = file
                        }
                        false
                    } else false
                }
            )

            SwipeToDismiss(
                state = dismissState,
                directions = setOf(DismissDirection.EndToStart),
                background = {
                    val color = when (dismissState.targetValue) {
                        DismissValue.DismissedToStart -> Color.Red.copy(alpha = 0.8f)
                        else -> Color.Transparent
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(color, shape = RoundedCornerShape(12.dp))
                            .padding(horizontal = 20.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                    }
                },
                dismissContent = {
                    FileListItem(file = file, onFileClick = onFileClick) // Truyền lambda xuống
                }
            )
        }
    }
}

@Composable
fun DiscoverContent(files: List<LibraryFile>, onFileClick: (LibraryFile) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(files) { file ->
            FileListItem(file = file, onFileClick = onFileClick) // Truyền lambda xuống
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

// Dialog xác nhận tải tệp
@Composable
fun DownloadConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Xác nhận") },
        text = { Text("Bạn có chắc chắn muốn tải tệp này?") },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Tải") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}


@Composable
fun FileListItem(file: LibraryFile, onFileClick: (LibraryFile) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .border(1.dp, Color(0xFFB8C7E0), RoundedCornerShape(12.dp))
            .padding(16.dp)
            .clickable { onFileClick(file) }, // Gọi lambda được truyền vào
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
                    color = Color.Gray
                )
            }
        }
        Icon(
            imageVector = Icons.Default.Download, // Thay đổi icon cho phù hợp
            contentDescription = "Download",
            modifier = Modifier.size(24.dp),
            tint = Color(0xFF1A73E8)
        )
    }
}

@Composable
fun FileIcon(mimeType: String, fileUrl: String) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(Color(0xFFE0E0E0), RoundedCornerShape(8.dp)),
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
        fileName = "Bài tập giải tích.pdf",
        fileUrl = "https://example.com/file.pdf",
        mimeType = "application/pdf",
        uploaderName = "Nguyễn Văn A"
    )
    FileListItem(file = file, onFileClick = {})
}

