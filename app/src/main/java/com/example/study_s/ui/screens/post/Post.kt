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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import kotlinx.coroutines.launch
import coil.compose.AsyncImage

import com.example.study_s.viewmodel.AuthViewModel
import com.example.study_s.viewmodel.AuthViewModelFactory

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
fun PostScreen(
    navController: NavController,
    postViewModel: PostViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory()),
    postToEdit: PostModel? = null
) {
    // Xác định chế độ: true nếu đang chỉnh sửa, false nếu tạo mới
    val currentUser by authViewModel.currentUser.collectAsState()
    val isEditMode = postToEdit != null

    // --- Khởi tạo State với dữ liệu có sẵn nếu là chế độ chỉnh sửa ---
    var postContent by remember { mutableStateOf(postToEdit?.content ?: "") }

    // State cho ảnh/file
    // imageUrl/fileName từ bài đăng cũ
    var existingImageUrl by remember { mutableStateOf(postToEdit?.imageUrl) }
    var existingFileName by remember { mutableStateOf(postToEdit?.fileName) }

    // Uri cho ảnh/file mới được chọn
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var fileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(0f) }
    val coroutineScope = rememberCoroutineScope()
    val libraryRepository = remember { LibraryRepository() }
    // Làm mới dữ liệu người dùng khi vào màn hình
    LaunchedEffect(Unit) {
        authViewModel.reloadCurrentUser()
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri // Chọn ảnh mới
        fileUri = null
        selectedFileName = null
        existingImageUrl = null // Hủy ảnh cũ
        existingFileName = null // Hủy file cũ
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        fileUri = uri // Chọn file mới
        selectedFileName = uri?.let { getFileName(it, context) }
        imageUri = null
        existingImageUrl = null // Hủy ảnh cũ
        existingFileName = null // Hủy file cũ
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Chỉnh sửa bài đăng" else "Tạo bài đăng") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            val user = currentUser
                            if (user != null) {
                                coroutineScope.launch {
                                    isLoading = true
                                    uploadProgress = 0f
                                    try {
                                        var newImageUrl = existingImageUrl
                                        var newFileName = existingFileName

                                        val uriToUpload = imageUri ?: fileUri
                                        if (uriToUpload != null) {
                                            val fileName = selectedFileName ?: getFileName(uriToUpload, context) ?: "file"
                                            val mimeType = context.contentResolver.getType(uriToUpload) ?: "*/*"
                                            val uploadResultUrl = libraryRepository.uploadFile(context, uriToUpload, fileName, mimeType, user.uid, user.displayName ?: "User") { progressInt ->
                                                uploadProgress = progressInt / 100f
                                            }

                                            newImageUrl = uploadResultUrl
                                            if (imageUri != null) newFileName = null
                                            if (fileUri != null) newFileName = selectedFileName
                                        }

                                        if (isEditMode) {
                                            val updatedPost = postToEdit!!.copy(
                                                content = postContent,
                                                imageUrl = newImageUrl,
                                                fileName = newFileName
                                            )
                                            postViewModel.updatePost(updatedPost) {
                                                Toast.makeText(context, "Cập nhật thành công!", Toast.LENGTH_SHORT).show()
                                                navController.popBackStack()
                                            }
                                        } else {
                                            val newPost = PostModel(
                                                authorId = user.uid,
                                                content = postContent,
                                                imageUrl = newImageUrl,
                                                fileName = newFileName
                                            )
                                            postViewModel.createNewPost(newPost)
                                            navController.popBackStack()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Thao tác thất bại: ${e.message}", Toast.LENGTH_LONG).show()
                                        e.printStackTrace()
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                        },
                        enabled = (postContent.isNotBlank() || imageUri != null || fileUri != null || existingImageUrl != null) && currentUser != null && !isLoading,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (isEditMode) "Lưu" else "Đăng", fontSize = 16.sp)
                    }
                }
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()) // Cho phép cuộn nếu nội dung dài
            ) {
                // ... Giao diện người dùng (giữ nguyên)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = currentUser?.photoUrl, // Lấy URL từ state mới nhất
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                        // Hiển thị ảnh mặc định này khi `model` là null hoặc có lỗi
                        placeholder = painterResource(id = R.drawable.ic_profile),
                        error = painterResource(id = R.drawable.ic_profile)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (currentUser != null) currentUser?.displayName ?: "Đang tải..." else "Đang tải...",
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = postContent,
                    onValueChange = { postContent = it },
                    placeholder = { Text("Bạn đang nghĩ gì? ...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 120.dp),
                    shape = RoundedCornerShape(10.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start, // Chỉ cần các nút ở bên trái
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = "Chọn ảnh", modifier = Modifier.size(28.dp))
                    }
                    IconButton(onClick = { filePickerLauncher.launch("*/*") }) {
                        Icon(Icons.Default.AttachFile, contentDescription = "Đính kèm tệp", modifier = Modifier.size(28.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- Hiển thị ảnh/file đã có hoặc mới chọn ---

                // Ảnh mới chọn
                if (imageUri != null) {
                    Box(modifier = Modifier.height(200.dp)) {
                        Image(
                            painter = rememberAsyncImagePainter(imageUri),
                            contentDescription = "Ảnh đã chọn",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = { imageUri = null },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Xóa ảnh", tint = Color.White)
                        }
                    }
                }
                // Ảnh đã có từ trước
                else if (existingImageUrl != null) {
                    Box(modifier = Modifier.height(200.dp)) {
                        Image(
                            painter = rememberAsyncImagePainter(existingImageUrl),
                            contentDescription = "Ảnh hiện tại",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = { existingImageUrl = null }, // Xóa ảnh cũ
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Xóa ảnh", tint = Color.White)
                        }
                    }
                }


                // File mới chọn
                if (fileUri != null && selectedFileName != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = selectedFileName ?: "Tệp đính kèm", modifier = Modifier.weight(1f))
                        IconButton(onClick = { fileUri = null; selectedFileName = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Xóa tệp")
                        }
                    }
                }
                // File đã có từ trước
                else if (existingFileName != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = existingFileName!!, modifier = Modifier.weight(1f))
                        IconButton(onClick = { existingFileName = null }) { // Xóa file cũ
                            Icon(Icons.Default.Close, contentDescription = "Xóa tệp")
                        }
                    }
                }
            }
            // ... Giao diện loading (giữ nguyên)
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable(enabled = false, onClick = {}),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Đang xử lý: ${(uploadProgress * 100).toInt()}%", color = Color.White, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}
