

package com.example.study_s.ui.screens.post

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.study_s.R
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
    navController: NavController,
    postViewModel: PostViewModel = viewModel() // Đổi tên cho nhất quán
) {
    val post by postViewModel.selectedPost.collectAsState()
    val comments by postViewModel.comments.collectAsState()
    val lazyListState = rememberLazyListState()

    var commentInput by remember { mutableStateOf("") }
    var commentToEdit by remember { mutableStateOf<CommentModel?>(null) }

    LaunchedEffect(postId) {
        postViewModel.selectPostAndLoadComments(postId)
    }

    LaunchedEffect(comments.size) {
        if (comments.isNotEmpty()) {
            val lastIndex = lazyListState.layoutInfo.totalItemsCount - 1
            if (lastIndex >= 0) {
                lazyListState.animateScrollToItem(index = lastIndex)
            }
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
                }
            )
        },
        bottomBar = {
            if (commentToEdit != null) {
                EditCommentBar(
                    initialText = commentToEdit!!.content,
                    onConfirm = { newContent ->
                        postViewModel.updateComment(commentToEdit!!.postId, commentToEdit!!.commentId, newContent)
                        commentToEdit = null
                    },
                    onCancel = {
                        commentToEdit = null
                    }
                )
            } else {
                // ✅ BƯỚC 1: SỬA LẠI LOGIC GỬI BÌNH LUẬN
                BottomCommentBar(
                    commentText = commentInput,
                    onCommentChanged = { commentInput = it },
                    onSendClick = {
                        val content = commentInput.trim()
                        if (content.isNotBlank()) {
                            // Chỉ truyền postId và content, ViewModel sẽ tự xử lý phần còn lại
                            postViewModel.addComment(postId, content)
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
        ) {
            item {
                post?.let { postData ->
                    PostItem(
                        post = postData,
                        navController = navController,
                        viewModel = postViewModel
                        // Xóa isClickable không cần thiết
                    )
                } ?: Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            item {
                Divider(thickness = 1.dp, modifier = Modifier.padding(top = 8.dp))
                Text(
                    "Bình luận (${comments.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            items(comments, key = { it.commentId }) { comment ->
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    CommentItem(
                        comment = comment,
                        viewModel = postViewModel,
                        navController = navController,
                        onEditClick = { selectedComment ->
                            commentToEdit = selectedComment
                        }
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
}


/* ======================= COMMENT ITEM (ĐÃ SỬA LỖI AVATAR) ======================= */
@Composable
fun CommentItem(
    comment: CommentModel,
    viewModel: PostViewModel,
    navController: NavController,
    onEditClick: (CommentModel) -> Unit
) {
    val userCache by viewModel.userCache.collectAsState()
    val author = userCache[comment.authorId]
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val isMyComment = currentUserId != null && currentUserId == comment.authorId
    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(comment.authorId) {
        if (author == null && comment.authorId.isNotBlank()) {
            viewModel.fetchUser(comment.authorId)
        }
    }

    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
        // ✅ BƯỚC 2: SỬA LỖI AVATAR, DÙNG ASYNCIMAGE VỚI PLACEHOLDER
        AsyncImage(
            model = author?.avatarUrl,
            contentDescription = "Avatar",
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable {
                    if (!isMyComment) {
                        navController.navigate("${Routes.Profile}?userId=${comment.authorId}")
                    } else {
                        navController.navigate(Routes.Profile)
                    }
                },
            contentScale = ContentScale.Crop,
            placeholder = painterResource(id = R.drawable.ic_profile),
            error = painterResource(id = R.drawable.ic_profile)
        )

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
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
            Text(
                text = comment.content,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
        }

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
                            onEditClick(comment)
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


/* ======================= BOTTOM COMMENT BAR (Giữ nguyên) ======================= */
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


/* ======================= EDIT COMMENT BAR (Giữ nguyên) ======================= */
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
