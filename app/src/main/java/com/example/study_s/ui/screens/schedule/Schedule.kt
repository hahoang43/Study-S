// ĐƯỜNG DẪN: app/src/main/java/com/example/study_s/ui/screens/schedule/ScheduleScreen.kt
// PHIÊN BẢN KẾT HỢP GIAO DIỆN CŨ VÀ VIEWMODEL MỚI

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.study_s.data.model.ScheduleModel
import com.example.study_s.ui.navigation.Routes
import com.example.study_s.ui.screens.components.BottomNavBar
import com.example.study_s.viewmodel.ReminderOption
import com.example.study_s.viewmodel.ScheduleViewModel
import com.google.firebase.auth.FirebaseAuth
import java.util.*

// Model ScheduleModel và Enum ReminderOption đã được chuyển sang file riêng,
// không cần khai báo lại ở đây.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    navController: NavHostController,
    // ✅ 1. KHỞI TẠO VIEWMODEL
    viewModel: ScheduleViewModel = viewModel()
) {
    // ✅ 2. LẤY DỮ LIỆU HOÀN TOÀN TỪ VIEWMODEL
    val currentMonth by viewModel.currentMonth.collectAsState()
    val currentYear by viewModel.currentYear.collectAsState()
    val events by viewModel.events.collectAsState()
    val scheduleToRemind by viewModel.showReminderDialog

    // State cho các UI tạm thời (vẫn giữ lại vì nó thuộc về UI)
    val todayCalendar = remember { Calendar.getInstance() }
    var selectedDay by remember { mutableStateOf(todayCalendar.get(Calendar.DAY_OF_MONTH)) }
    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<ScheduleModel?>(null) }


    // Lọc ra các sự kiện của ngày được chọn từ State của ViewModel
    val eventsOfSelectedDay by remember(events, selectedDay, currentMonth, currentYear) {
        derivedStateOf {
            events.filter { event ->
                event.day == selectedDay && event.month == currentMonth + 1 && event.year == currentYear
            }
        }
    }

    Scaffold(
        topBar = {
            TopBar(onAddClick = {
                editingEvent = null // Chế độ "Thêm mới"
                showAddEditDialog = true
            })
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
            CalendarCard(
                currentMonth = currentMonth,
                currentYear = currentYear,
                events = events,
                selectedDay = selectedDay,
                onMonthChange = { newMonth, newYear ->
                    // ✅ 3. GỌI VIEWMODEL ĐỂ THAY ĐỔI THÁNG
                    viewModel.changeMonth(newMonth, newYear)
                    // Cập nhật selectedDay nếu chuyển sang tháng/năm hiện tại
                    if (newYear == todayCalendar.get(Calendar.YEAR) && newMonth == todayCalendar.get(Calendar.MONTH)) {
                        selectedDay = todayCalendar.get(Calendar.DAY_OF_MONTH)
                    } else {
                        selectedDay = 1
                    }
                },
                onDaySelected = { day -> selectedDay = day }
            )

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
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(eventsOfSelectedDay, key = { it.scheduleId }) { event ->
                        EventItemCard(
                            event = event,
                            onEditClick = {
                                editingEvent = event
                                selectedDay = event.day
                                showAddEditDialog = true
                            },
                            onDeleteClick = {
                                // ✅ 4. GỌI VIEWMODEL ĐỂ XÓA
                                viewModel.deleteSchedule(event)
                            }
                        )
                    }
                }
            }
        }

        // =======================
        // 🟩 DIALOG THÊM / SỬA (AddEditEventDialog)
        // =======================
        if (showAddEditDialog) {
            AddEditEventDialog(
                editingEvent = editingEvent,
                selectedDay = selectedDay,
                currentMonth = currentMonth,
                currentYear = currentYear,
                onDismiss = { showAddEditDialog = false },
                onConfirm = { schedule ->
                    // ✅ 5. GỌI VIEWMODEL ĐỂ KÍCH HOẠT HỘP THOẠI NHẮC NHỞ
                    viewModel.onSaveOrUpdateClicked(schedule)
                    showAddEditDialog = false // Ẩn dialog thêm/sửa
                }
            )
        }

        // ==================================
        // 🟩 DIALOG CHỌN LỜI NHẮC (ReminderOptionsDialog)
        // ==================================
        scheduleToRemind?.let { schedule ->
            ReminderOptionsDialog(
                onDismiss = { viewModel.dismissReminderDialog() },
                onOptionSelected = { reminderOption ->
                    // ✅ 6. GỌI VIEWMODEL ĐỂ XỬ LÝ LỰA CHỌN VÀ ĐẶT BÁO THỨC
                    viewModel.processScheduleWithReminder(schedule, reminderOption)
                }
            )
        }
    }
}

// =======================
// CÁC COMPONENT PHỤ (Được tách ra từ code cũ của bạn, không thay đổi nhiều)
// =======================

@Composable
fun TopBar(onAddClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 50.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Lịch học", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        IconButton(
            onClick = onAddClick,
            modifier = Modifier.size(40.dp),
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            )
        ) {
            Icon(Icons.Default.Add, contentDescription = "Thêm lịch học")
        }
    }
}

