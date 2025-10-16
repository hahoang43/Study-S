package com.example.study_s.ui.screens.components

import androidx.compose.foundation.Image
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.example.study_s.R

@Composable
fun BottomNavBar(
    selectedIndex: Int = 0,
    onItemSelected: (Int) -> Unit = {}
) {
    val items = listOf(
        "Thư viện" to R.drawable.library,
        "Nhóm" to R.drawable.group,
        "Trang chủ" to R.drawable.home,
        "Lịch" to R.drawable.calendar,
        "Hồ sơ" to R.drawable.profile
    )

    NavigationBar(containerColor = Color.White) {
        items.forEachIndexed { index, (label, iconRes) ->
            val isSelected = index == selectedIndex

            NavigationBarItem(
                selected = isSelected,
                onClick = { onItemSelected(index) },
                icon = {
                    IconButton(onClick = { onItemSelected(index) }) {
                        Image(
                            painter = painterResource(id = iconRes),
                            contentDescription = label
                        )
                    }
                },
                // Tắt hiệu ứng Bỏ màu nền tím khi chọn
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BottomNavBarPreview() {
    BottomNavBar(selectedIndex = 2)
}
