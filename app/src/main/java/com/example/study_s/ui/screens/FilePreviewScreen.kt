package com.example.study_s.ui.screens

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun FilePreviewScreen(
    navController: NavController,
    fileUrl: String,
    fileName: String
) {
    // 🔍 Kiểm tra nếu file là hình ảnh
    val isImage = fileName.endsWith(".jpg", true) ||
            fileName.endsWith(".jpeg", true) ||
            fileName.endsWith(".png", true) ||
            fileName.endsWith(".gif", true) ||
            fileName.endsWith(".webp", true)

    // 🧩 Tạo URL xem trước qua Google Docs Viewer (chỉ dùng cho file không phải ảnh)
    val viewerUrl = "https://docs.google.com/gview?embedded=true&url=${
        URLEncoder.encode(fileUrl, "UTF-8")
    }"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = fileName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isImage) {
                // 🖼️ Hiển thị ảnh trực tiếp
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(fileUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = fileName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                )
            } else {
                // 🧾 Hiển thị file khác (PDF, DOC, v.v.) qua Google Docs Viewer
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            webViewClient = WebViewClient()
                            settings.javaScriptEnabled = true
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            settings.setSupportZoom(true)
                        }
                    },
                    update = { webView ->
                        webView.loadUrl(viewerUrl)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
