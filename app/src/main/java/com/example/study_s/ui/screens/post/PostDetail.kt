// ĐƯỜNG DẪN: ui/screens/post/PostDetailScreen.kt
// NỘI DUNG HOÀN CHỈNH, ĐÃ SỬA LỖI

package com.example.study_s.ui.screens.post
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
import coil.compose.rememberAsyncImagePainter
import com.example.study_s.data.model.CommentModel
import com.example.study_s.data.model.PostModel
import com.example.study_s.data.model.User
import com.example.study_s.ui.navigation.Routes // DÒNG IMPORT QUAN TRỌNG
import com.example.study_s.ui.screens.components.PostItem
import com.example.study_s.viewmodel.PostViewModel
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    postId: String,
    viewModel: PostViewModel = viewModel(),
    navController: NavController
) {
    // Tải post + cmt
    LaunchedEffect(postId) {
        viewModel.selectPostAndLoadComments(postId)
    }

    val post by viewModel.selectedPost.collectAsState()
    val comments by viewModel.comments.collectAsState()
    var commentInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bài đăng", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại"
                        )
                    }
                },
                // Tự động đổi màu theo theme
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            BottomCommentBar(
                commentText = commentInput,
                onCommentChanged = { commentInput = it },
                onSendClick = {
                    // ✅ HÀM GỬI COMMENT VÀ THÔNG BÁO ĐƯỢC GỌI Ở ĐÂY
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
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // Phần post
            item {
                post?.let { postData ->
                    PostItem(
                        post = postData,
                        navController = navController,
                        viewModel = viewModel,
                        isClickable = false
                    )
                } ?: run {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            // Thêm đường kẻ phân cách để tách biệt bài đăng và bình luận
            item {
                Divider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            // Tiêu đề cmt
            item {
                Text(
                    "Bình luận (${comments.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    // SỬA: Dùng màu chữ từ theme
                    color = MaterialTheme.colorScheme.onSurface)
            }

            // List cmt
            items(comments) { comment ->
                CommentItem(
                    comment = comment,
                    viewModel = viewModel,
                    navController = navController,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
}



/* ======================= COMMENT ITEM ======================= */
@Composable
fun CommentItem(
    comment: CommentModel,
    viewModel: PostViewModel,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val userCache by viewModel.userCache.collectAsState()
    val author = userCache[comment.authorId] ?: User(name = "Đang tải...")
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val isMyComment = currentUserId != null && currentUserId == comment.authorId

    LaunchedEffect(comment.authorId) {
        if (comment.authorId.isNotBlank()) {
            viewModel.fetchUser(comment.authorId)
        }
    }

    Row(modifier = modifier.fillMaxWidth()) {
        Image(
            painter = rememberAsyncImagePainter(
                model = author.avatarUrl ?: "https://i.pravatar.cc/150?img=3"
            ),
            contentDescription = "Avatar",
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable {
                    if (isMyComment) {
                        navController.navigate(Routes.Profile)
                    } else {
                        // SỬA LẠI CHO ĐÚNG ROUTE
                        navController.navigate("${Routes.OtherProfile}/${comment.authorId}")
                    }
                },
            contentScale = ContentScale.Crop
        )

        Spacer(Modifier.width(12.dp))

        Column(
            modifier = Modifier
                // SỬA: Dùng màu nền từ theme thay vì màu cứng
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = author.name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                modifier = Modifier.clickable {
                    if (isMyComment) {
                        navController.navigate(Routes.Profile)
                    } else {
                        // SỬA LẠI CHO ĐÚNG ROUTE
                        navController.navigate("${Routes.OtherProfile}/${comment.authorId}")
                    }
                }
            )
            Spacer(Modifier.height(4.dp))
            Text(comment.content, fontSize = 15.sp)
        }
    }
}

/* ======================= BOTTOM COMMENT BAR ======================= */
@Composable
fun BottomCommentBar(
    commentText: String,
    onCommentChanged: (String) -> Unit,
    onSendClick: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxWidth(), shadowElevation = 8.dp) {
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
                    tint = if (commentText.isNotBlank())
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
