package com.example.study_s.ui.screens.schedule

import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.study_s.R
import com.example.study_s.ui.navigation.Routes
import com.example.study_s.ui.screens.components.BottomNavBar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.util.*

data class ScheduleEvent(
    val day: Int,
    val month: Int,
    val year: Int,
    val subject: String,
    val timeStart: String,
    val timeEnd: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(navController: NavHostController) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "unknown_user"

    var currentMonth by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MONTH)) }
    var currentYear by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var selectedDay by remember { mutableStateOf<Int?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var noteText by remember { mutableStateOf("") }
    var timeStart by remember { mutableStateOf("08:00") }
    var timeEnd by remember { mutableStateOf("10:00") }

    val notes = remember { mutableStateMapOf<String, String>() }
    var events by remember { mutableStateOf(listOf<ScheduleEvent>()) }

    // 🔁 Load dữ liệu Firestore khi đổi tháng/năm
    LaunchedEffect(currentMonth, currentYear) {
        try {
            val snapshot = db.collection("schedules")
                .whereEqualTo("userId", userId)
                .whereEqualTo("month", currentMonth + 1)
                .whereEqualTo("year", currentYear)
                .orderBy("day", Query.Direction.ASCENDING)
                .get()
                .await()

            val list = mutableListOf<ScheduleEvent>()
            notes.clear()

            for (doc in snapshot.documents) {
                val day = (doc["day"] as? Long)?.toInt() ?: continue
                val subject = doc["subject"] as? String ?: ""
                val timeStartDb = doc["timeStart"] as? String ?: ""
                val timeEndDb = doc["timeEnd"] as? String ?: ""
                list.add(ScheduleEvent(day, currentMonth + 1, currentYear, subject, timeStartDb, timeEndDb))
                notes["$day-${currentMonth + 1}-$currentYear"] = subject
            }
            events = list
        } catch (e: Exception) {
            Toast.makeText(context, "Lỗi tải dữ liệu: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            "Study-S",
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = { }) {
                            BadgedBox(badge = {
                                Badge(containerColor = Color.Red) {
                                    Text("5", color = Color.White, fontSize = 10.sp)
                                }
                            }) {
                                Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                            }
                        }
                        IconButton(onClick = { }) {
                            Image(
                                painter = painterResource(id = R.drawable.hinh_avatar),
                                contentDescription = "Avatar",
                                modifier = Modifier.size(36.dp).clip(CircleShape)
                            )
                        }
                    }
                )

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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous month")
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
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next month")
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedDay != null) showDialog = true
                    else Toast.makeText(context, "Hãy chọn ngày trước khi thêm chú thích!", Toast.LENGTH_SHORT).show()
                },
                containerColor = Color(0xFF1976D2)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add note", tint = Color.White)
            }
        },
        bottomBar = { BottomNavBar(navController = navController, currentRoute = Routes.Schedule) } // ✅ Giữ thanh dưới
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color.White)
                .padding(horizontal = 20.dp)
        ) {
            CalendarView(
                month = currentMonth,
                year = currentYear,
                selectedDay = selectedDay,
                notes = notes,
                onDaySelected = { day -> selectedDay = day }
            )

            Spacer(modifier = Modifier.height(20.dp))
            Text("Sự kiện", fontWeight = FontWeight.Bold, fontSize = 16.sp)

            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(events) { event ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Ngày ${event.day}/${event.month}/${event.year}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(event.subject, fontSize = 14.sp)
                            Text("${event.timeStart} - ${event.timeEnd}", color = Color.Gray, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        if (showDialog && selectedDay != null) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                confirmButton = {
                    TextButton(onClick = {
                        val key = "$selectedDay-${currentMonth + 1}-$currentYear"
                        notes[key] = noteText
                        val data = hashMapOf(
                            "userId" to userId,
                            "year" to currentYear,
                            "month" to currentMonth + 1,
                            "day" to selectedDay,
                            "subject" to noteText,
                            "timeStart" to timeStart,
                            "timeEnd" to timeEnd
                        )

                        db.collection("schedules").add(data)
                            .addOnSuccessListener {
                                Toast.makeText(context, "Đã lưu ghi chú cho ngày $selectedDay/${currentMonth + 1}/$currentYear", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Lỗi khi lưu Firestore!", Toast.LENGTH_SHORT).show()
                            }

                        showDialog = false
                        noteText = ""
                    }) { Text("Lưu") }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) { Text("Hủy") }
                },
                title = { Text("Thêm chú thích") },
                text = {
                    Column {
                        Text("Ngày được chọn: $selectedDay/${currentMonth + 1}/$currentYear")
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = noteText,
                            onValueChange = { noteText = it },
                            label = { Text("Nội dung chú thích") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Button(onClick = {
                                val cal = Calendar.getInstance()
                                TimePickerDialog(context, { _, hour, minute ->
                                    timeStart = String.format("%02d:%02d", hour, minute)
                                }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
                            }) { Text("Giờ bắt đầu: $timeStart") }

                            Button(onClick = {
                                val cal = Calendar.getInstance()
                                TimePickerDialog(context, { _, hour, minute ->
                                    timeEnd = String.format("%02d:%02d", hour, minute)
                                }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
                            }) { Text("Giờ kết thúc: $timeEnd") }
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun CalendarView(
    month: Int,
    year: Int,
    selectedDay: Int?,
    notes: Map<String, String>,
    onDaySelected: (Int) -> Unit
) {
    val daysOfWeek = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    val calendar = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    val totalDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    val today = Calendar.getInstance()
    val todayDay = today.get(Calendar.DAY_OF_MONTH)
    val todayMonth = today.get(Calendar.MONTH)
    val todayYear = today.get(Calendar.YEAR)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            daysOfWeek.forEach {
                Text(it, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        var dayCounter = 1
        for (week in 0 until 6) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                for (dayOfWeek in 1..7) {
                    val isBefore = (week == 0 && dayOfWeek < firstDayOfWeek)
                    val isAfter = dayCounter > totalDays
                    if (isBefore || isAfter) {
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val day = dayCounter
                        val keyStr = "$day-${month + 1}-$year"
                        val hasNote = notes.containsKey(keyStr)
                        val isSelected = (day == selectedDay)
                        val isToday = (day == todayDay && month == todayMonth && year == todayYear)

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .background(if (isSelected) Color(0xFF1976D2) else Color.Transparent)
                                .then(
                                    if (isToday) Modifier.border(2.dp, Color(0xFF9E9E9E), CircleShape)
                                    else Modifier
                                )
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) { onDaySelected(day) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(day.toString(), color = if (isSelected) Color.White else Color.Black)
                            if (hasNote) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .size(6.dp)
                                        .background(Color.Red, CircleShape)
                                )
                            }
                        }
                        dayCounter++
                    }
                }
            }
        }
    }
}
