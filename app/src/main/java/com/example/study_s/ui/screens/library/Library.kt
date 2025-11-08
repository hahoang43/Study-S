package com.example.study_s.ui.screens.library

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.study_s.data.model.LibraryFile
import com.example.study_s.ui.navigation.Routes
import com.example.study_s.ui.screens.components.TopBar
import com.example.study_s.viewmodel.LibraryViewModel
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    navController: NavController,
    libraryViewModel: LibraryViewModel = viewModel()
) {
    val files by libraryViewModel.files.collectAsState()
    val isRefreshing by libraryViewModel.isRefreshing.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        libraryViewModel.loadAllFiles()
    }

    Scaffold(
        topBar = {
            TopBar(
                onNavIconClick = { /* TODO */ },
                onNotificationClick = { /* TODO */ },
                onSearchClick = { /* TODO */ }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Routes.FilePreview) }, // Điều hướng không có tham số
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add File", tint = Color.White)
            }
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { libraryViewModel.loadAllFiles() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF1A73E8), RoundedCornerShape(8.dp))
                        .background(Color(0xFFEAF1FD), RoundedCornerShape(8.dp))
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "THƯ VIỆN",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF1A73E8)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(files) { file ->
                        FileListItem(file = file, navController = navController)
                    }
                }
            }
        }
    }
}

@Composable
fun FileListItem(file: LibraryFile, navController: NavController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .border(1.dp, Color(0xFFB8C7E0), RoundedCornerShape(12.dp))
            .padding(16.dp)
            .clickable {
                // Sử dụng tham số truy vấn để truyền URL và tên tệp
                val encodedUrl = URLEncoder.encode(file.fileUrl, "UTF-8")
                navController.navigate("${Routes.FilePreview}?fileUrl=$encodedUrl&fileName=${file.fileName}")
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            FileIcon(mimeType = file.mimeType, fileUrl = file.fileUrl)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.fileName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tải lên bởi: ${file.uploaderName}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
        Icon(
            imageVector = Icons.Default.ArrowDownward,
            contentDescription = "Download",
            modifier = Modifier.size(24.dp),
            tint = Color(0xFF1A73E8)
        )
    }
}

@Composable
fun FileIcon(mimeType: String, fileUrl: String) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(Color(0xFFE0E0E0), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (mimeType.startsWith("image/")) {
            AsyncImage(
                model = fileUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val icon = when (mimeType) {
                "application/pdf" -> Icons.Default.PictureAsPdf
                "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> Icons.Default.Description
                "application/vnd.ms-powerpoint", "application/vnd.openxmlformats-officedocument.presentationml.presentation" -> Icons.Default.Slideshow
                else -> Icons.Default.Description
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewLibraryScreen() {
    LibraryScreen(navController = rememberNavController())
}

@Preview(showBackground = true)
@Composable
fun PreviewFileListItem() {
    val file = LibraryFile(
        fileName = "Bài tập giải tích.pdf",
        fileUrl = "https://example.com/file.pdf",
        mimeType = "application/pdf",
        uploaderName = "Nguyễn Văn A"
    )
    FileListItem(file = file, navController = rememberNavController())
}
