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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.study_s.ui.navigation.Routes
import com.example.study_s.ui.theme.Study_STheme

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

    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        items.forEach { item ->
            // Trạng thái "selected" được quyết định bằng cách so sánh route của item với route hiện tại
            val isSelected = currentRoute == item.route

            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (item.route == Routes.Home && isSelected) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    } else if (!isSelected) {
                        navController.navigate(item.route) {
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
                        contentDescription = item.label
                    )
                },
                label = { Text(text = item.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.surfaceColorAtElevation(LocalAbsoluteTonalElevation.current + 3.dp)
                )
            )
        }
    }
}

@Preview(showBackground = true, name = "Light Mode")
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, name = "Dark Mode")
@Composable
fun BottomNavBarPreview() {
    Study_STheme {
        Surface {
            BottomNavBar(navController = rememberNavController(), currentRoute = Routes.Home)
        }
    }
}