package com.example.study_s.ui.screens.profiles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.study_s.ui.navigation.Routes
import com.example.study_s.ui.screens.components.BottomNavBar
import com.example.study_s.ui.screens.components.TopBar
import androidx.lifecycle.viewmodel.compose.viewModel // Thêm import này
import com.example.study_s.data.model.User
import com.example.study_s.viewmodel.ProfileViewModel
import com.example.study_s.viewmodel.ProfileUiState
import java.util.Date
import com.example.study_s.viewmodel.ProfileViewModelFactory
// Dán khối code này vào phần import ở đầu file ProfileScreen.kt
import com.example.study_s.viewmodel.ProfileActionState
import com.example.study_s.viewmodel.AuthEvent
import com.example.study_s.viewmodel.AuthViewModel
import com.example.study_s.viewmodel.AuthViewModelFactory

@Composable

fun ProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = viewModel(factory = ProfileViewModelFactory()),
            authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory())


) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val uiState = viewModel.profileUiState
    // [THÊM] Tải dữ liệu người dùng khi màn hình được hiển thị
    LaunchedEffect(Unit) {
        viewModel.loadCurrentUserProfile()
    }
// NOTE 2: THÊM KHỐI CODE NÀY ĐỂ LẮNG NGHE SỰ KIỆN ĐĂNG XUẤT
    LaunchedEffect(Unit) {
        authViewModel.event.collect { event ->
            when (event) {
                is AuthEvent.OnSignOut -> {
                    // Điều hướng về màn hình Login và xóa hết các màn hình trước đó
                    navController.navigate(Routes.Login) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopBar(
                onNavIconClick = { /* TODO: open drawer */ },
                onNotificationClick = { navController.navigate(Routes.Notification) },
                onSearchClick = { navController.navigate(Routes.Search) }
            )
        },
        bottomBar = {
            // SỬA Ở ĐÂY: Gọi BottomNavBar có sẵn và truyền NavController, route hiện tại
            BottomNavBar(navController = navController, currentRoute = currentRoute)
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            // Dùng when để quyết định hiển thị gì dựa trên trạng thái (uiState)
            when (val uiState = viewModel.profileUiState) { // Lấy state từ ViewModel
                is ProfileUiState.Loading -> {
                    // Nếu đang tải -> chỉ hiển thị vòng quay
                    CircularProgressIndicator()
                }

                is ProfileUiState.Error -> {
                    // Nếu có lỗi -> chỉ hiển thị thông báo lỗi
                    Text(text = uiState.message, color = MaterialTheme.colorScheme.error)
                }

                is ProfileUiState.Success -> {
                    ProfileContent(
                        navController = navController,
                        user = uiState.user,
                        profileViewModel = viewModel,
                        onSignOutClick = { authViewModel.signOut() })
                }
            }
        }
    }
}
@Composable
private fun ProfileContent(
    navController: NavController,
    user: User,
    profileViewModel: ProfileViewModel,
    onSignOutClick: () -> Unit
) {
    val actionState = profileViewModel.actionState
    val context = LocalContext.current

    // State để điều khiển việc hiển thị dialog
    var showChangePasswordDialog by remember { mutableStateOf(false) }

    // LaunchedEffect để hiển thị Toast và xử lý sau khi có kết quả
    LaunchedEffect(actionState) {
        when (actionState) {
            is ProfileActionState.Success -> {
                // Khi thành công, hiển thị thông báo và đóng dialog
                android.widget.Toast.makeText(context, actionState.message, android.widget.Toast.LENGTH_LONG).show()
                showChangePasswordDialog = false
                profileViewModel.resetActionState()
            }
            is ProfileActionState.Failure -> {
                // Khi thất bại, chỉ hiển thị thông báo, không đóng dialog
                android.widget.Toast.makeText(context, actionState.message, android.widget.Toast.LENGTH_LONG).show()
                profileViewModel.resetActionState()
            }
            else -> { /* Không làm gì với Idle và Loading */ }
        }
    }

    // Nếu showChangePasswordDialog là true, hiển thị Dialog
    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            onDismissRequest = {
                // Chỉ cho phép đóng khi không đang loading
                if (actionState !is ProfileActionState.Loading) {
                    showChangePasswordDialog = false
                }
            },
            onConfirmClick = { old, new, confirm ->
                // Gọi hàm mới trong ViewModel
                profileViewModel.changePassword(old, new)
            },
            isLoading = actionState is ProfileActionState.Loading
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ===== Header + Avatar =====
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(Color(0xFFA3D6E0)),
            contentAlignment = Alignment.BottomCenter
        ) {
            AsyncImage(
                model = user.avatarUrl.takeIf { !it.isNullOrEmpty() }
                    ?: "https://i.imgur.com/8p3xYso.png",
                contentDescription = "Avatar",
                modifier = Modifier
                    .size(90.dp)
                    .offset(y = 45.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(50.dp))

        // ===== Name, Email + Edit Icon =====
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = user.name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp
            )
            IconButton(
                onClick = { navController.navigate(Routes.EditProfile) },
                modifier = Modifier
                    .size(24.dp)
                    .padding(start = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Profile",
                    tint = Color.Gray
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = user.email,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Nút "Các bài viết"
        Button(
            onClick = { /* TODO */ },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            shape = RoundedCornerShape(50)
        ) {
            Text("Các bài viết")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Nút "Đổi mật khẩu" (ĐÃ SỬA)
        // Nút này bây giờ sẽ mở Dialog thay vì gọi thẳng ViewModel
        Button(
            onClick = {
                showChangePasswordDialog = true // <--- SỬA CHỖ NÀY
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C757D))
        ) {
            Text("Đổi mật khẩu") // <--- SỬA CHỖ NÀY, BỎ VÒNG XOAY ĐI
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Nút "Đăng xuất"
        Button(
            onClick = onSignOutClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Đăng xuất")
        }
    }
}
@Composable
fun ChangePasswordDialog(
    onDismissRequest: () -> Unit,
    onConfirmClick: (String, String, String) -> Unit,
    isLoading: Boolean
) {
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) }


    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Đổi mật khẩu") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = oldPassword,
                    onValueChange = { oldPassword = it },
                    label = { Text("Mật khẩu cũ") },
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password)
                )

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = {
                        newPassword = it
                        // Xóa lỗi ngay khi người dùng sửa
                        if (it.length >= 6) passwordError = null
                    },
                    label = { Text("Mật khẩu mới (ít nhất 6 ký tự)") },
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password),
                    isError = passwordError != null
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        // Xóa lỗi ngay khi người dùng sửa
                        if (it == newPassword) passwordError = null
                    },
                    label = { Text("Xác nhận mật khẩu mới") },
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password),
                    isError = passwordError != null
                )

                // Hiển thị thông báo lỗi nếu có
                passwordError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Kiểm tra lỗi trước khi nhấn
                    if (newPassword.length < 6) {
                        passwordError = "Mật khẩu mới quá ngắn."
                    } else if (newPassword != confirmPassword) {
                        passwordError = "Mật khẩu xác nhận không khớp."
                    } else {
                        passwordError = null
                        onConfirmClick(oldPassword, newPassword, confirmPassword)
                    }
                },
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Xác nhận")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Hủy")
            }
        }
    )
}
            @Preview(showBackground = true, showSystemUi = true)
    @Composable
    fun ProfileScreenPreview() {
        // Tạo một đối tượng User giả để xem trước
        val fakeUser = User(
            userId = "fakeId",
            name = "Ngo Ich (Preview)",
            email = "preview@vnu.edu.vn",
            avatarUrl = null,
            bio = "Đây là bio xem trước",
            createdAt = Date()
        )
        // Gọi thẳng ProfileContent để xem trước giao diện khi đã có dữ liệu
        //ProfileContent(navController = rememberNavController(), user = fakeUser,profileViewModel ={},onSignOutClick = {})
    }

