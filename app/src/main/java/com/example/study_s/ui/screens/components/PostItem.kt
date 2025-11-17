package com.example.study_s.ui.screens.components

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.study_s.data.model.PostModel
import com.example.study_s.data.model.User
import com.example.study_s.ui.navigation.Routes
import com.example.study_s.viewmodel.PostViewModel
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Locale

// hàm downloadFile không đổi
fun downloadFile(context: Context, url: String, fileName: String) {
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
        Log.e("DownloadDebug", "Lỗi DownloadManager: ${e.message}", e)
        Toast.makeText(context, "Lỗi tải xuống: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

@Composable
fun PostItem(navController: NavController, post: PostModel, viewModel: PostViewModel, modifier: Modifier = Modifier,isClickable: Boolean = true) {
    val context = LocalContext.current
    val userCache by viewModel.userCache.collectAsState()

    val author = userCache[post.authorId] ?: User(name = "Đang tải...")
    LaunchedEffect(post.authorId) {
        if (post.authorId.isNotBlank()) {
            viewModel.fetchUser(post.authorId)
        }
    }
    val authorName = author.name
    val authorAvatar = author.avatarUrl

    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val isLiked = remember(post.likedBy) {
        currentUserId?.let { post.likedBy.contains(it) } ?: false
    }
    val isSaved = remember(post.savedBy) {
        currentUserId?.let { post.savedBy.contains(it) } ?: false
    }
    // KIỂM TRA QUYỀN SỞ HỮU BÀI VIẾT
    val isAuthor = currentUserId != null && currentUserId == post.authorId

    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = isClickable) { navController.navigate("${Routes.PostDetail}/${post.postId}") },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ){
        Column(modifier = Modifier.padding(12.dp)) {
            // Header (không đổi)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = rememberAsyncImagePainter(authorAvatar ?: "https://i.pravatar.cc/150?img=5"),
                    contentDescription = "Avatar của ${authorName}",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = authorName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    val formattedDate = post.timestamp?.toDate()?.let {
                        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(it)
                    } ?: "Không rõ thời gian"
                    Text(text = formattedDate, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Tùy chọn", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    // =================== CẬP NHẬT DROPDOWN MENU ===================
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (isSaved) "Bỏ lưu" else "Lưu bài viết") },
                            onClick = {
                                viewModel.toggleSavePost(post.postId)
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                    contentDescription = null
                                )
                            }
                        )

                        // CHỈ HIỂN THỊ NẾU LÀ TÁC GIẢ
                        if (isAuthor) {
                            Divider()
                            DropdownMenuItem(
                                text = { Text("Sửa bài viết") },
                                onClick = {
                                    navController.navigate("${Routes.EditPost}/${post.postId}")
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Sửa bài viết"
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Xóa bài viết") },
                                onClick = {
                                    viewModel.deletePost(post.postId)
                                    showMenu = false
                                    Toast.makeText(context, "Đã xóa bài viết", Toast.LENGTH_SHORT).show()
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Xóa bài viết",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            )
                        }
                    }
                    // =============================================================
                }
            }

            // Nội dung (không đổi)
            Spacer(modifier = Modifier.height(12.dp))
            if (post.content.isNotBlank()) {
                Text(
                    text = post.content,
                    style = MaterialTheme.typography.bodyLarge,
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Ảnh/File (không đổi)
            if (post.imageUrl != null) {
                val isImage = post.fileName?.let {
                    it.endsWith(".jpg", true) || it.endsWith(".jpeg", true) || it.endsWith(".png", true)
                } ?: true

                if (isImage) {
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
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
                        IconButton(onClick = {
                            post.imageUrl?.let { downloadFile(context, it, post.fileName ?: "downloaded_file") }
                        }) {
                            Icon(
                                imageVector = Icons.Filled.AttachFile,
                                contentDescription = "Tải xuống",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Footer Actions (không đổi)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // LIKE
                Row(
                    modifier = Modifier.clickable { viewModel.toggleLike(post.postId) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Thích",
                        tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(text = post.likesCount.toString(), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // COMMENT
                Row(
                    modifier = Modifier.clickable { navController.navigate("${Routes.PostDetail}/${post.postId}") },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "Bình luận",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(text = post.commentsCount.toString(), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
