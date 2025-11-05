// File: com/example/study_s/ui/screens/profiles/StrangerScreen.kt
package com.example.study_s.ui.screens.profiles

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.study_s.R
import com.example.study_s.data.model.User
import com.example.study_s.viewmodel.ProfileUiState
import com.example.study_s.viewmodel.ProfileViewModel
import java.util.*
import com.example.study_s.viewmodel.ProfileViewModelFactory
// =========================================================================
// HÀM 1: MÀN HÌNH CHÍNH
// =========================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StragerScreen(
    navController: NavController,
    userId: String, // Nhận userId từ NavHost
    viewModel: ProfileViewModel = viewModel(factory = ProfileViewModelFactory())
) {
    // Tải hồ sơ người lạ khi Composable được khởi tạo hoặc khi userId thay đổi.
    LaunchedEffect(userId) {
        viewModel.loadStragerProfile(userId)
    }

    val uiState = viewModel.stragerProfileUiState

    Scaffold(
        topBar = {
            TopAppBar(
                title = { /* Để trống hoặc hiển thị tên người dùng khi đã tải xong */ },
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
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            when (uiState) {
                is ProfileUiState.Loading -> {
                    CircularProgressIndicator()
                }
                is ProfileUiState.Error -> {
                    Text(
                        text = uiState.message,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                is ProfileUiState.Success -> {
                    // Khi thành công, hiển thị giao diện chi tiết
                    StragerContent(user = uiState.user)
                }
            }
        }
    }
}

// =========================================================================
// HÀM 2: GIAO DIỆN NỘI DUNG
// =========================================================================
@Composable
private fun StragerContent(user: User) {
    // Dùng LazyColumn để toàn bộ màn hình có thể cuộn
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // PHẦN HEADER VÀ THÔNG TIN CÁ NHÂN
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(Color(0xFFB3E5FC)) // Màu nền header
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-50).dp), // Đẩy lên để chồng lên header
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AsyncImage(
                    model = user.avatarUrl ?: R.drawable.profile_placeholder,
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .border(3.dp, Color.White, CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(user.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { /* TODO: Logic theo dõi */ }) {
                        Text("Theo dõi")
                    }
                    Button(onClick = { /* TODO: Logic nhắn tin */ }) {
                        Text("Nhắn tin")
                    }
                }
            }
        }

        // PHẦN DANH SÁCH BÀI VIẾT
        item {
            Text(
                "Bài viết",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, bottom = 16.dp)
            )
        }

        // TODO: Thay thế danh sách giả này bằng dữ liệu bài viết thật
        items(listOf("2 giờ trước", "1 ngày trước")) { time ->
            PostItem(time = time, user = user)
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

// =========================================================================
// HÀM 3: ITEM BÀI VIẾT
// =========================================================================
@Composable
private fun PostItem(time: String, user: User) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(Color(0xFFE3F2FD), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = user.avatarUrl ?: R.drawable.profile_placeholder,
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(user.name, fontWeight = FontWeight.Bold)
                Text(time, fontSize = 12.sp, color = Color.Gray)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text("Đây là nội dung của một bài viết mẫu.")
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.FavoriteBorder, contentDescription = "Thích")
                Spacer(modifier = Modifier.width(4.dp))
                Text("23")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Share, contentDescription = "Chia sẻ")
                Spacer(modifier = Modifier.width(4.dp))
                Text("10")
            }
        }
    }
}

// =========================================================================
// HÀM 4: PREVIEW
// =========================================================================
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun StragerScreenPreview() {
    val fakeUser = User(
        userId = "fakeUserId",
        name = "Nhật Long (Preview)",
        email = "nhatlong@example.com",
        avatarUrl = null,
        bio = "Đây là bio preview.",
        createdAt = Date()
    )
    MaterialTheme {
        // Gọi thẳng StrangerContent để xem giao diện khi đã có dữ liệu
        StragerContent(user = fakeUser)
    }
}
