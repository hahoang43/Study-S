package com.example.study_s.ui.screens.profiles

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage
import com.example.study_s.R
import com.example.study_s.data.model.PostModel
import com.example.study_s.ui.navigation.Routes
import com.example.study_s.ui.screens.components.BottomNavBar
import com.example.study_s.ui.screens.components.PostItem
import com.example.study_s.viewmodel.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = viewModel(factory = ProfileViewModelFactory()),
    postViewModel: PostViewModel,
    authViewModel: AuthViewModel,
    onNavigateToFollowList: (userId: String, listType: String) -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val currentUserId = viewModel.currentUserId
    var myPosts by remember { mutableStateOf<List<PostModel>>(emptyList()) }

    // --- LOGIC CHO PULL-TO-REFRESH ---
    var isRefreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = {
            coroutineScope.launch {
                isRefreshing = true
                viewModel.loadCurrentUserProfile()
                if (currentUserId != null) {
                    postViewModel.loadPosts()
                }
                isRefreshing = false
            }
        }
    )

    // Tải dữ liệu lần đầu tiên khi màn hình được mở
    LaunchedEffect(currentUserId) {
        if (currentUserId != null && myPosts.isEmpty()) { // Chỉ tải nếu danh sách đang trống
            viewModel.loadCurrentUserProfile()
            postViewModel.loadPosts()
        }
    }

    // Logic lọc bài viết của bạn (giữ nguyên)
    LaunchedEffect(currentUserId, postViewModel.posts) {
        postViewModel.posts.collect { allPosts ->
            if (currentUserId != null) {
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

        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .pullRefresh(pullRefreshState)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                when (val state = uiState) {
                    is ProfileUiState.Error -> item { Text(state.message, color = Color.Red, modifier = Modifier.padding(16.dp)) }
                    is ProfileUiState.Success -> {
                        val user = state.userModel
                        item {
                            // Phần Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = user.avatarUrl,
                                    contentDescription = "User Avatar",
                                    modifier = Modifier
                                        .size(90.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop,
                                    placeholder = painterResource(id = R.drawable.ic_profile),
                                    error = painterResource(id = R.drawable.ic_profile)
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
                            // Phần Bio
                            if (!user.bio.isNullOrBlank()) {
                                Text(
                                    text = user.bio,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 32.dp),
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                            Divider(thickness = 1.dp)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Bài viết", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 16.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Danh sách bài viết
                        items(myPosts, key = { it.postId }) { post ->
                            PostItem(
                                post = post,
                                navController = navController,
                                viewModel = postViewModel,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                    is ProfileUiState.Loading -> {
                        // Nếu muốn hiện skeleton loading thì thêm code ở đây
                    }
                }
            }

            // Indicator cho Pull-to-Refresh
            PullRefreshIndicator(
                refreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

// Composable này không đổi
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
