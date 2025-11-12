package com.example.study_s.ui.screens.post

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.study_s.R
import com.example.study_s.data.model.PostModel
import com.example.study_s.data.repository.LibraryRepository
import com.example.study_s.viewmodel.PostViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

// Hàm hỗ trợ để lấy tên tệp từ Uri (Giữ nguyên)
fun getFileName(uri: Uri, context: Context): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
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
fun NewPostScreen(navController: NavController, viewModel: PostViewModel = viewModel()) {
    var postContent by remember { mutableStateOf("") }
    val currentUser = FirebaseAuth.getInstance().currentUser
    val context = LocalContext.current

    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var fileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }

    var isLoading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(0f) }
    val coroutineScope = rememberCoroutineScope()

    val libraryRepository = remember { LibraryRepository() }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
        fileUri = null
        selectedFileName = null
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        fileUri = uri
        selectedFileName = uri?.let { getFileName(it, context) }
        imageUri = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tạo bài đăng", fontWeight = FontWeight.Bold, fontSize = 22.sp) },
                actions = { TextButton(onClick = { navController.popBackStack() }) { Text("Hủy", color = Color.Black, fontSize = 22.sp, fontWeight = FontWeight.Bold) } }
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .fillMaxSize()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = rememberAsyncImagePainter(
                            model = currentUser?.photoUrl ?: R.drawable.avatar1
                        ),
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(currentUser?.displayName ?: "Người dùng", fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = postContent,
                    onValueChange = { postContent = it },
                    placeholder = { Text("Bạn đang nghĩ gì? ...") },
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 120.dp),
                    shape = RoundedCornerShape(10.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row {
                        IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = "Chọn ảnh", modifier = Modifier.size(28.dp))
                        }
                        IconButton(onClick = { filePickerLauncher.launch("*/*") }) {
                            Icon(Icons.Default.AttachFile, contentDescription = "Đính kèm tệp", modifier = Modifier.size(28.dp))
                        }
                    }
                    Button(
                        onClick = {
                            if (currentUser != null) {
                                coroutineScope.launch {
                                    isLoading = true
                                    uploadProgress = 0f // Reset tiến trình
                                    try {
                                        var attachmentUrl: String? = null
                                        var attachmentFileName: String? = null

                                        val uriToUpload = imageUri ?: fileUri

                                        if (uriToUpload != null) {
                                            val fileName = selectedFileName ?: getFileName(uriToUpload, context) ?: "file"
                                            val mimeType = context.contentResolver.getType(uriToUpload) ?: "*/*"
                                            val uploaderId = currentUser.uid
                                            val uploaderName = currentUser.displayName?.takeIf { it.isNotBlank() }
                                                ?: currentUser.email
                                                ?: "Người dùng"

                                            val uploadResultUrl = libraryRepository.uploadFile(
                                                context = context,
                                                fileUri = uriToUpload,
                                                fileName = fileName,
                                                mimeType = mimeType,
                                                uploaderId = uploaderId,
                                                uploaderName = uploaderName,
                                                onProgress = { progressInt ->
                                                    uploadProgress = progressInt / 100f
                                                }
                                            )
                                            attachmentUrl = uploadResultUrl
                                            attachmentFileName = if (fileUri != null) selectedFileName else null
                                        }

                                        val newPost = PostModel(
                                            authorId = currentUser.uid,
                                            content = postContent,
                                            imageUrl = attachmentUrl,
                                            fileName = attachmentFileName
                                        )

                                        viewModel.createNewPost(newPost)
                                        navController.popBackStack()

                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Tải lên thất bại: ${e.message}", Toast.LENGTH_LONG).show()
                                        e.printStackTrace()
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                        },
                        enabled = (postContent.isNotBlank() || imageUri != null || fileUri != null) && currentUser != null && !isLoading,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Đăng", fontSize = 16.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (imageUri != null) {
                    Box(modifier = Modifier.height(200.dp)) {
                        Image(
                            painter = rememberAsyncImagePainter(imageUri),
                            contentDescription = "Ảnh đã chọn",
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = { imageUri = null },
                            modifier = Modifier.align(Alignment.TopEnd).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Xóa ảnh", tint = Color.White)
                        }
                    }
                }
                if (fileUri != null && selectedFileName != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().border(1.dp, Color.Gray, RoundedCornerShape(8.dp)).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = selectedFileName ?: "Tệp đính kèm", modifier = Modifier.weight(1f))
                        IconButton(onClick = { fileUri = null; selectedFileName = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Xóa tệp")
                        }
                    }
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).clickable(enabled = false, onClick = {}),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Đang tải lên: ${(uploadProgress * 100).toInt()}%",
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}
