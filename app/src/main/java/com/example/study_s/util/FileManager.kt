package com.example.study_s.util
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import kotlinx.coroutines.withContext

// ✅ BIẾN HÀM NÀY THÀNH "suspend fun"
suspend fun downloadFile(context: Context, url: String, fileName: String, fileType: String?) {
    // Với Android 10 (Q) trở lên
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        // ✅ CHUYỂN TOÀN BỘ LOGIC VÀO LUỒNG I/O
        withContext(Dispatchers.IO) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, fileType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

            if (uri != null) {
                try {
                    // Thao tác mạng bây giờ đã an toàn bên trong Dispatchers.IO
                    URL(url).openStream().use { inputStream ->
                        resolver.openOutputStream(uri)?.use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    // ✅ Hiển thị thông báo khi thành công (chạy lại trên Main thread)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Đã tải xong: $fileName", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    resolver.delete(uri, null, null) // Dọn dẹp nếu có lỗi
                    // ✅ Hiển thị thông báo lỗi (chạy lại trên Main thread)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Tải file thất bại", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    } else {
        // Logic cho Android cũ hơn với DownloadManager không cần thay đổi
        // vì nó đã tự chạy trên luồng nền.
        @Suppress("DEPRECATION")
        val request = DownloadManager.Request(Uri.parse(url))
            // ... (giữ nguyên phần này) ...
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)
    }
}


// Hàm để mở một file đã được tải về
fun openFile(context: Context, fileName: String) {
    // Đường dẫn đến thư mục Downloads
    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    val file = File(downloadsDir, fileName)

    if (file.exists()) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val mimeType = context.contentResolver.getType(uri)

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Xử lý trường hợp không có ứng dụng nào có thể mở file
        }
    }
}

// Hàm để kiểm tra file đã tồn tại trong thư mục Downloads chưa
fun isFileDownloaded(fileName: String): Boolean {
    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    val file = File(downloadsDir, fileName)
    return file.exists()
}
