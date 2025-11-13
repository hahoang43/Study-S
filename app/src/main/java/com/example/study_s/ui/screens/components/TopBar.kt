package com.example.study_s.ui.screens.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * TopBar hoàn chỉnh, kết hợp các điểm tốt nhất từ cả hai phiên bản.
 * - Sử dụng CenterAlignedTopAppBar để căn giữa tiêu đề.
 * - Có đầy đủ 3 icon: Menu, Search, và Notifications.
 * - Sử dụng màu sắc và font chữ từ MaterialTheme để linh hoạt với theme sáng/tối.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    onNavIconClick: () -> Unit={},
    onNotificationClick: () -> Unit,
    onSearchClick: () -> Unit
) {
    // 1. Dùng CenterAlignedTopAppBar để có tiêu đề ở giữa (từ file 1)
    CenterAlignedTopAppBar(
        // 2. Tiêu đề với font chữ cách điệu (từ file 2)
        title = {
            Text(
                text = "STUDY-S",
                color = MaterialTheme.colorScheme.onSurface, // Dùng màu của theme
                fontWeight = FontWeight.Bold,
                fontSize = 23.sp,
                fontFamily = FontFamily.Serif
            )
        },

        // 3. Icon điều hướng (Menu) (từ file 1)
        navigationIcon = {
            IconButton(onClick = onNavIconClick) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = MaterialTheme.colorScheme.onSurface // Dùng màu của theme
                )
            }
        },

        // 4. Các icon hành động (Search, Notifications) (từ cả hai file)
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Tìm kiếm",
                    modifier = Modifier.size(28.dp), // Có thể tùy chỉnh kích thước
                    tint = MaterialTheme.colorScheme.onSurface // Dùng màu của theme
                )
            }
            IconButton(onClick = onNotificationClick) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Thông báo",
                    modifier = Modifier.size(28.dp), // Có thể tùy chỉnh kích thước
                    tint = MaterialTheme.colorScheme.onSurface // Dùng màu của theme
                )
            }
        },

        // 5. Màu nền của TopBar (từ file 2)
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface // Dùng màu nền của theme
        )
    )
}

@Preview(showBackground = true)
@Composable
fun TopBarPreview() {
    // Preview với đầy đủ 3 tham số
    TopBar(
        onNavIconClick = {},
        onNotificationClick = {},
        onSearchClick = {}
    )
}
