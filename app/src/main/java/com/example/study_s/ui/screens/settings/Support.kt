package com.example.study_s.ui.screens.settings

import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.example.study_s.ui.theme.Study_STheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(navController: NavController) {
    val uriHandler = LocalUriHandler.current
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Hỗ trợ") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SupportItem("Hỗ trợ tài khoản") {
                // TODO: Thay thế bằng URL thực tế của bạn
                uriHandler.openUri("https://docs.google.com/document/d/10wmOw6YAuX2WteSM6oO8nb52p0H7_2zWGF5gQIhSFZE/edit?usp=sharing")
            }
            SupportItem("Hỗ trợ kĩ thuật") {
                // TODO: Thay thế bằng URL thực tế của bạn
                uriHandler.openUri("https://docs.google.com/document/d/1Ypj6xwFl1elabz3HoihG4gUPEXcvRJM7_aE98_c2U0g/edit?usp=sharing")
            }

        }
    }
}

@Composable
fun SupportItem(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
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
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SupportPreview() {
    val navController = rememberNavController()
    Study_STheme {
        SupportScreen(navController = navController)
    }
}
