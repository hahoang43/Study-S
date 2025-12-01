package com.example.study_s.ui.screens.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.study_s.R
import com.example.study_s.data.model.CommentModel
import com.example.study_s.ui.navigation.Routes
import com.example.study_s.viewmodel.PostViewModel
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Locale

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

    // Tự động fetch thông tin người dùng nếu chưa có trong cache
    LaunchedEffect(comment.authorId) {
        if (author == null && comment.authorId.isNotBlank()) {
            viewModel.fetchUser(comment.authorId)
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Avatar người bình luận
        AsyncImage(
            model = author?.avatarUrl,
            contentDescription = "Avatar",
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable {
                    // Điều hướng đến trang cá nhân của người bình luận
                    if (!isMyComment) {
                        navController.navigate("${Routes.Profile}?userId=${comment.authorId}")
                    } else {
                        navController.navigate(Routes.Profile) // Về trang cá nhân của mình
                    }
                },
            contentScale = ContentScale.Crop,
            placeholder = painterResource(id = R.drawable.ic_profile),
            error = painterResource(id = R.drawable.ic_profile)
        )

        Spacer(Modifier.width(12.dp))

        // Nội dung bình luận
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

        // Menu tùy chọn cho bình luận của mình
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
