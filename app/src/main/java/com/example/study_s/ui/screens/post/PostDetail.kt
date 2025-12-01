package com.example.study_s.ui.screens.post

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.study_s.data.model.CommentModel
// ✅ BƯỚC 3.1: IMPORT CÁC COMPOSABLE ĐÃ TÁCH
import com.example.study_s.ui.screens.components.BottomCommentBar
import com.example.study_s.ui.screens.components.CommentItem
import com.example.study_s.ui.screens.components.EditCommentBar
import com.example.study_s.ui.screens.components.PostItem
import com.example.study_s.viewmodel.PostViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    postId: String,
    navController: NavController,
    postViewModel: PostViewModel = viewModel()
) {
    val post by postViewModel.selectedPost.collectAsState()
    val comments by postViewModel.comments.collectAsState()
    val lazyListState = rememberLazyListState()

    var commentInput by remember { mutableStateOf("") }
    var commentToEdit by remember { mutableStateOf<CommentModel?>(null) }

    LaunchedEffect(postId) {
        postViewModel.selectPostAndLoadComments(postId)
    }

    // Tự động cuộn xuống bình luận mới nhất
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
            // ✅ BƯỚC 3.2: GỌI CÁC COMPOSABLE, MỌI THỨ VẪN NHƯ CŨ
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
                BottomCommentBar(
                    commentText = commentInput,
                    onCommentChanged = { commentInput = it },
                    onSendClick = {
                        val content = commentInput.trim()
                        if (content.isNotBlank()) {
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
            // Phần hiển thị bài đăng
            item {
                post?.let { postData ->
                    PostItem(
                        post = postData,
                        navController = navController,
                        viewModel = postViewModel,
                        isClickable = false
                    )
                } ?: Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            // Phần tiêu đề bình luận
            item {
                Divider(thickness = 1.dp, modifier = Modifier.padding(top = 8.dp))
                Text(
                    "Bình luận (${comments.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            // ✅ BƯỚC 3.3: GỌI CommentItem TRONG VÒNG LẶP
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
