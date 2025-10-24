package com.example.study_s.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.study_s.ui.screens.components.BottomNavBar

@Composable
fun FileListScreen(navController: NavController) {
    var selectedSubject by remember { mutableStateOf("Môn học") }
    var expandedSubject by remember { mutableStateOf(false) }

    var selectedFileType by remember { mutableStateOf("File type") }
    var expandedFileType by remember { mutableStateOf(false) }

    val subjects = listOf("Tất cả", "Kinh tế vĩ mô", "Cấu trúc rời rạc")
    val fileTypes = listOf("PDF", "DOCX", "PPTX")

    val files = listOf(
        FileItem("Kinh tế vĩ mô", "PDF"),
        FileItem("Cấu trúc rời rạc", "PDF")
    )

    Scaffold(
        bottomBar = {
            BottomNavBar(
                selectedIndex = 0, // "Thư viện" là mục đầu tiên
                onItemSelected = { index ->
                    // TODO: Thêm logic điều hướng dựa trên index
                    // Ví dụ: navController.navigate(routes[index])
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // Bộ lọc trên đầu
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box {
                    OutlinedButton(
                        onClick = { expandedSubject = true },
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(selectedSubject)
                    }
                    DropdownMenu(
                        expanded = expandedSubject,
                        onDismissRequest = { expandedSubject = false }
                    ) {
                        subjects.forEach { subject ->
                            DropdownMenuItem(
                                text = { Text(subject) },
                                onClick = {
                                    selectedSubject = subject
                                    expandedSubject = false
                                }
                            )
                        }
                    }
                }

                Box {
                    OutlinedButton(
                        onClick = { expandedFileType = true },
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(selectedFileType)
                    }
                    DropdownMenu(
                        expanded = expandedFileType,
                        onDismissRequest = { expandedFileType = false }
                    ) {
                        fileTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    selectedFileType = type
                                    expandedFileType = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Danh sách file
            files.forEach { file ->
                FileCard(fileName = file.name, fileType = file.type)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun FileCard(fileName: String, fileType: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE3F2FD)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = fileType,
                        color = Color(0xFF1565C0),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = fileName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {  }) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Tải xuống",
                                tint = Color.Black
                            )
                        }
                        Text("Tải xuống", fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(16.dp))
                        IconButton(onClick = { }) {
                            Icon(
                                imageVector = Icons.Default.StarBorder,
                                contentDescription = "Lưu",
                                tint = Color.Black
                            )
                        }
                        Text("Lưu", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

data class FileItem(val name: String, val type: String)

@Preview(showBackground = true)
@Composable
fun PreviewFileListScreen() {
    FileListScreen(navController = rememberNavController())
}
