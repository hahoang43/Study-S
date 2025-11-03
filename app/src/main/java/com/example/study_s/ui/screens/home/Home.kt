package com.example.study_s.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.example.study_s.data.model.PostModel // SỬA: Import đúng data class Post
import com.example.study_s.ui.navigation.Routes
import com.example.study_s.ui.screens.components.BottomNavBar
import com.example.study_s.viewmodel.PostViewModel
import java.text.SimpleDateFormat
import java.util.Locale

// Lớp dữ liệu cho một mục trên BottomBar
data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: PostViewModel = viewModel() // SỬA: Inject ViewModel
) {
    // SỬA: Lấy danh sách bài viết từ ViewModel
    val posts by viewModel.posts.collectAsState()

    // SỬA: Tải bài viết khi màn hình được hiển thị lần đầu
    LaunchedEffect(Unit) {
        viewModel.loadPosts()
    }

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
            // SỬA: Hiển thị danh sách bài viết từ state
            items(posts) { post ->
                PostItem(post = post, modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp))
            }
        }
    }
}


@Composable
fun PostItem(post: PostModel, modifier: Modifier = Modifier) { // SỬA: Sử dụng data class Post từ model
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
                // TODO: Cần một cơ chế để lấy avatar của tác giả từ authorId
                Image(
                    painter = rememberAsyncImagePainter("https://i.pravatar.cc/150?img=5"), // Ảnh đại diện mẫu
                    contentDescription = "Avatar của ${post.authorId}",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    // TODO: Cần một cơ chế để lấy tên tác giả từ authorId
                    Text(text = "Tác giả: ${post.authorId}", fontWeight = FontWeight.Bold, fontSize = 16.sp)

                    // SỬA: Format lại timestamp để hiển thị
                    val formattedDate = post.timestamp?.toDate()?.let {
                        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(it)
                    } ?: "Không rõ thời gian"
                    Text(text = formattedDate, fontSize = 12.sp, color = Color.Gray)
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

            // SỬA: Hiển thị ảnh của bài viết nếu có
            if (post.imageUrl != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Image(
                    painter = rememberAsyncImagePainter(post.imageUrl),
                    contentDescription = "Ảnh bài viết",
                    modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Footer
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = { /* TODO: Xử lý like */ }) {
                    Icon(Icons.Default.FavoriteBorder, contentDescription = "Thích")
                }
                Text(text = "${post.likesCount}")
                Spacer(modifier = Modifier.width(16.dp))
                IconButton(onClick = { /* TODO: Xử lý comment */ }) {
                    Icon(Icons.Default.Send, contentDescription = "Bình luận")
                }
                Text(text = "${post.commentsCount}")
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
