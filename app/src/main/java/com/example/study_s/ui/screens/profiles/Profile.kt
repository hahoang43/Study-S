package com.example.study_s.ui.screens.profiles

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
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import coil.compose.rememberAsyncImagePainter
import com.example.study_s.data.model.PostModel
import com.example.study_s.data.model.User
import com.example.study_s.ui.navigation.Routes
import com.example.study_s.ui.screens.components.BottomNavBar
import com.example.study_s.viewmodel.*
import com.google.firebase.auth.FirebaseAuth
import androidx.navigation.compose.currentBackStackEntryAsState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = viewModel(factory = ProfileViewModelFactory()),
    postViewModel: PostViewModel = viewModel()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    var myPosts by remember { mutableStateOf<List<PostModel>>(emptyList()) }

    // Load thông tin user
    LaunchedEffect(Unit) { viewModel.loadCurrentUserProfile() }

    // Load bài viết của mình
    LaunchedEffect(currentUserId) {
        postViewModel.loadPosts()
        postViewModel.posts.collect { posts ->
            myPosts = posts
                .filter { it.authorId == currentUserId }
                .sortedByDescending { it.timestamp?.toDate()?.time ?: 0L }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "STUDY-S",
                        fontWeight = FontWeight.Bold,
                        fontSize = 23.sp,
                        fontFamily = FontFamily.Serif
                    )
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Routes.Settings) }) {
                        Icon(Icons.Default.Settings, contentDescription = null)
                    }
                }
            )
        },
        bottomBar = {
            BottomNavBar(navController = navController, currentRoute = currentRoute)
        }
    ) { padding ->

        val uiState = viewModel.profileUiState

        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when (uiState) {

                is ProfileUiState.Loading -> item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is ProfileUiState.Error -> item {
                    Text(uiState.message, color = Color.Red, modifier = Modifier.padding(16.dp))
                }

                is ProfileUiState.Success -> {

                    val user = uiState.user

// ================= HEADER =================
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            // ===== AVATAR BÊN TRÁI =====
                            Image(
                                painter = rememberAsyncImagePainter(
                                    user.avatarUrl ?: "https://i.pravatar.cc/200"
                                ),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )

                            Spacer(modifier = Modifier.width(20.dp))

                            // ===== TÊN + SỐ LIỆU BÊN PHẢI =====
                            Column(modifier = Modifier.weight(1f)) {

                                Text(
                                    user.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {

                                    ProfileStat(myPosts.size.toString(), "bài viết")
                                    ProfileStat(user.followerCount.toString(), "người theo dõi")
                                    ProfileStat(user.followingCount.toString(), "đang theo dõi")
                                }
                            }
                        }

                        Divider(thickness = 1.dp)

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            "Bài viết",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // =============== LIST BÀI VIẾT ===============
                    items(myPosts) { post ->
                        ProfilePostCard(post, navController)
                        Divider(color = Color.LightGray, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))
                    }

                }
            }
        }
    }
}

@Composable
fun ProfileStat(count: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(count, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(label, fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
fun ProfilePostCard(post: PostModel, navController: NavController) {

    val formattedDate = post.timestamp?.toDate()?.let {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(it)
    } ?: ""

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                navController.navigate("${Routes.PostDetail}/${post.postId}")
            }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {

        // HEADER avatar + tên + thời gian
        Row(verticalAlignment = Alignment.CenterVertically) {

            Image(
                painter = rememberAsyncImagePainter(
                    post.authorAvatarUrl ?: "https://i.pravatar.cc/150"
                ),
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = post.authorName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                Text(
                    text = formattedDate,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // CONTENT
        if (post.content.isNotBlank()) {
            Text(
                text = post.content,
                fontSize = 16.sp
            )
            Spacer(Modifier.height(8.dp))
        }

        // IMAGE nếu có
        post.imageUrl?.let {
            Image(
                painter = rememberAsyncImagePainter(it),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .heightIn(max = 300.dp),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.height(12.dp))
        }

        // LIKE + COMMENT xem thôi
        Row(verticalAlignment = Alignment.CenterVertically) {

            Icon(
                imageVector = Icons.Default.FavoriteBorder,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("${post.likesCount}")

            Spacer(modifier = Modifier.width(20.dp))

            Icon(
                imageVector = Icons.Default.ChatBubbleOutline,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("${post.commentsCount}")
        }
    }
}

