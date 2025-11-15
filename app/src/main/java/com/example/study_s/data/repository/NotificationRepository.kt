// ĐƯỜNG DẪN: data/repository/NotificationRepository.kt

package com.example.study_s.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.withContext

import com.example.study_s.data.model.Notification // Sử dụng Notification model
import com.example.study_s.data.model.PostModel // Cần để lấy thông tin bài viết
import com.example.study_s.data.model.User
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
    /**
     * Gửi yêu cầu tạo Push Notification đến server FCM của Google.
     */
    private suspend fun sendPushNotification(token: String, title: String, body: String) {
        withContext(Dispatchers.IO) {
            val serverKey = "AAAAzO-kC6U:APA91bFXl_JqE_0aHk304-1-U9K1v2wLz4Bw7qZ5hX2eX5c4vX6J3yX2n_Y7mH4W6aX7jB5g_Y8a_Z7qC9rQ5zJ_X5dK6nZ3vV1k_Z5tZ_X2n_Y7mH4W6aX7jB5g_Y8a" // Thay thế bằng Server Key của bạn

            val json = JSONObject().apply {
                put("to", token)
                // FCM mới khuyến khích dùng `notification` payload để hiển thị đơn giản
                val notificationPayload = JSONObject().apply {
                    put("title", title)
                    put("body", body)
                    put("sound", "default") // Thêm âm thanh
                }
                put("notification", notificationPayload)
            }

            val requestBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url("https://fcm.googleapis.com/fcm/send")
                .post(requestBody)
                .addHeader("Authorization", "key=$serverKey")
                .addHeader("Content-Type", "application/json")
                .build()

            try {
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    Log.e("FCM_SEND", "Failed to send notification. Code: ${response.code}, Response: ${response.body?.string()}")
                }
                response.close()
            } catch (e: IOException) {
                Log.e("FCM_SEND", "Failed to send notification due to network error.", e)
            }
        }
    }

    /**
     * Hàm để lưu một thông báo mới vào Firestore.
     */
    private suspend fun saveNotificationToFirestore(notification: Notification) {
        try {
            val newDocRef = notificationCollection.document()
            newDocRef.set(notification.copy(notificationId = newDocRef.id)).await()
            Log.d("NotificationRepo", "Notification saved for user ${notification.userId}")
        } catch (e: Exception) {
            Log.e("NotificationRepo", "Error saving notification", e)
        }
    }

    /**
     * Hàm để lấy danh sách thông báo cho một người dùng cụ thể. (Giữ nguyên)
     */
    suspend fun getNotificationsForUser(userId: String): List<Notification> {
        return try {
            notificationCollection
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(30)
                .get()
                .await()
                .toObjects(Notification::class.java)
        } catch (e: Exception) {
            Log.e("NotificationRepo", "Error getting notifications", e)
            emptyList()
        }
    }


    //====================================================================
    // ✅ CÁC HÀM TIỆN ÍCH MỚI ĐỂ GỬI CÁC LOẠI THÔNG BÁO CỤ THỂ
    //====================================================================

    /**
     * Tạo và gửi thông báo "LIKE".
     * @param post Bài viết được thích.
     * @param actor Người thực hiện hành động (người like).
     * @param postOwner Chủ nhân của bài viết (người nhận thông báo).
     */
    suspend fun sendLikeNotification(post: PostModel, actor: User, postOwner: User) {
        // Không gửi thông báo nếu tự like bài của mình
        if (actor.userId == postOwner.userId) return

        val likeNotification = Notification(
            userId = postOwner.userId,
            actorId = actor.userId,
            actorName = actor.name,
            actorAvatarUrl = actor.avatarUrl,
            type = "like",
            message = "đã thích bài viết của bạn.",
            postId = post.postId,
            postImageUrl = post.imageUrl
        )

        // 1. Lưu vào Firestore
        saveNotificationToFirestore(likeNotification)

        // 2. Gửi Push Notification
        postOwner.fcmToken?.takeIf { it.isNotEmpty() }?.let { token ->
            val title = "Bài viết có tương tác mới"
            val body = "${actor.name} đã thích bài viết của bạn."
            sendPushNotification(token, title, body)
        }
    }

    /**
     * Tạo và gửi thông báo "COMMENT".
     * @param post Bài viết được bình luận.
     * @param actor Người thực hiện hành động (người bình luận).
     * @param postOwner Chủ nhân của bài viết (người nhận thông báo).
     * @param commentText Nội dung bình luận.
     */
    suspend fun sendCommentNotification(post: PostModel, actor: User, postOwner: User, commentText: String) {
        // Không gửi thông báo nếu tự bình luận bài của mình
        if (actor.userId == postOwner.userId) return

        val commentNotification = Notification(
            userId = postOwner.userId,
            actorId = actor.userId,
            actorName = actor.name,
            actorAvatarUrl = actor.avatarUrl,
            type = "comment",
            message = "đã bình luận: \"$commentText\"",
            postId = post.postId,
            postImageUrl = post.imageUrl
        )

        // 1. Lưu vào Firestore
        saveNotificationToFirestore(commentNotification)

        // 2. Gửi Push Notification
        postOwner.fcmToken?.takeIf { it.isNotEmpty() }?.let { token ->
            val title = "Bài viết có bình luận mới"
            // Giới hạn độ dài body để không quá dài
            val shortComment = if (commentText.length > 50) commentText.substring(0, 50) + "..." else commentText
            val body = "${actor.name}: $shortComment"
            sendPushNotification(token, title, body)
        }
    }

    /**
     * Tạo và gửi thông báo "FOLLOW". (Hàm này đã có trong StragerViewModel, giờ chuyển về đây cho tập trung)
     * @param actor Người đi follow.
     * @param userToNotify Người được follow (người nhận thông báo).
     */
    suspend fun sendFollowNotification(actor: User, userToNotify: User) {
        val followNotification = Notification(
            userId = userToNotify.userId,
            actorId = actor.userId,
            actorName = actor.name,
            actorAvatarUrl = actor.avatarUrl,
            type = "follow",
            message = "đã bắt đầu theo dõi bạn."
            // Không cần postId và postImageUrl cho loại "follow"
        )

        // 1. Lưu vào Firestore
        saveNotificationToFirestore(followNotification)

        // 2. Gửi Push Notification
        userToNotify.fcmToken?.takeIf { it.isNotEmpty() }?.let { token ->
            val title = "Bạn có lượt theo dõi mới!"
            val body = "${actor.name} đã bắt đầu theo dõi bạn."
            sendPushNotification(token, title, body)
        }
    }
}

