package com.example.study_s.ui.screens.post

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.example.study_s.data.model.CommentModel
import com.example.study_s.data.model.PostModel
import com.example.study_s.data.model.User // <-- 1. IMPORT USER MODEL
import com.example.study_s.viewmodel.PostViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    postId: String,
    viewModel: PostViewModel = viewModel(),
    navController: NavController
) {

    LaunchedEffect(postId) {
        viewModel.selectPostAndLoadComments(postId)
    }

    val post by viewModel.selectedPost.collectAsState()
    val comments by viewModel.comments.collectAsState()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    var commentInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bài đăng", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Quay lại"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            BottomCommentBar(
                commentText = commentInput,
                onCommentChanged = { commentInput = it },
                onSendClick = {
                    viewModel.addComment(postId, commentInput)
                    commentInput = ""
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
        ) {
            // Phần chi tiết bài đăng
            item {
                post?.let { postData ->
                    PostDetailContent(
                        post = postData,
                        currentUserId = currentUserId,
                        onLikeToggle = { viewModel.toggleLike(postData.postId) },
                        viewModel = viewModel // <-- 2. TRUYỀN VIEWMODEL XUỐNG
                    )
                } ?: run {
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }

            // Phần tiêu đề bình luận
            item {
                Text(
                    "Bình luận (${comments.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            // Danh sách bình luận
            items(comments) { comment ->
                CommentItem(
                    comment = comment,
                    viewModel = viewModel, // <-- 3. TRUYỀN VIEWMODEL XUỐNG
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

// Composable riêng cho nội dung chi tiết bài đăng
@Composable
fun PostDetailContent(
    post: PostModel,
    currentUserId: String?,
    onLikeToggle: () -> Unit,
    viewModel: PostViewModel // <-- 4. NHẬN VIEWMODEL
) {
    val isLiked = remember(post.likedBy) {
        currentUserId?.let { post.likedBy.contains(it) } ?: false
    }

    // --- 5. LẤY THÔNG TIN TÁC GIẢ TỪ CACHE ---
    val userCache by viewModel.userCache.collectAsState()
    val author = userCache[post.authorId] ?: User(name = "Đang tải...") // Dùng User model

    // Trigger fetch nếu chưa có trong cache
    LaunchedEffect(post.authorId) {
        if (post.authorId.isNotBlank()) {
            viewModel.fetchUser(post.authorId)
        }
    }
    // --- HẾT PHẦN LẤY THÔNG TIN TÁC GIẢ ---

    Column(modifier = Modifier
        .fillMaxWidth()
        .background(Color.White)
        .padding(horizontal = 16.dp, vertical = 12.dp)) {

        // --- 6. HIỂN THỊ THÔNG TIN TÁC GIẢ ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = rememberAsyncImagePainter(
                    model = author.avatarUrl, // Dùng author.avatarUrl
                    fallback = rememberAsyncImagePainter("https://i.pravatar.cc/150?img=5")
                ),
                contentDescription = "Avatar của ${author.name}", // Dùng author.name
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = author.name, // Dùng author.name
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                post.timestamp?.toDate()?.let {
                    val formattedDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(it)
                    Text(text = formattedDate, fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        // --- HẾT PHẦN HIỂN THỊ TÁC GIẢ ---

        Text(post.content, fontSize = 20.sp)
        Spacer(Modifier.height(8.dp))
        // Đã chuyển phần timestamp lên trên cùng tác giả
//        post.timestamp?.toDate()?.let { ... }

        Spacer(Modifier.height(16.dp))

        // ... (Phần hiển thị ảnh/file giữ nguyên) ...
        if (post.imageUrl != null && (post.fileName == null || post.fileName.endsWith(".png") || post.fileName.endsWith(".jpg"))) {
            Image(
                painter = rememberAsyncImagePainter(post.imageUrl),
                contentDescription = "Ảnh đính kèm",
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.height(16.dp))
        }

        // TODO: Hiển thị tệp (nếu là tệp, tương tự Home.kt)

        // Hàng Like/Comment
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onLikeToggle) {
                Icon(
                    imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Thích",
                    tint = if (isLiked) Color.Red else Color.Gray
                )
            }
            Text("${post.likesCount}", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(24.dp))
            Icon(Icons.Default.Send, contentDescription = "Bình luận", tint = Color.Gray)
            Spacer(Modifier.width(8.dp))
            Text("${post.commentsCount}", fontWeight = FontWeight.SemiBold)
        }

        Divider(modifier = Modifier.padding(vertical = 12.dp))
    }
}

// ... (BottomCommentBar giữ nguyên, không cần sửa) ...
@Composable
fun BottomCommentBar(
    commentText: String,
    onCommentChanged: (String) -> Unit,
    onSendClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = commentText,
                onValueChange = onCommentChanged,
                placeholder = { Text("Viết bình luận...") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onSendClick, enabled = commentText.isNotBlank()) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Gửi bình luận",
                    tint = if (commentText.isNotBlank()) MaterialTheme.colorScheme.primary else Color.Gray
                )
            }
        }
    }
}


// Composable riêng cho 1 item bình luận
@Composable
fun CommentItem(
    comment: CommentModel,
    viewModel: PostViewModel, // <-- 7. NHẬN VIEWMODEL
    modifier: Modifier = Modifier
) {

    // --- 8. LẤY THÔNG TIN NGƯỜI BÌNH LUẬN TỪ CACHE ---
    // XÓA BỎ LaunchedEffect cũ
    val userCache by viewModel.userCache.collectAsState()
    val author = userCache[comment.authorId] ?: User(name = "Đang tải...") // Dùng User model

    // Trigger fetch
    LaunchedEffect(comment.authorId) {
        if (comment.authorId.isNotBlank()) {
            viewModel.fetchUser(comment.authorId)
        }
    }
    // --- HẾT PHẦN LẤY THÔNG TIN ---

    Row(modifier = modifier.fillMaxWidth()) {
        Image(
            painter = rememberAsyncImagePainter(
                model = author.avatarUrl, // <-- 9. DÙNG author.avatarUrl
                fallback = rememberAsyncImagePainter("https://i.pravatar.cc/150?img=3")
            ),
            contentDescription = "Avatar",
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.width(12.dp))
        Column(
            modifier = Modifier
                .background(Color(0xFFEEEEEE), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = author.name, // <-- 10. DÙNG author.name
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(comment.content, fontSize = 15.sp)
        }
    }
}


@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewPostDetailScreen() {
    val navController = rememberNavController()
    PostDetailScreen(postId = "1", navController = navController)
}