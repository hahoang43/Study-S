package com.example.study_s.ui.screens.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.study_s.ui.screens.components.BottomNavBar
import java.util.*
import com.example.study_s.R
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen() {
    var currentMonth by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MONTH)) }
    var currentYear by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR)) }

    val calendar = Calendar.getInstance()
    calendar.set(Calendar.MONTH, currentMonth)
    calendar.set(Calendar.YEAR, currentYear)

    Scaffold(
        topBar = {
            Column {
                // Thanh trên cùng
                TopAppBar(
                    title = {
                        Text(
                            "Study-S",
                            fontWeight = FontWeight.Bold,
                            fontSize = 30.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {  }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        // Icon thông báo
                        IconButton(onClick = {  }) {
                            BadgedBox(badge = {
                                Badge(containerColor = Color.Red) {
                                    Text("5", color = Color.White, fontSize = 10.sp)
                                }
                            }) {
                                Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                            }
                        }

                        // Avatar trong drawable
                        IconButton(onClick = {  }) {
                            Image(
                                painter = painterResource(id = R.drawable.hinh_avatar), // tên file ảnh
                                contentDescription = "Avatar",
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                            )
                        }
                    }

                )

                // Thanh hiển thị tháng + nút chuyển tháng
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x2C8E8E93))
                        .padding(vertical = 15.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        if (currentMonth == 0) {
                            currentMonth = 11
                            currentYear -= 1
                        } else currentMonth -= 1
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Previous month")
                    }

                    Text(
                        text = "Tháng ${currentMonth + 1} năm $currentYear",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF141515),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    IconButton(onClick = {
                        if (currentMonth == 11) {
                            currentMonth = 0
                            currentYear += 1
                        } else currentMonth += 1
                    }) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "Next month")
                    }
                }
            }
        },

    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color.White)
                .padding(horizontal = 20.dp)
        ) {
            // Lịch tháng
            CalendarView(currentMonth, currentYear)

            Spacer(modifier = Modifier.height(20.dp))

            // 🔹 Phần “Sự kiện” giả định
            Text("Sự kiện", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))

            EventItem("09:00", "Học nhóm môn Cấu trúc dữ liệu")
            EventItem("13:30", "Thực hành Lập trình mạng")
            EventItem("18:00", "Học thêm Python Online")
        }
    }
}

@Composable
fun CalendarView(month: Int, year: Int) {
    val daysOfWeek = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    val calendar = Calendar.getInstance()
    calendar.set(year, month, 1)

    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    val totalDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    Column(modifier = Modifier.fillMaxWidth()) {
        // Hàng tên thứ
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            daysOfWeek.forEach {
                Text(
                    text = it,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(8f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        var dayCounter = 1
        for (week in 0 until 6) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (dayOfWeek in 1..7) {
                    val isBeforeFirstDay = (week == 0 && dayOfWeek < firstDayOfWeek)
                    val isAfterLastDay = dayCounter > totalDays

                    if (isBeforeFirstDay || isAfterLastDay) {
                        Box(modifier = Modifier.size(36.dp))
                    } else {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    if (dayCounter == Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
                                        && month == Calendar.getInstance().get(Calendar.MONTH)
                                        && year == Calendar.getInstance().get(Calendar.YEAR)
                                    ) Color(0xFF90CAF9)
                                    else Color.Transparent,
                                    shape = MaterialTheme.shapes.small
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dayCounter.toString(),
                                textAlign = TextAlign.Center,
                                color = if (dayCounter == Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
                                    && month == Calendar.getInstance().get(Calendar.MONTH)
                                    && year == Calendar.getInstance().get(Calendar.YEAR)
                                ) Color.White else Color.Black
                            )
                        }
                        dayCounter++
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (dayCounter > totalDays) break
        }
    }
}

@Composable
fun EventItem(time: String, title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = time,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFF43636),
            modifier = Modifier.width(60.dp)
        )
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewScheduleScreen() {
    ScheduleScreen()
}
