package com.example.study_s.ui.screens.components
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
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
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
/**
 * TopBar hoàn chỉnh, kết hợp các điểm tốt nhất từ cả hai phiên bản.
 * - Sử dụng CenterAlignedTopAppBar để căn giữa tiêu đề.
 * - Có đầy đủ 3 icon: Menu, Search, và Notifications.
 * - Sử dụng màu sắc và font chữ từ MaterialTheme để linh hoạt với theme sáng/tối.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    onNavIconClick: () -> Unit = {},
    onNotificationClick: () -> Unit,
    onSearchClick: () -> Unit,
    onChatClick: () -> Unit,
    notificationCount: Int=0
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = "STUDY-S",
                color = MaterialTheme.colorScheme.onSurface, // Dùng màu của theme
                fontWeight = FontWeight.Bold,
                fontSize = 23.sp,
                fontFamily = FontFamily.Serif
            )
        },
        navigationIcon = {
            IconButton(onClick = onChatClick) {
                Icon(
                    imageVector = Icons.Default.ChatBubble,
                    contentDescription = "Chat",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Tìm kiếm",
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            // ✅ SỬA LẠI HOÀN TOÀN NÚT NOTIFICATION
            IconButton(onClick = onNotificationClick) {
                // BadgedBox sẽ bao bọc Icon chuông
                BadgedBox(
                    badge = {
                        // Chỉ hiển thị badge nếu số lượng > 0
                        if (notificationCount > 0) {
                            Badge(
                                // ✅ THÊM MODIFIER NÀY VÀO ĐỂ TINH CHỈNH VỊ TRÍ
                                modifier = Modifier.offset(x = (-4).dp, y = 6.dp)
                            ) {
                                Text(
                                    text = if (notificationCount > 99) "99+" else notificationCount.toString(),
                                    fontSize = 10.sp // Giảm cỡ chữ một chút cho đẹp hơn
                                )
                            }
                        }
                    }
                ) {
                    // Icon chuông bây giờ nằm bên trong BadgedBox
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Thông báo",
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface // Dùng màu nền của theme
        )
    )
}

@Preview(showBackground = true)
@Composable
fun TopBarPreview() {
    TopBar(
        onNavIconClick = {},
        onNotificationClick = {},
        onSearchClick = {},
        onChatClick = {}
    )
}
