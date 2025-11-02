package com.example.study_s.ui.screens.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.study_s.ui.navigation.Routes

// Lớp dữ liệu để định nghĩa một mục trên Bottom NavBar, liên kết trực tiếp với Routes
private data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

@Composable
fun BottomNavBar(
    navController: NavController,
    currentRoute: String? // Truyền vào route hiện tại của màn hình
) {
    // Danh sách các mục, giờ đây liên kết trực tiếp với Routes
    val items = listOf(
        BottomNavItem("Thư viện", Icons.Default.VideoLibrary, Routes.Library),
        BottomNavItem("Nhóm", Icons.Default.Groups, Routes.GroupList),
        BottomNavItem("Trang chủ", Icons.Default.Home, Routes.Home),
        BottomNavItem("Lịch", Icons.Default.CalendarToday, Routes.Schedule),
        BottomNavItem("Hồ sơ", Icons.Default.Person, Routes.Profile)
    )

    NavigationBar(containerColor = Color.White) {
        items.forEach { item ->
            // Trạng thái "selected" được quyết định bằng cách so sánh route của item với route hiện tại
            val isSelected = currentRoute == item.route

            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    // Chỉ điều hướng khi người dùng bấm vào một mục chưa được chọn
                    if (!isSelected) {
                        navController.navigate(item.route) {
                            // Tối ưu hóa back stack để tránh chồng chất màn hình
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        // Thay đổi màu sắc dựa trên trạng thái isSelected
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        // Thay đổi màu sắc dựa trên trạng thái isSelected
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                },
                // Tùy chỉnh màu sắc để trông đẹp hơn
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.Transparent // Tắt màu nền của mục được chọn
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BottomNavBarPreview() {
    // Giả lập NavController và route để preview
    val navController = rememberNavController()
    // Để xem trước trạng thái "selected", hãy thay đổi route ở đây, ví dụ: Routes.Profile
    BottomNavBar(navController = navController, currentRoute = Routes.Home)
}
