package com.example.study_s.ui.screens.profiles

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.study_s.R
import com.example.study_s.viewmodel.StragerViewModel
import com.example.study_s.ui.navigation.Routes
import com.example.study_s.ui.screens.components.PostItem
import com.example.study_s.viewmodel.PostViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StragerProfileScreen(
    navController: NavController,
    userId: String,
    stragerViewModel: StragerViewModel = viewModel(),
    postViewModel: PostViewModel = viewModel()
) {
    val user by stragerViewModel.user.collectAsState()
    val posts by stragerViewModel.posts.collectAsState()
    val isFollowing by stragerViewModel.isFollowing.collectAsState()
    val isLoading by stragerViewModel.isLoading.collectAsState()

    LaunchedEffect(userId) {
        stragerViewModel.loadUserProfile(userId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(user?.name ?: "Trang cá nhân") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        user?.let { u ->

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
            ) {

                // ========= HEADER PROFILE =========
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        // Avatar
                        Image(
                            painter = rememberAsyncImagePainter(
                                model = u.avatarUrl ?: R.drawable.avatar
                            ),
                            contentDescription = "Avatar",
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(modifier = Modifier.width(20.dp))

                        // Name + Stats
                        Column {
                            Text(
                                u.name,
                                fontSize = 23.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(u.followerCount.toString(), fontWeight = FontWeight.Bold)
                                    Text("Người theo dõi", fontSize = 13.sp)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(u.followingCount.toString(), fontWeight = FontWeight.Bold)
                                    Text("Đang theo dõi", fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // ===== BUTTONS: FOLLOW + MESSAGE =====
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {

                        // Follow button
                        if (isFollowing) {
                            OutlinedButton(
                                onClick = { stragerViewModel.toggleFollow(userId) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Đang theo dõi")
                            }
                        } else {
                            Button(
                                onClick = { stragerViewModel.toggleFollow(userId) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Theo dõi")
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Message button
                        Button(
                            onClick = {
                                navController.navigate("chat/$userId")
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Nhắn tin")
                        }
                    }

                    Spacer(modifier = Modifier.height(30.dp))

                    Text(
                        "Bài viết",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                }

                // ========= LIST POSTS =========
                items(posts) { postData ->
                    PostItem(
                        post = postData, // `postData` là một bài viết từ danh sách `posts`
                        navController = navController,
                        viewModel = postViewModel, // Truyền `postViewModel` vào
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}
