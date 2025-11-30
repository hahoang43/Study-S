
package com.example.study_s.ui.screens.post

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.study_s.R
import com.example.study_s.data.model.PostModel
import com.example.study_s.data.model.UserModel
import com.example.study_s.ui.navigation.Routes
import com.example.study_s.viewmodel.PostViewModel
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPostsScreen(
    navController: NavController,
    postViewModel: PostViewModel = viewModel() // Đổi tên cho nhất quán
) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    var myPosts by remember { mutableStateOf<List<PostModel>>(emptyList()) }

    LaunchedEffect(currentUserId) {
        if (currentUserId != null) {
            postViewModel.loadPosts()
            postViewModel.posts.collect { list ->
                myPosts = list
                    .filter { it.authorId == currentUserId }
                    .sortedByDescending { it.timestamp?.toDate()?.time ?: 0L }
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
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
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
                Text("Bạn chưa có bài viết nào.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface), // Đổi màu nền cho nhất quán
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(myPosts) { post ->
                    MyPostCard(
                        post = post,
                        viewModel = postViewModel,
                        navController = navController, // Truyền NavController để có thể click
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MyPostCard(
    post: PostModel,
    viewModel: PostViewModel,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val userCache by viewModel.userCache.collectAsState()
    // Dùng authorId để lấy UserModel mới nhất từ cache
    val author = userCache[post.authorId] ?: UserModel(name = "Đang tải...")

    // Yêu cầu ViewModel tìm nạp thông tin người dùng nếu chưa có trong cache
    LaunchedEffect(post.authorId) {
        if (post.authorId.isNotBlank()) {
            viewModel.fetchUser(post.authorId)
        }
    }

    val formattedDate = post.timestamp?.toDate()?.let {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(it)
    } ?: "Vừa xong"

    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val isLiked = remember(post.likedBy) { currentUserId?.let { post.likedBy.contains(it) } ?: false }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { navController.navigate("${Routes.PostDetail}/${post.postId}") }, // Thêm hành động click
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Avatar + tên + thời gian
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Sửa lỗi Avatar
                AsyncImage(
                    model = author.avatarUrl,
                    contentDescription = "Avatar",
                    modifier = Modifier.size(40.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = R.drawable.ic_profile),
                    error = painterResource(id = R.drawable.ic_profile)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    // Sửa lỗi Tên người dùng
                    Text(author.name ?: "Đang tải...", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(formattedDate, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Nội dung
            if (post.content.isNotBlank()) {
                Text(post.content, fontSize = 16.sp, lineHeight = 22.sp)
                Spacer(Modifier.height(12.dp))
            }

            // Hình ảnh (nếu có)
            post.imageUrl?.let {
                AsyncImage(
                    model = it,
                    contentDescription = "Ảnh bài viết",
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.height(12.dp))
            }

            // Like - Comment - Share
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // LIKE
                Row(
                    modifier = Modifier.clickable { viewModel.toggleLike(post.postId) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Thích",
                        tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(post.likesCount.toString(), fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // COMMENT
                Row(
                    modifier = Modifier.clickable { navController.navigate("${Routes.PostDetail}/${post.postId}") },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = "Bình luận",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(post.commentsCount.toString(), fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // SHARE (Ví dụ)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Chia sẻ",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MyPostsScreenPreview() {
    MyPostsScreen(navController = rememberNavController())
}
