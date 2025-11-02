package com.example.study_s.ui.screens.profiles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.study_s.ui.navigation.Routes
import com.example.study_s.ui.screens.components.BottomNavBar
import com.example.study_s.ui.screens.components.TopBar

@Composable
fun ProfileScreen(
    navController: NavController
) {
    val username = "Ngo Ich"
    val email = "20020509@vnu.edu.vn"
    val profileImageUrl = ""
    var selectedTab by remember { mutableStateOf(4) } // Profile is the 5th item (index 4)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        topBar = {
            TopBar(
                onNavIconClick = { /* TODO: open drawer */ },
                onNotificationClick = { /* TODO: open notifications */ }
            )
        },
        bottomBar = {
                // SỬA Ở ĐÂY: Gọi BottomNavBar có sẵn và truyền NavController, route hiện tại
                BottomNavBar(navController = navController, currentRoute = currentRoute)
            }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ===== Header + Avatar =====
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(Color(0xFFA3D6E0)),
                contentAlignment = Alignment.BottomCenter
            ) {
                AsyncImage(
                    model = profileImageUrl.ifEmpty { "https://i.imgur.com/8p3xYso.png" },
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(90.dp)
                        .offset(y = 45.dp) // Overlap the header
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(50.dp)) // Space for the overlapping avatar

            // ===== Name, Email + Edit Icon =====
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = username,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp
                )
                IconButton(
                    onClick = { navController.navigate(Routes.EditProfile) },
                    modifier = Modifier.size(24.dp).padding(start = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Profile",
                        tint = Color.Gray
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = email, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)

            Spacer(modifier = Modifier.height(24.dp))

            Button(onClick = { /* TODO: Navigate to user's posts */ }) {
                Text("Các bài viết")
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ProfileScreenPreview() {
    ProfileScreen(navController = rememberNavController())
}
