// ĐƯỜNG DẪN: ui/screens/post/PostDetailScreen.kt
// NỘI DUNG HOÀN CHỈNH, ĐÃ SỬA LỖI VÀ THÊM CHỨC NĂNG SỬA/XÓA BÌNH LUẬN

package com.example.study_s.ui.screens.post

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.study_s.data.model.CommentModel
import com.example.study_s.ui.navigation.Routes
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
    LaunchedEffect(postId) {
        viewModel.selectPostAndLoadComments(postId)
    }

    val post by viewModel.selectedPost.collectAsState()

    // State để quản lý việc nhập/sửa bình luận
    var commentInput by remember { mutableStateOf("") }
    var commentToEdit by remember { mutableStateOf<CommentModel?>(null) }
    val comments by viewModel.comments.collectAsState()
    val lazyListState = rememberLazyListState()

    LaunchedEffect(comments.size) {
        // Chỉ cuộn khi danh sách không rỗng
        if (comments.isNotEmpty()) {
            // Lấy index của item cuối cùng
            val lastIndex = comments.size - 1 + 3 // 3 items cố định ở trên: Post, Divider, Title
            lazyListState.animateScrollToItem(index = lastIndex)
        }
    }

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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            // ✅ HIỂN THỊ GIAO DIỆN SỬA HOẶC THÊM MỚI TÙY VÀO STATE
            if (commentToEdit != null) {
                // Giao diện sửa bình luận
                EditCommentBar(
                    initialText = commentToEdit!!.content,
                    onConfirm = { newContent ->
                        viewModel.updateComment(commentToEdit!!.postId, commentToEdit!!.commentId, newContent)
                        commentToEdit = null // Xong, đóng giao diện sửa
                    },
                    onCancel = {
                        commentToEdit = null // Hủy sửa
                    }
                )
            } else {
                // Giao diện thêm bình luận mới
                BottomCommentBar(
                    commentText = commentInput,
                    onCommentChanged = { commentInput = it },
                    onSendClick = {
                        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                        if (currentUserId != null && commentInput.isNotBlank()) {
                            val newComment = CommentModel(
                                postId = postId,
                                authorId = currentUserId,
                                content = commentInput
                            )

                        viewModel.addComment(postId, newComment)
                        commentInput = ""
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
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
            item {
                Divider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            item {
                Text(
                    "Bình luận (${comments.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            items(comments) { comment ->
                CommentItem(
                    comment = comment,
                    viewModel = viewModel,
                    navController = navController,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    // ✅ Callback để mở giao diện sửa
                    onEditClick = { selectedComment ->
                        commentToEdit = selectedComment
                    }
                )
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
}

/* ======================= COMMENT ITEM (ĐÃ CẬP NHẬT) ======================= */
@Composable
fun CommentItem(
    comment: CommentModel,
    viewModel: PostViewModel,
    navController: NavController,
    modifier: Modifier = Modifier,
    onEditClick: (CommentModel) -> Unit // ✅ Thêm Callback để xử lý sự kiện sửa
) {
    val userCache by viewModel.userCache.collectAsState()
    val author = userCache[comment.authorId]
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val isMyComment = currentUserId != null && currentUserId == comment.authorId
    var showMenu by remember { mutableStateOf(false) }

    // Lấy thông tin người dùng nếu chưa có trong cache
    LaunchedEffect(comment.authorId) {
        if (author == null && comment.authorId.isNotBlank()) {
            viewModel.fetchUser(comment.authorId)
        }
    }

    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Image(
            painter = rememberAsyncImagePainter(
                model = author?.avatarUrl ?: "https://i.pravatar.cc/150?img=3"
            ),
            contentDescription = "Avatar",
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable {
                    if (isMyComment) {
                        navController.navigate(Routes.Profile)
                    } else {
                        navController.navigate("${Routes.OtherProfile}/${comment.authorId}")
                    }
                },
            contentScale = ContentScale.Crop
        )

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            // Tên và ngày tháng
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = author?.name ?: "Đang tải...",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                val formattedDate = comment.timestamp?.toDate()?.let {
                    SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault()).format(it)
                } ?: ""
                Text(
                    text = formattedDate,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(2.dp))
            // Nội dung bình luận
            Text(
                text = comment.content,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
        }

        // ✅ CHỈ HIỂN THỊ MENU NẾU LÀ TÁC GIẢ BÌNH LUẬN
        if (isMyComment) {
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Tùy chọn bình luận")
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Sửa bình luận") },
                        onClick = {
                            onEditClick(comment) // Gọi callback để mở giao diện sửa
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = "Sửa")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Xóa bình luận") },
                        onClick = {
                            viewModel.deleteComment(comment.postId, comment.commentId)
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "Xóa",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    )
                }
            }
        }
    }
}

/* ======================= BOTTOM COMMENT BAR (Thêm mới/Sửa) ======================= */
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

// ✅ GIAO DIỆN SỬA BÌNH LUẬN
@Composable
fun EditCommentBar(
    initialText: String,
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit
) {
    var text by remember(initialText) { mutableStateOf(initialText) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                label = { Text("Sửa bình luận...") },
                shape = RoundedCornerShape(24.dp)
            )
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Close, contentDescription = "Hủy")
            }
            IconButton(onClick = { onConfirm(text) }, enabled = text.isNotBlank()) {
                Icon(Icons.Default.Check, contentDescription = "Lưu")
            }
        }
    }
}
