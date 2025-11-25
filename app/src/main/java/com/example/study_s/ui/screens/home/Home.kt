package com.example.study_s.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState // <-- 2. IMPORT
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.example.study_s.ui.navigation.Routes
import com.example.study_s.ui.screens.components.BottomNavBar
import com.example.study_s.ui.screens.components.TopBar
import com.example.study_s.viewmodel.PostViewModel
import com.example.study_s.ui.screens.components.PostItem
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch // <-- 3. IMPORT
import kotlinx.coroutines.flow.collectLatest // <-- THÊM IMPORT NÀY// ...

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

    LaunchedEffect(Unit) {
        // Luôn lắng nghe các sự kiện yêu cầu cuộn lên đầu
        viewModel.scrollToTopEvent.collectLatest {
            scope.launch {
                lazyListState.animateScrollToItem(index = 0)
            }
        }
    }

    // Tải bài đăng lần đầu tiên khi màn hình được tạo (nếu danh sách đang rỗng)
    LaunchedEffect(Unit) {
        if (posts.isEmpty()) {
            viewModel.loadPosts()
        }
    }


    Scaffold(
        topBar = {
            TopBar(
                onNotificationClick = { navController.navigate(Routes.Notification) },
                onSearchClick = { navController.navigate(Routes.Search) },
                onChatClick = { navController.navigate(Routes.Message) }
            )
        },

        bottomBar = {
            BottomNavBar(navController = navController, currentRoute = currentRoute, onHomeIconReselected = {
                // Khi nhấn lại icon Home, gọi hàm trong ViewModel
                viewModel.reloadPostsAndScrollToTop()
            })
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

        }
    }
}



@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    val navController = rememberNavController()
    HomeScreen(navController = navController)
}
