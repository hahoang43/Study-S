package com.example.study_s.ui.screens.settings

import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.example.study_s.ui.theme.Study_STheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PolicyScreen(navController: NavController) {
    var urlToLoad by remember { mutableStateOf<String?>(null) }
    val title = if (urlToLoad == null) "Chính sách và điều khoản" else ""

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (urlToLoad != null) {
                        IconButton(onClick = { urlToLoad = null }) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    } else {
                        // Giữ lại nút back mặc định nếu có để quay về màn hình trước
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (urlToLoad == null) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PolicyItem("Chính sách người dùng") {
                    // TODO: Thay thế bằng URL thực tế của bạn
                    urlToLoad = "https://docs.google.com/document/d/1SPeguTL2g8x12_9yNXIWKpgH0l2SXG9H1s4-gqhaVAo/edit?usp=sharing"
                }
                PolicyItem("Điều khoản ứng dụng") {
                    // TODO: Thay thế bằng URL thực tế của bạn
                    urlToLoad = "https://docs.google.com/document/d/1q8pyhfy9uDZF_LhneafXhuZ6pJ9gmbzunkhsTA8IvmM/edit?usp=sharing"
                }
            }
        } else {
            PolicyWebView(
                url = urlToLoad!!,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
fun PolicyWebView(url: String, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = true // Bật JavaScript nếu cần
                loadUrl(url)
            }
        }
    )
}


@Composable
fun PolicyItem(text: String, onClick: () -> Unit) {
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
