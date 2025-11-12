package com.example.study_s.ui.screens.library

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.study_s.data.repository.LibraryRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.util.UUID
import kotlin.math.log10
import kotlin.math.pow

data class FileUploadInfo(
    val uri: Uri,
    var displayName: String,
    val size: Long,
    val mimeType: String,
    var progress: Int = 0,
    var isUploading: Boolean = false,
    val id: UUID = UUID.randomUUID()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadFileScreen(
    navController: NavController,
    fileUrl: String?,
    fileName: String?
) {
    val context = LocalContext.current
    val fileItems = remember { mutableStateListOf<FileUploadInfo>() }
    var isUploading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val libraryRepository = remember { LibraryRepository() }
    val currentUser = remember { FirebaseAuth.getInstance().currentUser }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
        onResult = { uris: List<Uri> ->
            val newFiles = uris.mapNotNull { uri ->
                val name = getFileName(context, uri)
                val size = getFileSize(context, uri)
                val mimeType = context.contentResolver.getType(uri)
                if (name != null && size != null && mimeType != null) {
                    FileUploadInfo(uri = uri, displayName = name, size = size, mimeType = mimeType)
                } else null
            }
            fileItems.addAll(newFiles)
        }
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { TopBar(navController = navController, title = "Tải lên tệp") },
        bottomBar = {
            if (fileItems.isNotEmpty()) {
                UploadActionBar(
                    fileCount = fileItems.size,
                    totalSize = fileItems.sumOf { it.size },
                    isUploading = isUploading,
                    onUploadClick = {
                        if (currentUser?.uid == null) {
                            Toast.makeText(context, "Vui lòng đăng nhập để tải lên", Toast.LENGTH_LONG).show()
                            return@UploadActionBar
                        }
                        isUploading = true
                        scope.launch {
                            val uploaderId = currentUser.uid
                            val uploaderName = currentUser.displayName?.takeIf { it.isNotBlank() }
                                ?: currentUser.providerData.firstNotNullOfOrNull { it.displayName?.takeIf { it.isNotBlank() } }
                                ?: currentUser.email
                                ?: "Người dùng ẩn danh"

                            fileItems.forEachIndexed { index, fileInfo ->
                                launch {
                                    try {
                                        fileItems[index] = fileInfo.copy(isUploading = true)
                                        libraryRepository.uploadFile(
                                            context = context,
                                            fileUri = fileInfo.uri,
                                            fileName = fileInfo.displayName,
                                            mimeType = fileInfo.mimeType,
                                            uploaderId = uploaderId,
                                            uploaderName = uploaderName,
                                            onProgress = { progress ->
                                                fileItems[index] = fileItems[index].copy(progress = progress)
                                            }
                                        )
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Lỗi tải lên ${fileInfo.displayName}: ${e.message}", Toast.LENGTH_LONG).show()
                                    } finally {
                                        fileItems[index] = fileItems[index].copy(isUploading = false)
                                    }
                                }
                            }
                        }.invokeOnCompletion {
                            isUploading = false
                            if (it == null) { // Coroutine finished without cancellation/error
                                Toast.makeText(context, "Tất cả đã được tải lên!", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            }
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                AddFileButton(onClick = { filePickerLauncher.launch("*/*") })
            }
            itemsIndexed(fileItems, key = { _, item -> item.id }) { index, fileInfo ->
                FileCard(
                    fileInfo = fileInfo,
                    onRemove = { fileItems.removeAt(index) },
                    onNameChange = { newName ->
                        fileItems[index] = fileItems[index].copy(displayName = newName)
                    },
                    isUploading = isUploading
                )
            }
        }
    }
}

@Composable
fun AddFileButton(onClick: () -> Unit) {
    val buttonColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable(onClick = onClick)
            .border(BorderStroke(2.dp, buttonColor), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Add, contentDescription = "Thêm tệp", tint = buttonColor)
            Text("Thêm tệp", color = buttonColor)
        }
    }
}

@Composable
fun FileCard(fileInfo: FileUploadInfo, onRemove: () -> Unit, onNameChange: (String) -> Unit, isUploading: Boolean) {
    var isEditing by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isEditing) {
        if (isEditing) {
            focusRequester.requestFocus()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FileIcon(mimeType = fileInfo.mimeType, uri = fileInfo.uri)
            Column(modifier = Modifier.weight(1f)) {
                if (isEditing) {
                    BasicTextField(
                        value = fileInfo.displayName,
                        onValueChange = onNameChange,
                        textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                } else {
                    Text(fileInfo.displayName, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(formatFileSize(fileInfo.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { isEditing = !isEditing }, enabled = !isUploading) {
                Icon(Icons.Default.Edit, contentDescription = "Đổi tên")
            }
            IconButton(onClick = onRemove, enabled = !isUploading) {
                Icon(Icons.Default.Delete, contentDescription = "Xóa")
            }
        }
        if (fileInfo.isUploading) {
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { fileInfo.progress / 100f },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun FileIcon(mimeType: String, uri: Uri) {
    Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
        if (mimeType.startsWith("image/")) {
            AsyncImage(model = uri, contentDescription = "Preview", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(4.dp)))
        } else {
            val icon = when {
                mimeType.contains("pdf") -> Icons.Default.PictureAsPdf
                mimeType.contains("word") -> Icons.Default.Description
                mimeType.contains("presentation") || mimeType.contains("powerpoint") -> Icons.Default.Slideshow
                else -> Icons.Default.Folder
            }
            Icon(imageVector = icon, contentDescription = "File type", modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
fun UploadActionBar(fileCount: Int, totalSize: Long, isUploading: Boolean, onUploadClick: () -> Unit) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Đã chọn: $fileCount tệp", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(formatFileSize(totalSize), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
        if (isUploading) {
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onUploadClick,
            enabled = !isUploading,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(if (isUploading) "ĐANG TẢI LÊN..." else "TẢI LÊN", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.SemiBold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(navController: NavController, title: String) {
    TopAppBar(
        title = {
            Text(text = title, fontWeight = FontWeight.Bold)
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface
        ),
        navigationIcon = {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                )
            }
        }
    )
}

fun getFileName(context: Context, uri: Uri): String? {
    return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        cursor.moveToFirst()
        cursor.getString(nameIndex)
    }
}

fun getFileSize(context: Context, uri: Uri): Long? {
    return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
        cursor.moveToFirst()
        cursor.getLong(sizeIndex)
    }
}

fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt()
    if (digitGroups >= units.size) return "Large File"
    return "${DecimalFormat("#,##0.#").format(size / 1024.0.pow(digitGroups.toDouble()))} ${units[digitGroups]}"
}
