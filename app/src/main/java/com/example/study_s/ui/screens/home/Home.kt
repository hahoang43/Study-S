package com.example.study_s.ui.screens.home

import android.os.Environment
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState // <-- 1. IMPORT
import androidx.compose.foundation.lazy.rememberLazyListState // <-- 2. IMPORT
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Bookmark // <-- 1. IMPORT MỚI
import androidx.compose.material.icons.filled.BookmarkBorder // <-- 2. IMPORT MỚI
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.example.study_s.data.model.PostModel
import com.example.study_s.data.model.User
import com.example.study_s.ui.navigation.Routes
import com.example.study_s.ui.screens.components.BottomNavBar
import com.example.study_s.ui.screens.components.TopBar
import com.example.study_s.viewmodel.PostViewModel
import com.example.study_s.ui.screens.components.PostItem
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.material.icons.filled.Favorite
import com.example.study_s.ui.screens.components.PostItem
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import android.widget.Toast
import kotlinx.coroutines.launch // <-- 3. IMPORT
import android.util.Log // <-- THÊM IMPORT NÀY
// Hàm downloadFile (Giữ nguyên, không thay đổi)

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: PostViewModel = viewModel()
) {
    val posts by viewModel.posts.collectAsState()
    val lazyListState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    LaunchedEffect(navBackStackEntry) {
        if (currentRoute == Routes.Home) {
            viewModel.loadPosts()
            scope.launch {
                lazyListState.animateScrollToItem(index = 0)
            }
        }
    }


    Scaffold(
        topBar = {
            TopBar(
                onNotificationClick = { navController.navigate(Routes.Notification) },
                onSearchClick = { navController.navigate(Routes.Search) }
            )
        },

        bottomBar = {
            BottomNavBar(navController = navController, currentRoute = currentRoute)
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            state = lazyListState,
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp) // <-- 4. Thêm khoảng cách giữa các item
        ) {
            // <-- 5. THÊM COMPONENT MỚI VÀO ĐẦU DANH SÁCH
            item {
                CreatePostTrigger(
                    navController = navController,
                    modifier = Modifier.padding(horizontal = 8.dp) // Thêm padding ngang
                )
            }

            // Danh sách bài đăng (giữ nguyên)
            items(posts) { post ->
                PostItem(
                    navController = navController,
                    post = post,
                    viewModel = viewModel,
                    modifier = Modifier.padding(horizontal = 8.dp) // Chỉ cần padding ngang
                )
            }
        }
    }
}

// <-- 6. COMPOSABLE MỚI CHO Ô TẠO BÀI ĐĂNG GỌN GÀNG
@Composable
fun CreatePostTrigger(navController: NavController, modifier: Modifier = Modifier) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    // Lấy avatar từ tài khoản Google (hoặc fallback)
    val avatarUrl = currentUser?.photoUrl
    val userName = currentUser?.displayName ?: "Người dùng"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { navController.navigate(Routes.NewPost) }, // Nhấn vào đây để đi đến trang tạo bài
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = rememberAsyncImagePainter(
                    model = avatarUrl,
                    // Ảnh dự phòng nếu không có avatar
                    fallback = rememberAsyncImagePainter("https://i.pravatar.cc/150?img=5")
                ),
                contentDescription = "Avatar của $userName",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            // Chữ mờ
            Text("Bạn đang nghĩ gì?", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
            Spacer(modifier = Modifier.weight(1f))
            // Icon ảnh cho trực quan
            Icon(
                imageVector = Icons.Default.PhotoLibrary,
                contentDescription = "Thêm ảnh",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}



@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    val navController = rememberNavController()
    HomeScreen(navController = navController)
}