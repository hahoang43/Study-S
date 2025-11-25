
package com.example.study_s.ui.screens.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.study_s.data.model.CommentModel
import com.example.study_s.viewmodel.PostViewModel
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun CommentItem(
    comment: CommentModel,
    viewModel: PostViewModel,
    // Callback để thông báo cho màn hình PostDetail rằng chúng ta muốn sửa bình luận này
    onEditClick: (CommentModel) -> Unit
) {
    val userCache by viewModel.userCache.collectAsState()
    val author = userCache[comment.authorId]

    // Lấy thông tin người dùng nếu chưa có trong cache
    LaunchedEffect(comment.authorId) {
        if (author == null && comment.authorId.isNotBlank()) {
            viewModel.fetchUser(comment.authorId)
        }
    }

    // Kiểm tra xem người dùng hiện tại có phải là tác giả của bình luận không
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val isCommentAuthor = currentUserId != null && currentUserId == comment.authorId

    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Image(
            painter = rememberAsyncImagePainter(author?.avatarUrl ?: "https://i.pravatar.cc/150?img=3"),
            contentDescription = "Avatar",
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(12.dp))

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
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = comment.content,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
        }

        // Chỉ hiển thị menu nếu là tác giả của bình luận
        if (isCommentAuthor) {
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
                            onEditClick(comment) // Gọi callback để xử lý việc sửa
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
