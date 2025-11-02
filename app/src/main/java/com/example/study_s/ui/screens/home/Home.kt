package com.example.study_s.ui.screens.home

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.study_s.ui.navigation.Routes

// Lớp dữ liệu mẫu cho một bài viết. Sau này bạn sẽ thay bằng model thật.
data class Post( val id: String, val authorName: String, val authorAvatarUrl: String, val content: String, val timestamp: String)

@Composable
fun HomeScreen(navController: NavController) {

}