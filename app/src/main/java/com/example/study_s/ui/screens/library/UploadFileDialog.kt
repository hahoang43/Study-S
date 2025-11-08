package com.example.study_s.ui.screens.library.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class UploadFileInfo(
    val uriString: String, // URI tạm thời của file đã chọn
    val fileName: String,
    val mimeType: String
)

@Composable
fun SimpleUploadLinkDialog(
    fileInfo: UploadFileInfo,
    onDismiss: () -> Unit,
    onConfirmUpload: (fileName: String, fileUrl: String, mimeType: String) -> Unit
) {
    var fileUrl by remember { mutableStateOf("") }

    // Sử dụng tên file và loại file đã được lấy tự động
    val fileName = fileInfo.fileName
    val mimeType = fileInfo.mimeType

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tải Lên File: $fileName") },
        text = {
            Column {
                Text(
                    "Đã nhận file: $fileName (${fileInfo.mimeType}). Vui lòng dán Link Chia sẻ Công khai (Google Drive/Dropbox).",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = fileUrl,
                    onValueChange = { fileUrl = it },
                    label = { Text("Link Chia Sẻ Công Khai (Bắt buộc)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (fileUrl.isNotBlank()) {
                        onConfirmUpload(fileName.trim(), fileUrl.trim(), mimeType)
                        onDismiss()
                    }
                },
                enabled = fileUrl.isNotBlank()
            ) {
                Text("Tải Lên")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}