package com.example.study_s.ui.screens.policy

import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.example.study_s.ui.theme.Study_STheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PolicyScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Chính sách và điều khoản") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PolicyItem("Chính sách người dùng")
            PolicyItem("Điều khoản ứng dụng")
        }
    }
}

@Composable
fun PolicyItem(text: String) {
    Button(
        onClick = { },
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFD2E4FF),
            contentColor = Color.Black
        )
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text)
            Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PolicyPreview() {
    val navController = rememberNavController()
    Study_STheme {
        PolicyScreen(navController = navController)
    }
}

