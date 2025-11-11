package com.example.study_s.ui.screens.post

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.study_s.data.model.PostModel
import com.example.study_s.data.model.User
import com.example.study_s.viewmodel.PostViewModel
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPostsScreen(
    navController: NavController,
    viewModel: PostViewModel = viewModel()
) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    var myPosts by remember { mutableStateOf<List<PostModel>>(emptyList()) }

    // Tải danh sách bài viết của chính mình
    LaunchedEffect(currentUserId) {
        if (currentUserId != null) {
            viewModel.loadPosts()
            viewModel.posts.collect { list ->
                myPosts = list.filter { it.authorId == currentUserId }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bài viết của tôi", fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        if (myPosts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Bạn chưa có bài viết nào.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .background(Color(0xFFF8F9FA)),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(myPosts) { post ->
                    MyPostCard(post, viewModel)
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun MyPostCard(post: PostModel, viewModel: PostViewModel) {
    val userCache by viewModel.userCache.collectAsState()
    val author = userCache[post.authorId] ?: User(name = "Bạn")

    // Lấy thời gian hiển thị đẹp
    val formattedDate = post.timestamp?.toDate()?.let {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(it)
    } ?: "Vừa xong"

    // Kiểm tra đã like chưa
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val isLiked = remember(post.likedBy) { currentUserId?.let { post.likedBy.contains(it) } ?: false }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Avatar + tên + thời gian
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = author.avatarUrl ?: "https://i.pravatar.cc/150?img=5",
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(author.name ?: "Bạn", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(formattedDate, fontSize = 12.sp, color = Color.Gray)
                }
            }

            Spacer(Modifier.height(8.dp))

            // Nội dung
            Text(post.content, fontSize = 16.sp)

            Spacer(Modifier.height(8.dp))

            // Hình ảnh (nếu có)
            post.imageUrl?.let {
                AsyncImage(
                    model = it,
                    contentDescription = "Ảnh bài viết",
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 250.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(Modifier.height(12.dp))
            Divider(color = Color(0xFFE0E0E0))
            Spacer(Modifier.height(8.dp))

// Like - Comment - Share (Facebook style)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ❤️ LIKE
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Thích",
                        tint = if (isLiked) Color.Red else Color.Gray
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("${post.likesCount}", fontWeight = FontWeight.Medium)
                }

                // 💬 COMMENT
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = "Bình luận",
                        tint = Color.Gray
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("${post.commentsCount}", fontWeight = FontWeight.Medium)
                }

                // 🔁 SHARE
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Chia sẻ",
                        tint = Color.Gray
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("0", fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
