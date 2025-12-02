package com.example.study_s.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.study_s.R
import com.example.study_s.ui.navigation.Routes
import com.example.study_s.ui.screens.components.BottomNavBar
import com.example.study_s.ui.screens.components.PostItem
import com.example.study_s.ui.screens.components.TopBar
import com.example.study_s.viewmodel.AuthViewModel
import com.example.study_s.viewmodel.AuthViewModelFactory
import com.example.study_s.viewmodel.MainViewModel
import com.example.study_s.viewmodel.PostViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    navController: NavController,
    postViewModel: PostViewModel,
    mainViewModel: MainViewModel,
    authViewModel: AuthViewModel // Nhận ViewModel từ NavGraph, không tự khởi tạo
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val posts by postViewModel.posts.collectAsState()
    val unreadNotificationCount by mainViewModel.unreadNotificationCount.collectAsState()
    val unreadMessageCount by mainViewModel.unreadMessageCount.collectAsState() // ✅ GET UNREAD MESSAGE COUNT

    val lazyListState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val currentPostViewModel by rememberUpdatedState(postViewModel)
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                currentPostViewModel.loadPosts()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        postViewModel.scrollToTopEvent.collectLatest {
            scope.launch {
                lazyListState.animateScrollToItem(index = 0)
            }
        }
    }

    Scaffold(
        topBar = {
            TopBar(
                onNotificationClick = { navController.navigate(Routes.Notification) },
                onSearchClick = { navController.navigate(Routes.Search) },
                onChatClick = { navController.navigate(Routes.Message) },
                notificationCount = unreadNotificationCount,
                messageCount = unreadMessageCount // ✅ PASS COUNT TO TOPBAR
            )
        },
        bottomBar = {
            BottomNavBar(
                navController = navController,
                currentRoute = currentRoute,
                onHomeIconReselected = {
                    postViewModel.reloadPostsAndScrollToTop()
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            state = lazyListState,
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                CreatePostTrigger(
                    navController = navController,
                    avatarUrl = currentUser?.photoUrl?.toString(),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            items(posts, key = { it.postId }) { post ->
                PostItem(
                    navController = navController,
                    post = post,
                    viewModel = postViewModel,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}


@Composable
fun CreatePostTrigger(navController: NavController, avatarUrl: String?, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { navController.navigate(Routes.NewPost) },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = "Avatar của bạn",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(id = R.drawable.ic_profile),
                error = painterResource(id = R.drawable.ic_profile)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text("Bạn đang nghĩ gì?", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    val navController = rememberNavController()
    val mainViewModel: MainViewModel = viewModel()
    val postViewModel: PostViewModel = viewModel()
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory())
    HomeScreen(
        navController = navController,
        mainViewModel = mainViewModel,
        postViewModel = postViewModel,
        authViewModel = authViewModel
    )
}
