package com.example.study_s.ui.screens.library

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.study_s.data.model.LibraryFile
import com.example.study_s.ui.screens.components.BottomNavBar
import com.example.study_s.ui.screens.components.TopBar
import com.example.study_s.viewmodel.LibraryViewModel


@Composable
fun LibraryScreen(
    navController: NavController,
    libraryViewModel: LibraryViewModel = viewModel(),
    currentRoute: String? = null
) {
    val files by libraryViewModel.files.collectAsState()
    val isLoading by libraryViewModel.isLoading.collectAsState()
    val error by libraryViewModel.error.collectAsState()

    // Giả lập thông tin người dùng hiện tại
    val currentUserId = "user_123"
    val currentUserName by remember { mutableStateOf("User ABC") }

    // Bộ lọc và tìm kiếm
    var searchQuery by remember { mutableStateOf("") }
    val subjects by remember { mutableStateOf(listOf("Môn học", "Kinh tế vĩ mô", "Cấu trúc rời rạc", "Lập trình")) }
    var selectedSubject by remember { mutableStateOf(subjects.first()) }
    var expandedSubject by remember { mutableStateOf(false) }

    val context = LocalContext.current // Lấy Context hiện tại

    // Launcher để mở bộ chọn file hệ thống và TẢI LÊN TRỰC TIẾP
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let { fileUri ->
                // 1. Lấy MIME type và Tên file
                val (fileName, mimeType) = getFileInfo(context, fileUri)

                // 2. Gọi ViewModel để xử lý Upload file lên Cloudinary và lưu metadata
                libraryViewModel.uploadFile(
                    context = context, // Truyền Context
                    fileUri = fileUri,
                    fileName = fileName,
                    mimeType = mimeType,
                    uploaderId = currentUserId,
                    uploaderName = currentUserName
                )
            }
        }
    )

    // Logic lọc file
    val filteredFiles = files.filter { file ->
        val searchMatch = file.fileName.contains(searchQuery, ignoreCase = true) || file.uploaderName.contains(searchQuery, ignoreCase = true)
        val subjectMatch = selectedSubject == subjects.first() || file.fileName.contains(selectedSubject, ignoreCase = true)
        searchMatch && subjectMatch
    }


    Scaffold(
        topBar = {
            TopBar(
                onNavIconClick = { navController.navigateUp() },
                onNotificationClick = { /* TODO: Notification */ },
                onSearchClick = { /* TODO: Search */ }
            )
        },
        bottomBar = {
            BottomNavBar(navController = navController, currentRoute = currentRoute ?: "library")
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // Mở bộ chọn file hệ thống (cho phép chọn tất cả các loại file)
                    filePickerLauncher.launch("*/*")
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Upload File")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Thanh tìm kiếm
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Tìm kiếm tài liệu, môn học, người tải lên...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                shape = RoundedCornerShape(12.dp)
            )

            // Bộ lọc
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 16.dp)
            ) {
                // Dropdown 1: Môn học
                Box {
                    OutlinedButton(
                        onClick = { expandedSubject = true },
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(selectedSubject, maxLines = 1)
                    }
                    DropdownMenu(
                        expanded = expandedSubject,
                        onDismissRequest = { expandedSubject = false }
                    ) {
                        subjects.forEach { subject ->
                            DropdownMenuItem(
                                text = { Text(subject) },
                                onClick = {
                                    selectedSubject = subject
                                    expandedSubject = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Trạng thái Loading và Error
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                    Text("Đang tải file lên...", modifier = Modifier.padding(top = 80.dp), color = Color.Gray)
                }
            } else if (error != null) {
                Text("Lỗi: $error", color = Color.Red, modifier = Modifier.padding(vertical = 8.dp))
            } else if (files.isEmpty() && searchQuery.isBlank()) {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    Text("Chưa có tài liệu nào. Hãy tải lên!")
                }
            } else if (filteredFiles.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    Text("Không tìm thấy tài liệu phù hợp.", color = Color.Gray)
                }
            }

            // Danh sách file
            LazyColumn {
                items(filteredFiles) { file ->
                    FileCard(file = file) { fileUrl ->
                        // Mở link Cloudinary để tải xuống/xem
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fileUrl))
                        context.startActivity(intent)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

// --- Component con: Card hiển thị File ---
@Composable
fun FileCard(file: LibraryFile, onDownloadClick: (String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onDownloadClick(file.fileUrl) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Icon và Thông tin file
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(getMimeTypeColor(file.mimeType)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = getMimeTypeDisplayName(file.mimeType),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = file.fileName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Tải lên bởi: ${file.uploaderName}",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Nút Tải xuống (hoặc Xem)
            IconButton(onClick = { onDownloadClick(file.fileUrl) }) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Xem tài liệu",
                    tint = Color(0xFF0D47A1),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

// --- Hàm tiện ích chuyển đổi MimeType ---

@Composable
fun getMimeTypeColor(mimeType: String): Color {
    return when (mimeType.lowercase()) {
        "application/pdf" -> Color(0xFFE53935)
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/msword" -> Color(0xFF1976D2)
        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        "application/vnd.ms-powerpoint" -> Color(0xFFFDD835)
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/vnd.ms-excel" -> Color(0xFF43A047)
        else -> Color(0xFF9E9E9E)
    }
}

@Composable
fun getMimeTypeDisplayName(mimeType: String): String {
    return when (mimeType.lowercase()) {
        "application/pdf" -> "PDF"
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/msword" -> "DOCX"
        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        "application/vnd.ms-powerpoint" -> "PPTX"
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/vnd.ms-excel" -> "XLSX"
        else -> "FILE"
    }
}

/**
 * Hàm đọc tên file và mimeType từ Uri.
 */
fun getFileInfo(context: Context, uri: Uri): Pair<String, String> {
    var name = "Tên File Không Xác Định"
    var type = context.contentResolver.getType(uri) ?: "application/octet-stream"

    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1) {
                name = it.getString(nameIndex)
            }
        }
    }

    // Đảm bảo tên file được cắt ngắn nếu quá dài
    val finalName = if (name.length > 50) name.take(47) + "..." else name
    return Pair(finalName, type)
}

@Preview(showBackground = true)
@Composable
fun PreviewLibraryScreen() {
    LibraryScreen(navController = rememberNavController())
}