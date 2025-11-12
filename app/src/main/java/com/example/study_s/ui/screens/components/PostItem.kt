// Đây là nội dung đầy đủ của file PostItem.kt
package com.example.study_s.ui.screens.components // ✅ Dòng package cho file mới

// ✅ Tất cả các import cần thiết cho 2 hàm
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
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
import java.text.SimpleDateFormat
import java.util.Locale
import com.google.firebase.auth.FirebaseAuth
// ✅ HÀM SỐ 1 (Đã di chuyển)
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

// ✅ HÀM SỐ 2 (Đã di chuyển)
@Composable
fun PostItem(navController: NavController, post: PostModel, viewModel: PostViewModel, modifier: Modifier = Modifier) {
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

    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
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
                    Text(text = authorName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    val formattedDate = post.timestamp?.toDate()?.let {
                        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(it)
                    } ?: "Không rõ thời gian"
                    Text(text = formattedDate, fontSize = 12.sp, color = Color.Gray)
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Tùy chọn")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (isSaved) "Bỏ lưu bài viết" else "Lưu bài viết") },
                            onClick = {
                                viewModel.toggleSavePost(post.postId)
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = null
                                )
                            }
                        )
                    }
                }
            }

            // Nội dung
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

            // Ảnh/File
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
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                .clickable {
                                    val encodedUrl = Uri.encode(post.imageUrl)
                                    val encodedName = Uri.encode(post.fileName ?: "Tệp đính kèm")
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
                IconButton(onClick = {
                    viewModel.toggleLike(post.postId)
                }) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Thích",
                        tint = if (isLiked) Color.Red else Color.Gray
                    )
                }
                Text(text = "${post.likesCount}")
                Spacer(modifier = Modifier.width(16.dp))
                IconButton(onClick = {
                    navController.navigate("${Routes.PostDetail}/${post.postId}")
                }) {
                    Icon(Icons.Default.Send, contentDescription = "Bình luận")
                }
                Text(text = "${post.commentsCount}")
            }
        }
    }
}

// ✅ Thêm import này vào file PostItem.kt (nếu còn thiếu)
