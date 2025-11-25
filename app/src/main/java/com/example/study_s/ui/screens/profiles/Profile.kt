// ĐƯỜNG DẪN: ui/screens/profiles/ProfileScreen.kt (PHIÊN BẢN SỬA LỖI)

package com.example.study_s.ui.screens.profiles
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.rememberAsyncImagePainter
import com.example.study_s.data.model.PostModel
import com.example.study_s.ui.navigation.Routes
import com.example.study_s.ui.screens.components.BottomNavBar
import com.example.study_s.viewmodel.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = viewModel(factory = ProfileViewModelFactory()),
    postViewModel: PostViewModel = viewModel(),
    // Tham số để nhận lệnh điều hướng
    onNavigateToFollowList: (userId: String, listType: String) -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // ✅ SỬA LỖI 1: Lấy ID từ ViewModel một cách an toàn
    val currentUserId = viewModel.currentUserId
    var myPosts by remember { mutableStateOf<List<PostModel>>(emptyList()) }

    LaunchedEffect(Unit) {
        viewModel.loadCurrentUserProfile()
    }

    LaunchedEffect(currentUserId) {
        if (currentUserId != null) {
            postViewModel.loadPosts()
            postViewModel.posts.collect { allPosts ->
                myPosts = allPosts
                    .filter { it.authorId == currentUserId }
                    .sortedByDescending { it.timestamp?.toDate()?.time ?: 0L }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("STUDY-S", fontWeight = FontWeight.Bold, fontSize = 23.sp, fontFamily = FontFamily.Serif) },
                actions = { IconButton(onClick = { navController.navigate(Routes.Settings) }) { Icon(Icons.Default.Settings, contentDescription = null) } }
            )
        },
        bottomBar = { BottomNavBar(navController = navController, currentRoute = currentRoute) }
    ) { padding ->

        val uiState = viewModel.profileUiState

        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (uiState) {
                is ProfileUiState.Loading -> item { Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
                is ProfileUiState.Error -> item { Text(uiState.message, color = Color.Red, modifier = Modifier.padding(16.dp)) }
                is ProfileUiState.Success -> {
                    val user = uiState.userModel
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(user.avatarUrl ?: "https://i.pravatar.cc/200"),
                                contentDescription = null,
                                modifier = Modifier.size(90.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(20.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(user.name, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    ProfileStat(myPosts.size.toString(), "bài viết")

                                    // ✅ SỬA LỖI 2: Dùng Modifier.clickable đơn giản
                                    ProfileStat(
                                        count = user.followerCount.toString(),
                                        label = "người theo dõi",
                                        modifier = Modifier.clickable { onNavigateToFollowList(user.userId, "followers") }
                                    )
                                    ProfileStat(
                                        count = user.followingCount.toString(),
                                        label = "đang theo dõi",
                                        modifier = Modifier.clickable { onNavigateToFollowList(user.userId, "following") }
                                    )
                                }
                            }
                        }
                        // ✅ BƯỚC 1: DÁN ĐOẠN CODE HIỂN THỊ TIỂU SỬ VÀO ĐÂY
                        // ✅ ==============================================================
                        Spacer(modifier = Modifier.height(24.dp))

                        // Kiểm tra xem bio có nội dung không
                        if (!user.bio.isNullOrBlank()) {
                            Text(
                                text = user.bio, // Hiển thị tiểu sử
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 32.dp), // Căn lề cho đẹp
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, // Màu chữ phụ
                                textAlign = TextAlign.Center // Căn lề trái
                            )
                        }

                        Divider(thickness = 1.dp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Bài viết", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 16.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    items(myPosts) { post ->
                        ProfilePostCard(post, navController)
                        Divider(color = Color.LightGray, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))
                    }
                }
            }
        }
    }
}

// Composable này đã được sửa lại cho đúng
@Composable
fun ProfileStat(count: String, label: String, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(4.dp)
    ) {
        Text(count, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(label, fontSize = 12.sp, color = Color.Gray)
    }
}

// Composable này giữ nguyên
@Composable
fun ProfilePostCard(post: PostModel, navController: NavController) {
    val formattedDate = post.timestamp?.toDate()?.let { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(it) } ?: ""
    Column(
        modifier = Modifier.fillMaxWidth().clickable { navController.navigate("${Routes.PostDetail}/${post.postId}") }.padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = rememberAsyncImagePainter(post.authorAvatarUrl ?: "https://i.pravatar.cc/150"),
                contentDescription = null,
                modifier = Modifier.size(40.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = post.authorName, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text(text = formattedDate, fontSize = 12.sp, color = Color.Gray)
            }
        }
        Spacer(Modifier.height(12.dp))
        if (post.content.isNotBlank()) {
            Text(text = post.content, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
        }
        post.imageUrl?.let {
            Image(
                painter = rememberAsyncImagePainter(it),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).heightIn(max = 300.dp),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.height(12.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.FavoriteBorder, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("${post.likesCount}")
            Spacer(modifier = Modifier.width(20.dp))
            Icon(imageVector = Icons.Default.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("${post.commentsCount}")
        }
    }
}
