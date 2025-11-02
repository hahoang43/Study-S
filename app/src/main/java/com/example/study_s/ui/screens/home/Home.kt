package com.example.study_s.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.example.study_s.ui.navigation.Routes
import com.example.study_s.ui.screens.components.BottomNavBar
import com.example.study_s.ui.screens.post.NewPostScreen



// Lớp dữ liệu mẫu cho một bài viết.
data class Post(
    val id: String,
    val authorName: String,
    val authorAvatarUrl: String,
    val content: String,
    val timestamp: String
)

// Lớp dữ liệu cho một mục trên BottomBar
data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

@Composable
fun HomeScreen(navController: NavController) {
    // Dữ liệu mẫu để hiển thị.
    val samplePosts = listOf(
        Post("1", "An Nguyen", "https://i.pravatar.cc/150?img=1", "Chào mọi người, hôm nay trời đẹp quá! Có ai muốn đi học nhóm không?", "5 phút trước"),
        Post("2", "Binh Tran", "https://i.pravatar.cc/150?img=2", "Mình vừa tìm được một tài liệu ôn thi cuối kỳ rất hay, sẽ chia sẻ cho cả nhóm nhé.", "1 giờ trước"),
        Post("3", "Cam Le", "https://i.pravatar.cc/150?img=3", "Sắp tới trường mình có tổ chức cuộc thi lập trình, mọi người cùng tham gia cho vui!", "3 giờ trước"),
        Post("4", "Dung Pham", "https://i.pravatar.cc/150?img=4", "Đã ai làm xong bài tập lớn môn Cấu trúc dữ liệu chưa? Cho mình hỏi chút với.", "Hôm qua")
    )

    // Lấy thông tin về route hiện tại
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    navController.navigate(Routes.NewPost)
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Tạo bài viết mới", tint = Color.White)
            }
        },
        bottomBar = {
            // SỬA Ở ĐÂY: Gọi BottomNavBar có sẵn và truyền NavController, route hiện tại
            BottomNavBar(navController = navController, currentRoute = currentRoute)
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color(0xFFF0F2F5)),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(samplePosts) { post ->
                PostItem(post = post, modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp))
            }
        }
    }
}


@Composable
fun PostItem(post: Post, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = rememberAsyncImagePainter(post.authorAvatarUrl),
                    contentDescription = "Avatar của ${post.authorName}",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = post.authorName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(text = post.timestamp, fontSize = 12.sp, color = Color.Gray)
                }
                IconButton(onClick = { /* TODO: Xử lý menu thêm */ }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Tùy chọn")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Content
            Text(
                text = post.content,
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Footer
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = { /* TODO: Xử lý like */ }) {
                    Icon(Icons.Default.FavoriteBorder, contentDescription = "Thích")
                }
                Spacer(modifier = Modifier.width(16.dp))
                IconButton(onClick = { /* TODO: Xử lý comment */ }) {
                    Icon(Icons.Default.Send, contentDescription = "Bình luận")
                }
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    val navController = rememberNavController()
    HomeScreen(navController = navController)
}
