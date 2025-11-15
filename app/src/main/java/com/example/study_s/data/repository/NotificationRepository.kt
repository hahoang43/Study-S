// ĐƯỜNG DẪN: data/repository/NotificationRepository.kt

package com.example.study_s.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.withContext


import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
class NotificationRepository {

    private val client = OkHttpClient()
    private val db = FirebaseFirestore.getInstance()
    private val notificationCollection = db.collection("notifications")
    /**
     * Gửi yêu cầu tạo Push Notification đến server FCM của Google.
     * Hàm này nên được gọi từ một Coroutine Scope (ví dụ: viewModelScope).
     * @param token FCM Token của thiết bị người nhận.
     * @param title Tiêu đề của thông báo.
     * @param body Nội dung của thông báo.
     */
    suspend fun sendPushNotification(token: String, title: String, body: String) {
        // Chạy trên một thread riêng (IO) để không làm treo giao diện người dùng
        withContext(Dispatchers.IO) {
            // ✅ ĐÂY LÀ CHỖ QUAN TRỌNG NHẤT
            // Dán Server Key bạn đã lấy từ trang Firebase vào đây
            val serverKey = "BFtrM2assmv9dTohl6qDSGhiKvYek3MejMbGAuujavTOx5vcYtqLJ1mW5RnV9u3zwpspexFDYya0_o8UhDhxsQc"

            // Tạo payload JSON theo đúng định dạng của FCM
            val json = JSONObject().apply {
                put("to", token) // Gửi đến 1 thiết bị cụ thể
                val dataPayload = JSONObject().apply {
                    put("title", title)
                    put("body", body)
                }
                put("data", dataPayload) // `data` payload sẽ được nhận trong onMessageReceived
            }

            val requestBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url("https://fcm.googleapis.com/fcm/send") // Endpoint của FCM
                .post(requestBody)
                .addHeader("Authorization", "key=$serverKey") // Header xác thực
                .addHeader("Content-Type", "application/json")
                .build()

            // Thực thi request
            try {
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful) {
                    Log.d("FCM_SEND", "Notification sent successfully. Response: $responseBody")
                } else {
                    Log.e("FCM_SEND", "Failed to send notification. Code: ${response.code}, Response: $responseBody")
                }
                response.close()
            } catch (e: IOException) {
                Log.e("FCM_SEND", "Failed to send notification due to network error.", e)
            }
        }
    }
    /**
     * Hàm để lưu một thông báo mới vào Firestore.
     * Nó sẽ tự động tạo ID cho thông báo.
     */
    suspend fun saveNotificationToFirestore(notification: com.example.study_s.data.model.Notification) {
        try {
            // Tự tạo một document mới để lấy ID
            val newDocRef = notificationCollection.document()
            // Gán ID vừa tạo vào đối tượng notification và lưu nó
            newDocRef.set(notification.copy(notificationId = newDocRef.id)).await()
        } catch (e: Exception) {
            // Xử lý lỗi
            Log.e("NotificationRepo", "Error saving notification", e)
        }
    }

    /**
     * Hàm để lấy danh sách thông báo cho một người dùng cụ thể.
     */
    suspend fun getNotificationsForUser(userId: String): List<com.example.study_s.data.model.Notification> {
        return try {
            notificationCollection
                .whereEqualTo("userId", userId) // Chỉ lấy thông báo của người dùng này
                .orderBy("createdAt", Query.Direction.DESCENDING) // Sắp xếp mới nhất lên đầu
                .limit(30) // Giới hạn 30 thông báo gần nhất
                .get()
                .await()
                .toObjects(com.example.study_s.data.model.Notification::class.java)
        } catch (e: Exception) {
            emptyList() // Trả về danh sách rỗng nếu có lỗi
        }
    }
}

