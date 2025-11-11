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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
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
                navigationIcon = { TextButton(onClick = { navController.popBackStack() }) { Text("Hủy", color = Color.Black) } },
                actions = { IconButton(onClick = { /* TODO */ }) { Icon(Icons.Default.MoreVert, contentDescription = "More") } }
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
                // ... (Phần UI header, textfield, preview không thay đổi) ...
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.avatar1), // TODO: Thay bằng avatar thật
                        contentDescription = "Avatar",
                        modifier = Modifier.size(50.dp).clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(currentUser?.displayName ?: "Người dùng", fontWeight = FontWeight.SemiBold)
                        Text("Chia sẻ công khai", color = Color.Gray, fontSize = 14.sp)
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
                Spacer(modifier = Modifier.height(16.dp))

                // Xem trước ảnh và tệp (giữ nguyên)
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

                Spacer(modifier = Modifier.weight(1f))

                // Các nút chức năng (giữ nguyên)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = "Chọn ảnh", modifier = Modifier.size(28.dp))
                    }
                    IconButton(onClick = { filePickerLauncher.launch("*/*") }) {
                        Icon(Icons.Default.AttachFile, contentDescription = "Đính kèm tệp", modifier = Modifier.size(28.dp))
                    }
                }
                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Nút Đăng - Cập nhật logic tải lên Firebase
                Button(
                    onClick = {
                        if (currentUser != null) {
                            coroutineScope.launch {
                                isLoading = true
                                uploadProgress = 0f // Reset tiến trình
                                try {
                                    var attachmentUrl: String? = null
                                    var attachmentFileName: String? = null

                                    // Xác định URI cần tải lên
                                    val uriToUpload = imageUri ?: fileUri

                                    // Tải lên nếu có tệp đính kèm
                                    if (uriToUpload != null) {

                                        // ✅ BẮT ĐẦU THAY ĐỔI
                                        val fileName = selectedFileName ?: getFileName(uriToUpload, context) ?: "file"
                                        val mimeType = context.contentResolver.getType(uriToUpload) ?: "*/*"

                                        // Lấy thông tin người dùng
                                        val uploaderId = currentUser.uid
                                        val uploaderName = currentUser.displayName?.takeIf { it.isNotBlank() }
                                            ?: currentUser.email
                                            ?: "Người dùng"

                                        // Gọi hàm 'uploadFile' (hàm gốc)
                                        // Hàm này sẽ tự động lưu metadata vào 'libraryFiles'
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
                                        // ✅ KẾT THÚC THAY ĐỔI

                                        // 'uploadFile' sẽ ném Exception nếu lỗi,
                                        // nên nếu code chạy đến đây, 'uploadResultUrl' chắc chắn có giá trị
                                        attachmentUrl = uploadResultUrl
                                        attachmentFileName = if (fileUri != null) selectedFileName else null
                                    }

                                    // Tạo đối tượng PostModel
                                    val newPost = PostModel(
                                        authorId = currentUser.uid,
                                        content = postContent,
                                        imageUrl = attachmentUrl,
                                        fileName = attachmentFileName
                                    )

                                    viewModel.createNewPost(newPost)
                                    navController.popBackStack()

                                } catch (e: Exception) {
                                    // Bắt lỗi từ hàm uploadFile
                                    Toast.makeText(context, "Tải lên thất bại: ${e.message}", Toast.LENGTH_LONG).show()
                                    e.printStackTrace()
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    enabled = (postContent.isNotBlank() || imageUri != null || fileUri != null) && currentUser != null && !isLoading,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Đăng", fontSize = 16.sp)
                }
            }

            // Màn hình chờ (Cập nhật để hiển thị tiến trình)
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).clickable(enabled = false, onClick = {}),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                        if (uploadProgress > 0f) {
                            LinearProgressIndicator(
                                progress = { uploadProgress },
                                modifier = Modifier.width(200.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}