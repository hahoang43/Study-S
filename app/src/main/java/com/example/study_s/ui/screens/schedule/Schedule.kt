package com.example.study_s.ui.screens.schedule

import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.study_s.ui.navigation.Routes
import com.example.study_s.ui.screens.components.BottomNavBar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*

data class ScheduleEvent(
    val day: Int,
    val month: Int,
    val year: Int,
    val subject: String,
    val timeStart: String,
    val timeEnd: String,
    val id: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(navController: NavHostController) {

    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "unknown_user"
    val scope = rememberCoroutineScope()

    val today = Calendar.getInstance()
    var currentMonth by remember { mutableStateOf(today.get(Calendar.MONTH)) }
    var currentYear by remember { mutableStateOf(today.get(Calendar.YEAR)) }
    var selectedDay by remember { mutableStateOf(today.get(Calendar.DAY_OF_MONTH)) }

    var showDialog by remember { mutableStateOf(false) }
    var noteText by remember { mutableStateOf("") }
    var timeStart by remember { mutableStateOf("08:00") }
    var timeEnd by remember { mutableStateOf("10:00") }

    var editingEvent by remember { mutableStateOf<ScheduleEvent?>(null) }

    val notes = remember { mutableStateMapOf<String, String>() }
    var events by remember { mutableStateOf(listOf<ScheduleEvent>()) }

    val eventsOfSelectedDay by remember(events, selectedDay) {
        derivedStateOf {
            events.filter { it.day == selectedDay }
        }
    }

    // 🔥 LOAD FIRESTORE
    suspend fun loadEvents() {
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
            val ts = doc["timeStart"] as? String ?: ""
            val te = doc["timeEnd"] as? String ?: ""
            val id = doc.id

            list.add(
                ScheduleEvent(
                    day = day,
                    month = currentMonth + 1,
                    year = currentYear,
                    subject = subject,
                    timeStart = ts,
                    timeEnd = te,
                    id = id
                )
            )
            notes["$day-${currentMonth + 1}-$currentYear"] = subject
        }

        events = list
    }

    LaunchedEffect(currentMonth, currentYear) {
        loadEvents()
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 50.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Lịch học", fontSize = 24.sp, fontWeight = FontWeight.Bold)

                    IconButton(
                        onClick = {
                            editingEvent = null
                            noteText = ""
                            timeStart = "08:00"
                            timeEnd = "10:00"
                            showDialog = true
                        },
                        modifier = Modifier.size(40.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                    }
                }
            }
        },

        bottomBar = { BottomNavBar(navController = navController, currentRoute = Routes.Schedule) }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {

            // 🟧 KHUNG LỊCH
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, Color.Black, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {

                Column {
                    // Thanh đổi tháng
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = {
                            if (currentMonth == 0) {
                                currentMonth = 11
                                currentYear -= 1
                            } else currentMonth -= 1
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }

                        Text(
                            "Tháng ${currentMonth + 1} năm $currentYear",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )

                        IconButton(onClick = {
                            if (currentMonth == 11) {
                                currentMonth = 0
                                currentYear += 1
                            } else currentMonth += 1
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                        }
                    }

                    CalendarView(
                        month = currentMonth,
                        year = currentYear,
                        selectedDay = selectedDay,
                        notes = notes,
                        onDaySelected = { selectedDay = it }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(
                "Sự kiện ngày $selectedDay/${currentMonth + 1}/$currentYear",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(12.dp))

            if (eventsOfSelectedDay.isEmpty()) {
                Text("Không có sự kiện nào", color = Color.Gray)
            } else {

                LazyColumn {
                    items(eventsOfSelectedDay, key = { it.id }) { event ->

                        var showDeleteDialog by remember { mutableStateOf(false) }

                        if (showDeleteDialog) {
                            AlertDialog(
                                onDismissRequest = { showDeleteDialog = false },
                                title = { Text("Xoá lịch") },
                                text = { Text("Bạn chắc chắn muốn xoá?") },
                                confirmButton = {
                                    TextButton(onClick = {
                                        db.collection("schedules")
                                            .document(event.id)
                                            .delete()

                                        // 🔥 Xoá realtime
                                        events = events.filter { it.id != event.id }
                                        notes.remove("${event.day}-${event.month}-${event.year}")

                                        showDeleteDialog = false
                                    }) {
                                        Text("Xoá")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDeleteDialog = false }) {
                                        Text("Hủy")
                                    }
                                }
                            )
                        }

                        // ⭐ CARD SỰ KIỆN ĐẸP
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .shadow(4.dp, RoundedCornerShape(16.dp))
                                .clickable {
                                    editingEvent = event
                                    noteText = event.subject
                                    timeStart = event.timeStart
                                    timeEnd = event.timeEnd
                                    selectedDay = event.day
                                    showDialog = true
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {

                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                val icon = when {
                                    event.subject.contains("bài giảng", true) -> Icons.Default.School
                                    event.subject.contains("tự học", true) -> Icons.Default.MenuBook
                                    event.subject.contains("thí nghiệm", true) -> Icons.Default.Science
                                    else -> Icons.Default.EventNote
                                }

                                Icon(
                                    icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )

                                Spacer(Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {

                                    Text(
                                        event.subject,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Spacer(Modifier.height(6.dp))

                                    Box(
                                        modifier = Modifier
                                            .border(
                                                1.dp,
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            "${event.timeStart} - ${event.timeEnd}",
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 13.sp
                                        )
                                    }
                                }

                                IconButton(onClick = { showDeleteDialog = true }) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
                                }
                            }
                        }
                    }
                }
            }
        }

        // =======================
        // 🟩 DIALOG THÊM / SỬA
        // =======================
        if (showDialog) {

            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text(if (editingEvent == null) "Thêm ghi chú" else "Sửa ghi chú") },
                confirmButton = {
                    TextButton(onClick = {

                        if (noteText.isBlank()) {
                            Toast.makeText(context, "Bạn chưa nhập nội dung!", Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }

                        if (editingEvent == null) {
                            // ⭐ THÊM MỚI – cập nhật realtime
                            val data = hashMapOf(
                                "userId" to userId,
                                "year" to currentYear,
                                "month" to currentMonth + 1,
                                "day" to selectedDay,
                                "subject" to noteText,
                                "timeStart" to timeStart,
                                "timeEnd" to timeEnd
                            )

                            db.collection("schedules")
                                .add(data)
                                .addOnSuccessListener { doc ->

                                    // ⭐ Thêm vào UI ngay
                                    val newEvent = ScheduleEvent(
                                        selectedDay,
                                        currentMonth + 1,
                                        currentYear,
                                        noteText,
                                        timeStart,
                                        timeEnd,
                                        doc.id
                                    )

                                    events = events + newEvent
                                    notes["$selectedDay-${currentMonth + 1}-$currentYear"] = noteText
                                }

                        } else {
                            // ⭐ UPDATE – realtime
                            db.collection("schedules")
                                .document(editingEvent!!.id)
                                .update(
                                    mapOf(
                                        "subject" to noteText,
                                        "timeStart" to timeStart,
                                        "timeEnd" to timeEnd
                                    )
                                )

                            // cập nhật local UI
                            events = events.map {
                                if (it.id == editingEvent!!.id)
                                    it.copy(subject = noteText, timeStart = timeStart, timeEnd = timeEnd)
                                else it
                            }

                            notes["$selectedDay-${currentMonth + 1}-$currentYear"] = noteText
                        }

                        showDialog = false

                    }) {
                        Text(if (editingEvent == null) "Lưu" else "Cập nhật")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) { Text("Hủy") }
                },
                text = {
                    Column {

                        Text("Ngày: $selectedDay/${currentMonth + 1}/$currentYear")

                        Spacer(Modifier.height(10.dp))

                        OutlinedTextField(
                            value = noteText,
                            onValueChange = { noteText = it },
                            label = { Text("Nội dung ghi chú") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(16.dp))

                        // Giờ bắt đầu
                        Text("Giờ bắt đầu: $timeStart")
                        Button(
                            onClick = {
                                val cal = Calendar.getInstance()
                                TimePickerDialog(
                                    context,
                                    { _, h, m -> timeStart = "%02d:%02d".format(h, m) },
                                    cal.get(Calendar.HOUR_OF_DAY),
                                    cal.get(Calendar.MINUTE),
                                    true
                                ).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Chọn giờ bắt đầu")
                        }

                        Spacer(Modifier.height(12.dp))

                        // Giờ kết thúc
                        Text("Giờ kết thúc: $timeEnd")
                        Button(
                            onClick = {
                                val cal = Calendar.getInstance()
                                TimePickerDialog(
                                    context,
                                    { _, h, m -> timeEnd = "%02d:%02d".format(h, m) },
                                    cal.get(Calendar.HOUR_OF_DAY),
                                    cal.get(Calendar.MINUTE),
                                    true
                                ).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Chọn giờ kết thúc")
                        }
                    }
                }
            )
        }
    }
}

// ==============================
// ⭐ CALENDAR VIEW
// ==============================
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

    Column(modifier = Modifier.fillMaxWidth()) {

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            daysOfWeek.forEach {
                Text(
                    it,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        var dayCounter = 1

        for (week in 0 until 6) {
            Row(modifier = Modifier.fillMaxWidth()) {

                for (dow in 1..7) {

                    val isBefore = week == 0 && dow < firstDayOfWeek
                    val isAfter = dayCounter > totalDays

                    if (isBefore || isAfter) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                        )
                    } else {
                        val day = dayCounter
                        val key = "$day-${month + 1}-$year"
                        val hasNote = notes.containsKey(key)
                        val isSelected = day == selectedDay
                        val isToday =
                            day == today.get(Calendar.DAY_OF_MONTH) &&
                                    month == today.get(Calendar.MONTH) &&
                                    year == today.get(Calendar.YEAR)

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else Color.Transparent
                                )
                                .then(
                                    if (isToday)
                                        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    else Modifier
                                )
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) { onDaySelected(day) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                day.toString(),
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.onPrimary
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )

                            if (hasNote) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .size(6.dp)
                                        .background(MaterialTheme.colorScheme.error, CircleShape)
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
