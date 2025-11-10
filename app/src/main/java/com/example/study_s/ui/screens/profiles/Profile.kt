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
    private fun ProfileContent(navController: NavController,
                               user: User, // NOTE 3.1: THÊM HÀNH ĐỘNG onSignOut VÀO THAM SỐ
                               profileViewModel: ProfileViewModel,
                               onSignOutClick: () -> Unit) {
        val actionState = profileViewModel.actionState
        val context = LocalContext.current

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
                    // SỬA: Lấy ảnh từ `user.avatarUrl`
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

            Spacer(modifier = Modifier.height(50.dp)) // Khoảng trống cho Avatar

            // ===== Name, Email + Edit Icon =====
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    // SỬA: Lấy tên từ `user.name`
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
                // SỬA: Lấy email từ `user.email`
                text = user.email,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Nút "Các bài viết"
            Button(
                onClick = { /* TODO */ },
                // Modifier này sẽ là chuẩn cho tất cả các nút
                modifier = Modifier
                    .fillMaxWidth() // <-- Kéo dài ra toàn bộ chiều rộng
                    .padding(horizontal = 32.dp), // <-- Canh lề hai bên
                shape = RoundedCornerShape(50) // <-- Bo tròn thành hình viên thuốc
            ) {
                Text("Các bài viết")
            }

            Spacer(modifier = Modifier.height(16.dp))

// Nút "Đổi mật khẩu"
            Button(
                onClick = { profileViewModel.onResetPasswordClick() },
                // ÁP DỤNG CHÍNH XÁC MODIFIER VÀ SHAPE TỪ NÚT TRÊN
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C757D)),
                enabled = actionState !is ProfileActionState.Loading
            ) {
                if (actionState is ProfileActionState.Loading) {
                    // Khi loading, nút vẫn giữ nguyên kích thước vì đã có .fillMaxWidth()
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Đổi mật khẩu")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

// Nút "Đăng xuất"
            Button(
                onClick = onSignOutClick,
                // ÁP DỤNG CHÍNH XÁC MODIFIER VÀ SHAPE TỪ NÚT TRÊN
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

