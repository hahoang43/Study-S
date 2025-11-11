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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import android.widget.Toast
import kotlinx.coroutines.launch // <-- 3. IMPORT
import android.util.Log // <-- THÊM IMPORT NÀY
// Hàm downloadFile (Giữ nguyên, không thay đổi)
fun downloadFile(context: Context, url: String, fileName: String) {
    // Log URL để kiểm tra xem nó là gs:// hay https://
    Log.d("DownloadDebug", "Đang thử tải URL: $url")

    try {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(fileName)
            .setDescription("Đang tải xuống...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        downloadManager.enqueue(request)
        Toast.makeText(context, "Bắt đầu tải xuống $fileName", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        // ✅ THÊM DÒNG NÀY ĐỂ XEM LỖI TRONG LOGCAT
        Log.e("DownloadDebug", "Lỗi DownloadManager: ${e.message}", e)

        Toast.makeText(context, "Lỗi tải xuống: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

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
                .background(Color(0xFFF0F2F5)),
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
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
                    .background(Color.LightGray),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            // Chữ mờ
            Text("Bạn đang nghĩ gì?", color = Color.Gray, fontSize = 16.sp)
            Spacer(modifier = Modifier.weight(1f))
            // Icon ảnh cho trực quan
            Icon(
                imageVector = Icons.Default.PhotoLibrary,
                contentDescription = "Thêm ảnh",
                tint = Color(0xFF4CAF50) // Màu xanh lá
            )
        }
    }
}


@Composable
fun PostItem(navController: NavController, post: PostModel, viewModel: PostViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val userCache by viewModel.userCache.collectAsState()
    val author = userCache[post.authorId] ?: User(name = "Đang tải...")
    // ✅ Lấy thông tin người đăng bài từ Firestore
    LaunchedEffect(post.authorId) {
        if (post.authorId.isNotBlank()) {
            viewModel.fetchUser(post.authorId)
        }
    }
    val authorName = author.name
    val authorAvatar = author.avatarUrl
    // 3. Lấy thông tin like
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val isLiked = remember(post.likedBy) { // Re-check khi post.likedBy thay đổi
        currentUserId?.let { post.likedBy.contains(it) } ?: false
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { // 4. Click cả card để vào chi tiết
                navController.navigate("${Routes.PostDetail}/${post.postId}")
            },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ){
        Column(modifier = Modifier.padding(12.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = rememberAsyncImagePainter(authorAvatar ?: "https://i.pravatar.cc/150?img=5"),
                    contentDescription = "Avatar của ${authorName }",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = authorName ,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    val formattedDate = post.timestamp?.toDate()?.let {
                        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(it)
                    } ?: "Không rõ thời gian"
                    Text(text = formattedDate, fontSize = 12.sp, color = Color.Gray)
                }
                IconButton(onClick = { /* TODO: Menu */ }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Tùy chọn")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Nội dung bài viết
            if (post.content.isNotBlank()) {
                Text(
                    text = post.content,
                    style = MaterialTheme.typography.bodyLarge,
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // ✅ Hiển thị file hoặc ảnh đính kèm (Giữ nguyên)
            if (post.imageUrl != null) {
                val isImage = post.fileName?.let {
                    it.endsWith(".jpg", true) || it.endsWith(".jpeg", true) || it.endsWith(".png", true)
                } ?: true

                if (isImage) {
                    // Ảnh
                    Image(
                        painter = rememberAsyncImagePainter(post.imageUrl),
                        contentDescription = "Ảnh đính kèm",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Tệp
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Icon xem tệp
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                .clickable {
                                    val encodedUrl = Uri.encode(post.imageUrl)
                                    val encodedName = Uri.encode(post.fileName ?: "Tệp đính kèm")
// Dòng mới
                                    navController.navigate("${Routes.FilePreview}?fileUrl=$encodedUrl&fileName=$encodedName")
                                           },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Description,
                                contentDescription = "File Icon",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = post.fileName ?: "Tệp đính kèm",
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Nhấn biểu tượng để xem hoặc tải xuống",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Nút tải xuống
                        IconButton(onClick = {
                            downloadFile(context, post.imageUrl, post.fileName ?: "downloaded_file")
                        }) {
                            Icon(
                                imageVector = Icons.Filled.AttachFile,
                                contentDescription = "Tải xuống",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Footer (Giữ nguyên)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 5. Nút Like được cập nhật
                IconButton(onClick = {
                    viewModel.toggleLike(post.postId)
                }) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Thích",
                        tint = if (isLiked) Color.Red else Color.Gray
                    )
                }
                Text(text = "${post.likesCount}") // Tự động cập nhật
                Spacer(modifier = Modifier.width(16.dp))
                // 6. Nút bình luận (click vào sẽ đi đến chi tiết)
                IconButton(onClick = {
                    navController.navigate("${Routes.PostDetail}/${post.postId}")
                }) {
                    Icon(Icons.Default.Send, contentDescription = "Bình luận")
                }
                Text(text = "${post.commentsCount}") // Tự động cập nhật
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