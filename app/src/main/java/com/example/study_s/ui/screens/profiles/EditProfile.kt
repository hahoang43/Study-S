// ĐƯỜNG DẪN: ui/screens/profiles/EditProfileScreen.kt

package com.example.study_s.ui.screens.profiles

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.study_s.R
// ✅ BƯỚC 1: THÊM CÁC IMPORT CẦN THIẾT
import com.example.study_s.viewmodel.AuthViewModel
import com.example.study_s.viewmodel.PostViewModel
import com.example.study_s.viewmodel.ProfileUiState
import com.example.study_s.viewmodel.ProfileViewModel
import com.example.study_s.viewmodel.ProfileViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = viewModel(factory = ProfileViewModelFactory()),
    // ✅ BƯỚC 2: NHẬN CÁC VIEWMODEL TOÀN CỤC ĐƯỢC TRUYỀN TỪ NAVGRAPH
    authViewModel: AuthViewModel,
    postViewModel: PostViewModel
) {

    // NOTE: TẤT CẢ LOGIC HIỆN TẠI CỦA BẠN ĐỀU ĐƯỢC GIỮ NGUYÊN
    val uiState = viewModel.profileUiState
    var name by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) } // Ảnh mới người dùng chọn
    var initialAvatarUrl by remember { mutableStateOf<String?>(null) } // Ảnh cũ từ server
    var isLoading by remember { mutableStateOf(false) } // State để quản lý trạng thái loading

    LaunchedEffect(Unit) {
        viewModel.loadCurrentUserProfile()
    }

    LaunchedEffect(uiState) {
        if (uiState is ProfileUiState.Success) {
            val user = (uiState as ProfileUiState.Success).userModel
            if (name.isEmpty() && initialAvatarUrl == null) {
                name = user.name
                bio = user.bio ?: ""
                initialAvatarUrl = user.avatarUrl
            }
        }
    }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Chỉnh sửa hồ sơ", fontWeight = FontWeight.Medium) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Box(
                contentAlignment = Alignment.BottomEnd,
                modifier = Modifier.clickable { imagePickerLauncher.launch("image/*") }
            ) {
                AsyncImage(
                    model = imageUri ?: initialAvatarUrl,
                    placeholder = painterResource(id = R.drawable.profile_placeholder),
                    error = painterResource(id = R.drawable.profile_placeholder),
                    contentDescription = "Ảnh đại diện",
                    modifier = Modifier
                        .size(128.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainer),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Thay đổi ảnh",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Text(
                text = "Thay đổi ảnh đại diện",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Thông tin cá nhân",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên hiển thị") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Tiểu sử") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    isLoading = true
                    viewModel.updateUserProfile(name, bio, imageUri) { success, errorMessage ->
                        isLoading = false
                        scope.launch {
                            if (success) {
                                // --------------------------------------------------
                                // ✅ BƯỚC 3: KẾT NỐI VỚI HỆ THỐNG TOÀN CỤC KHI LƯU THÀNH CÔNG
                                // --------------------------------------------------
                                val userId = FirebaseAuth.getInstance().currentUser?.uid
                                if (userId != null) {
                                    // Ra lệnh cho PostViewModel xóa cache để các bài viết cũ cập nhật.
                                    postViewModel.refreshUserCache(userId)
                                    // Ra lệnh cho AuthViewModel tải lại dữ liệu user cho toàn ứng dụng.
                                    authViewModel.reloadCurrentUser()
                                }
                                // --------------------------------------------------

                                snackbarHostState.showSnackbar("Đã lưu thay đổi!")
                                navController.popBackStack()
                            } else {
                                snackbarHostState.showSnackbar("Lưu thất bại: $errorMessage")
                            }
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Lưu thay đổi", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// Preview này sẽ báo lỗi vì NavGraph giờ yêu cầu ViewModel.
// Bạn có thể tạm thời comment nó đi hoặc sửa lại nếu cần.
@Preview(showBackground = true, device = "id:pixel_7")
@Composable
fun EditProfileScreenPreview() {
    // Để chạy preview, cần cung cấp các ViewModel giả.
    // VD: EditProfileScreen(navController = rememberNavController(), authViewModel = ..., postViewModel = ...)
}