@Composable
fun CalendarCard(
    currentMonth: Int,
    currentYear: Int,
    events: List<ScheduleModel>,
    selectedDay: Int,
    onMonthChange: (Int, Int) -> Unit,
    onDaySelected: (Int) -> Unit
) {
    // Tạo map để hiển thị dấu chấm có sự kiện
    val notesMap = remember(events) {
        events.associate { "${it.day}-${it.month}-${it.year}" to true }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, Color.Black, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    val cal = Calendar.getInstance().apply { set(currentYear, currentMonth, 1) }
                    cal.add(Calendar.MONTH, -1)
                    onMonthChange(cal.get(Calendar.MONTH), cal.get(Calendar.YEAR))
                }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }

                Text("Tháng ${currentMonth + 1} năm $currentYear", fontWeight = FontWeight.Bold, fontSize = 18.sp)

                IconButton(onClick = {
                    val cal = Calendar.getInstance().apply { set(currentYear, currentMonth, 1) }
                    cal.add(Calendar.MONTH, 1)
                    onMonthChange(cal.get(Calendar.MONTH), cal.get(Calendar.YEAR))
                }) { Icon(Icons.AutoMirrored.Filled.ArrowForward, null) }
            }

            CalendarView(
                month = currentMonth,
                year = currentYear,
                selectedDay = selectedDay,
                notes = notesMap,
                onDaySelected = onDaySelected
            )
        }
    }
}


@Composable
fun EventItemCard(event: ScheduleModel, onEditClick: () -> Unit, onDeleteClick: () -> Unit) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Xoá lịch") },
            text = { Text("Bạn có chắc chắn muốn xoá lịch học '${event.content}'?") },
            confirmButton = { TextButton(onClick = { onDeleteClick(); showDeleteDialog = false }) { Text("Xoá") } },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Hủy") } }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .clickable(onClick = onEditClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = when {
                event.content.contains("bài giảng", true) -> Icons.Default.School
                event.content.contains("tự học", true) -> Icons.Default.MenuBook
                event.content.contains("thí nghiệm", true) -> Icons.Default.Science
                else -> Icons.Default.EventNote
            }
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(event.content, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    val timeString = String.format("%02d:%02d", event.hour, event.minute)
                    Text(timeString, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                }
            }
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(Icons.Default.Delete, contentDescription = "Xoá", tint = Color.Red)
            }
        }
    }
}


@Composable
fun AddEditEventDialog(
    editingEvent: ScheduleModel?,
    selectedDay: Int,
    currentMonth: Int,
    currentYear: Int,
    onDismiss: () -> Unit,
    onConfirm: (ScheduleModel) -> Unit
) {
    val context = LocalContext.current
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    var noteText by remember { mutableStateOf(editingEvent?.content ?: "") }
    var hour by remember { mutableStateOf(editingEvent?.hour ?: 8) }
    var minute by remember { mutableStateOf(editingEvent?.minute ?: 0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editingEvent == null) "Thêm lịch học" else "Sửa lịch học") },
        confirmButton = {
            TextButton(onClick = {
                if (noteText.isBlank()) {
                    Toast.makeText(context, "Vui lòng nhập nội dung!", Toast.LENGTH_SHORT).show()
                    return@TextButton
                }
                // Tạo hoặc cập nhật đối tượng ScheduleModel
                val schedule = editingEvent?.copy(
                    content = noteText,
                    hour = hour,
                    minute = minute
                ) ?: ScheduleModel(
                    userId = userId, // userId được gán ở đây
                    content = noteText,
                    year = currentYear,
                    month = currentMonth + 1, // Tháng lưu vào DB là 1-12
                    day = selectedDay,
                    hour = hour,
                    minute = minute
                )
                onConfirm(schedule)
            }) {
                Text(if (editingEvent == null) "Lưu" else "Cập nhật")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } },
        text = {
            Column {
                Text("Ngày: $selectedDay/${currentMonth + 1}/$currentYear")
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Nội dung") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Text("Thời gian: %02d:%02d".format(hour, minute))
                Button(
                    onClick = {
                        TimePickerDialog(context, { _, h, m -> hour = h; minute = m }, hour, minute, true).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Chọn thời gian")
                }
            }
        }
    )
}

// Dialog chọn lời nhắc (không thay đổi)
@Composable
fun ReminderOptionsDialog(onDismiss: () -> Unit, onOptionSelected: (ReminderOption) -> Unit) {
    val options = ReminderOption.values()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Đặt lời nhắc") },
        text = {
            Column {
                options.forEach { option ->
                    Text(
                        text = option.description,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOptionSelected(option) }
                            .padding(vertical = 16.dp)
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}


@Composable
fun CalendarView(month: Int, year: Int, selectedDay: Int, notes: Map<String, Boolean>, onDaySelected: (Int) -> Unit) {
    val daysOfWeek = listOf("CN", "T2", "T3", "T4", "T5", "T6", "T7")
    val calendar = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) // 1=CN, 2=T2,...
    val totalDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val today = Calendar.getInstance()

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            daysOfWeek.forEach { Text(it, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f)) }
        }
        Spacer(Modifier.height(8.dp))
        var dayCounter = 1
        for (week in 0 until 6) {
            if (dayCounter > totalDays) break
            Row(modifier = Modifier.fillMaxWidth()) {
                for (dow in 1..7) {
                    if (week == 0 && dow < firstDayOfWeek || dayCounter > totalDays) {
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val day = dayCounter
                        val key = "$day-${month + 1}-$year"
                        val hasNote = notes.containsKey(key)
                        val isSelected = day == selectedDay
                        val isToday = day == today.get(Calendar.DAY_OF_MONTH) && month == today.get(Calendar.MONTH) && year == today.get(Calendar.YEAR)

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .then(if (isToday) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier)
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) { onDaySelected(day) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(day.toString(), color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                            if (hasNote) {
                                Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp).size(6.dp).background(MaterialTheme.colorScheme.error, CircleShape))
                            }
                        }
                        dayCounter++
                    }
                }
            }
        }
    }
}
