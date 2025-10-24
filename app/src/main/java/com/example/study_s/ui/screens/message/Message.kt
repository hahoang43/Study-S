package com.example.study_s.ui.screens.message

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.study_s.R
import com.example.study_s.ui.screens.components.BottomNavBar

// Data class để quản lý dữ liệu tin nhắn
data class Message(
    val id: Int,
    val name: String,
    val lastMessage: String,
    val time: String,
    val avatarRes: Int, // Dùng Int cho ID tài nguyên drawable
    val isUnread: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageListScreen() {
    // Dữ liệu mẫu để hiển thị trong danh sách
    val messages = listOf(
        Message(1, "Nguyễn Văn A", "Bạn có ở đó không?", "10:30", R.drawable.ic_profile, true),
        Message(2, "Trần Thị B", "Cảm ơn bạn nhiều nhé!", "09:45", R.drawable.ic_profile, false),
        Message(3, "Lê Văn C", "Ok, hẹn gặp lại bạn sau.", "Hôm qua", R.drawable.ic_profile, true),
        Message(4, "Phạm Thị D", "Hình ảnh đã được gửi.", "Hôm qua", R.drawable.ic_profile, false),
        Message(5, "Hoàng Văn E", "Tuyệt vời! Tôi sẽ xem ngay.", "Thứ 2", R.drawable.ic_profile, false)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tin nhắn", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { /* Xử lý sự kiện quay lại */ }) {
                        Image(painter = painterResource(id = R.drawable.back),
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },

        bottomBar = {
            BottomNavBar()
        }
    ) { padding ->
        // Sử dụng LazyColumn để tối ưu hóa hiệu suất cho danh sách dài
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color.White)
        ) {
            // Dùng hàm items để lặp qua danh sách messages và hiển thị từng MessageItem
            items(messages) { message ->
                MessageItem(
                    name = message.name,
                    message = message.lastMessage,
                    time = message.time,
                    avatarRes = message.avatarRes,
                    isUnread = message.isUnread
                )
            }
        }
    }

}


@Composable
fun MessageItem(
    name: String,
    message: String,
    time: String,
    avatarRes: Int,
    isUnread: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Xử lý sự kiện khi nhấn vào tin nhắn */ }
            .padding(horizontal = 16.dp, vertical = 12.dp), // Tinh chỉnh padding
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            // Thêm Image để hiển thị avatar
            Image(
                painter = painterResource(id = avatarRes),
                contentDescription = "Avatar của $name",
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            // Dấu chấm đỏ báo tin nhắn chưa đọc
            if (isUnread) {
                Box(
                    modifier = Modifier
                        .size(12.dp) // Tăng kích thước một chút cho dễ thấy
                        .clip(CircleShape)
                        .background(Color.Red)
                        .align(Alignment.TopEnd)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(name, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1)
            Text(message, color = Color.Gray, fontSize = 14.sp, maxLines = 1)
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(time, color = Color.Gray, fontSize = 13.sp)
    }
}




@Preview(showBackground = true)
@Composable
fun MessageListScreenPreview() {
    MessageListScreen()
}
