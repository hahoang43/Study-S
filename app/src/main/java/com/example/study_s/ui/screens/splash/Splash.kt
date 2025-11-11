package com.example.study_s.ui.screens.splash

import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.example.study_s.ui.theme.Study_STheme
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.study_s.R
import com.example.study_s.ui.navigation.Routes
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController) {
    LaunchedEffect(Unit) {
        delay(500)
        // Kiểm tra trạng thái đăng nhập của người dùng
        val currentUser = FirebaseAuth.getInstance().currentUser
        val destination = if (currentUser != null) {
            // Nếu đã đăng nhập, đi tới màn hình chính (ví dụ: danh sách nhóm)
            Routes.Home
        } else {
            // Nếu chưa, đi tới màn hình đăng nhập
            Routes.Login
        }
        navController.navigate(destination) {
            // Xóa màn hình splash khỏi back stack để không thể quay lại
            popUpTo("splash") { inclusive = true }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE7F0F8)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.logo_study),
                contentDescription = "Logo App",
                modifier = Modifier.size(300.dp)
            )
        }
    }
}
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SplashPreview() {
    val navController = rememberNavController()
    Study_STheme {
        SplashScreen(navController = navController)
    }
}
