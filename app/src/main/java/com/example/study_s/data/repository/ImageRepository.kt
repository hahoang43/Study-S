// BẮT ĐẦU FILE: data/repository/ImageRepository.kt
package com.example.study_s.data.repository // Đảm bảo đúng package

import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ImageRepository {

    /**
     * Tải một file ảnh lên Cloudinary bằng phương thức "unsigned".
     * Đây là một suspend function, có thể gọi an toàn trong một Coroutine.
     *
     * @param imageUri Uri của file ảnh trên thiết bị (lấy từ thư viện ảnh).
     * @return Một đối tượng Result chứa URL của ảnh đã tải lên hoặc một Exception nếu thất bại.
     */
    suspend fun uploadImage(imageUri: Uri): Result<String> {
        // suspendCancellableCoroutine giúp biến một callback thành một suspend function
        // một cách an toàn, và có thể hủy được.
        return suspendCancellableCoroutine { continuation ->

            // Lấy requestId để có thể hủy nếu coroutine bị hủy
            val requestId = MediaManager.get()
                .upload(imageUri)
                // "ml_default" là tên mặc định của upload preset không cần chữ ký.
                // Đây là dòng cực kỳ quan trọng, cho phép client tự tải ảnh lên.
                .unsigned("Study_S")
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {
                        // Callback khi bắt đầu tải lên, có thể dùng để hiện ProgressBar
                    }

                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                        // Callback trong quá trình tải, hữu ích để cập nhật tiến trình
                    }

                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        // Tải lên thành công!
                        // Dữ liệu trả về là một Map, ta cần lấy URL từ key "secure_url".
                        val secureUrl = resultData["secure_url"] as? String

                        if (secureUrl != null && continuation.isActive) {
                            // Nếu lấy được URL và coroutine còn hoạt động, trả về thành công
                            continuation.resume(Result.success(secureUrl))
                        } else if (continuation.isActive) {
                            // Nếu không có URL, trả về lỗi
                            continuation.resumeWithException(Exception("Tải ảnh thành công nhưng không tìm thấy URL."))
                        }
                    }

                    override fun onError(requestId: String, error: ErrorInfo) {
                        // Tải lên thất bại
                        if (continuation.isActive) {
                            continuation.resumeWithException(Exception("Tải ảnh thất bại: ${error.description}"))
                        }
                    }

                    override fun onReschedule(requestId: String, error: ErrorInfo) {
                        // Được gọi khi quá trình tải được lên lịch lại (ví dụ: mất mạng)
                    }
                }).dispatch() // Bắt đầu thực hiện yêu cầu tải lên

            // Nếu coroutine bị hủy (ví dụ người dùng thoát màn hình), hủy luôn yêu cầu tải lên
            continuation.invokeOnCancellation {
                MediaManager.get().cancelRequest(requestId)
            }
        }
    }
}
// KẾT THÚC FILE
