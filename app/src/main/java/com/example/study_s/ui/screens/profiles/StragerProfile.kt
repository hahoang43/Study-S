package com.example.study_s.ui.screens.profiles

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StragerProfileScreen(
    navController: NavController,
    userId: String,
    stragerViewModel: StragerViewModel = viewModel()
) {
    val user by stragerViewModel.user.collectAsState()
    val isFollowing by stragerViewModel.isFollowing.collectAsState()
    val isLoading by stragerViewModel.isLoading.collectAsState()

    LaunchedEffect(userId) {
        stragerViewModel.loadUserProfile(userId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(user?.name ?: "Hồ sơ") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            user?.let {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(model = it.avatarUrl ?: R.drawable.avatar),
                        contentDescription = "Ảnh đại diện",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(it.name, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it.bio ?: "", fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(it.followerCount.toString(), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Text("Người theo dõi", fontSize = 16.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(it.followingCount.toString(), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Text("Đang theo dõi", fontSize = 16.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    if (isFollowing) {
                        OutlinedButton(onClick = { stragerViewModel.toggleFollow(userId) }) {
                            Text("Đang theo dõi")
                        }
                    } else {
                        Button(onClick = { stragerViewModel.toggleFollow(userId) }) {
                            Text("Theo dõi")
                        }
                    }
                }
            }
        }
    }
}
