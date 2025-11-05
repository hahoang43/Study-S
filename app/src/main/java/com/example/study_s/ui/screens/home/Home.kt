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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
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
import com.example.study_s.ui.navigation.Routes
import com.example.study_s.ui.screens.components.BottomNavBar
import com.example.study_s.ui.screens.components.TopBar
import com.example.study_s.viewmodel.PostViewModel
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import android.widget.Toast

// ✅ Hàm tải file xuống bằng DownloadManager
private fun downloadFile(context: Context, url: String, fileName: String) {
    try {
        val downloadManager = context.getSystemService(DownloadManager::class.java)
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(fileName)
            .setDescription("Đang tải xuống...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        // ✅ Sử dụng URI của thư mục Downloads thay vì đường dẫn tuyệt đối
        val destinationUri = Uri.parse("file://" +
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.absolutePath +
                "/$fileName"
        )
        request.setDestinationUri(destinationUri)

        downloadManager.enqueue(request)
        Toast.makeText(context, "Bắt đầu tải xuống...", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Không thể tải xuống: ${e.message}", Toast.LENGTH_LONG).show()
        e.printStackTrace()
    }
}


@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: PostViewModel = viewModel()
) {
    val posts by viewModel.posts.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadPosts()
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        topBar = {
            TopBar(
                onNavIconClick = { },
                onNotificationClick = { navController.navigate(Routes.Notification) },
                onSearchClick = { navController.navigate(Routes.Search) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Routes.NewPost) },
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
            items(posts) { post ->
                PostItem(navController = navController, post = post, modifier = Modifier.padding(8.dp))
            }
        }
    }
}

@Composable
fun PostItem(navController: NavController, post: PostModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var authorName by remember { mutableStateOf<String?>(null) }
    var authorAvatar by remember { mutableStateOf<String?>(null) }

    // ✅ Lấy thông tin người đăng bài từ Firestore
    LaunchedEffect(post.authorId) {
        if (post.authorId.isNotBlank()) {
            FirebaseFirestore.getInstance().collection("users").document(post.authorId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        authorName = document.getString("username")
                        authorAvatar = document.getString("avatarUrl")
                    } else {
                        authorName = "Người dùng ẩn danh"
                    }
                }
                .addOnFailureListener {
                    authorName = "Người dùng ẩn danh"
                }
        }
    }

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
                    painter = rememberAsyncImagePainter(authorAvatar ?: "https://i.pravatar.cc/150?img=5"),
                    contentDescription = "Avatar của ${authorName ?: post.authorId}",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = authorName ?: "Đang tải...",
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

            // ✅ Hiển thị file hoặc ảnh đính kèm
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
                                    navController.navigate("preview?fileUrl=$encodedUrl&fileName=$encodedName")
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
