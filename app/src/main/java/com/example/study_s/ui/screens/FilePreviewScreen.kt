package com.example.study_s.ui.screens

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.study_s.data.repository.LibraryRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilePreviewScreen(
    navController: NavController,
    fileUrl: String?,
    fileName: String?
) {
    val context = LocalContext.current
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val libraryRepository = remember { LibraryRepository() }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            selectedFileUri = uri
        }
    )

    Scaffold(
        topBar = {
            TopBar(navController = navController, title = if (fileUrl == null) "Tải lên tệp" else "Xem trước")
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator()
            } else {
                // Chế độ xem trước hoặc tải lên
                if (fileUrl != null && fileName != null) {
                    // Chế độ xem trước
                    Icon(imageVector = Icons.Default.Description, contentDescription = "File Icon", modifier = Modifier.size(120.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = fileName, fontWeight = FontWeight.Bold, fontSize = 20.sp, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = { downloadFile(context, fileUrl, fileName) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Tải xuống")
                    }
                } else {
                    // Chế độ tải lên
                    Icon(imageVector = Icons.Default.CloudUpload, contentDescription = "Upload Icon", modifier = Modifier.size(120.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Chưa có tệp nào được chọn", fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = { filePickerLauncher.launch("*/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Chọn tệp")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            selectedFileUri?.let {
                                val documentName = getFileName(context, it) ?: "Unknown File"
                                val mimeType = context.contentResolver.getType(it) ?: "application/octet-stream"

                                isLoading = true
                                scope.launch {
                                    try {
                                        libraryRepository.uploadFile(
                                            context = context,
                                            fileUri = it,
                                            fileName = documentName,
                                            mimeType = mimeType,
                                            uploaderId = "temp_user_id", // Thay thế bằng ID người dùng thực
                                            uploaderName = "temp_user_name" // Thay thế bằng tên người dùng thực
                                        )
                                        Toast.makeText(context, "Tải lên thành công!", Toast.LENGTH_SHORT).show()
                                        navController.popBackStack()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Tải lên thất bại: ${e.message}", Toast.LENGTH_LONG).show()
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                        },
                        enabled = selectedFileUri != null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Tải lên")
                    }
                }
            }
        }
    }
}

fun downloadFile(context: Context, url: String, fileName: String) {
    try {
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(fileName)
            .setDescription("Đang tải xuống...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)
        Toast.makeText(context, "Bắt đầu tải xuống...", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Lỗi khi tải xuống: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

fun getFileName(context: Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (columnIndex >= 0) {
                    result = cursor.getString(columnIndex)
                }
            }
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/')
        if (cut != -1) {
            if (cut != null) {
                result = result.substring(cut + 1)
            }
        }
    }
    return result
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(navController: NavController, title: String) {
    TopAppBar(
        title = { Text(text = title) },
        navigationIcon = {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewFilePreviewScreenUpload() {
    FilePreviewScreen(navController = rememberNavController(), fileUrl = null, fileName = null)
}

@Preview(showBackground = true)
@Composable
fun PreviewFilePreviewScreenDownload() {
    FilePreviewScreen(navController = rememberNavController(), fileUrl = "https://example.com/file.pdf", fileName = "example.pdf")
}
