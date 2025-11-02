@file:Suppress(
    "DEPRECATION",
    "OPT_IN_IS_NOT_ENABLED",
    "SpellCheckingInspection",
    "UnusedImport"
)

package com.example.study_s.ui.screens.admin.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SideMenu() {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(220.dp)
            .background(Color(0xFFF3F4F6))
            .padding(all = 16.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Menu",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            modifier = Modifier.padding(bottom = 24.dp),
            color = Color(0xFF1976D2)
        )

        // Menu Items (đã đổi icon mới để không cảnh báo)
        MenuItem(icon = Icons.Filled.Dashboard, title = "Bảng điều khiển")
        MenuItem(icon = Icons.Filled.Group, title = "Người dùng")
        MenuItem(icon = Icons.AutoMirrored.Filled.TrendingUp, title = "Báo cáo")
        MenuItem(icon = Icons.Filled.Settings, title = "Cài đặt")
    }
}

@Composable
fun MenuItem(icon: ImageVector, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = Color(0xFF1976D2)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            fontSize = 16.sp,
            color = Color.DarkGray,
            fontWeight = FontWeight.Medium
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SideMenuPreview() {
    MaterialTheme {
        SideMenu()
    }
}
