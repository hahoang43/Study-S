package com.example.study_s

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.util.Random

private const val FCM_DEBUG_TAG = "FCM_DEBUG_SERVICE"

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d(FCM_DEBUG_TAG, "--- onMessageReceived CALLED! ---")
        Log.d(FCM_DEBUG_TAG, "Data payload: " + remoteMessage.data.toString())

        val type = remoteMessage.data["type"]

        if (type == "new_message") {
            val senderName = remoteMessage.data["senderName"]
            val messageContent = remoteMessage.data["messageContent"]
            val targetUserId = remoteMessage.data["senderId"] // The person who sent the message is the target for navigation

            if (senderName != null && messageContent != null && targetUserId != null) {
                sendNewMessageNotification(senderName, messageContent, targetUserId)
            }
        } else {
            val title = remoteMessage.notification?.title
            val body = remoteMessage.notification?.body

            if (title != null && body != null) {
                showNotification(title, body)

                val shouldSave = remoteMessage.data["saveToHistory"]
                if (shouldSave == "true") {
                    Log.d(FCM_DEBUG_TAG, "Condition MET. Attempting to save to history...")
                    saveNotificationToHistory(title, body)
                }
            }
        }
    }

    private fun sendNewMessageNotification(senderName: String, messageContent: String, targetUserId: String) {
        val channelId = "new_message_channel"
        val channelName = "New Messages"

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("route", "chat/{targetUserId}")
            putExtra("targetUserId", targetUserId)
        }

        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this,
            Random().nextInt(), // Use a random request code to create a unique PendingIntent
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with your app icon
            .setContentTitle(senderName)
            .setContentText(messageContent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(Random().nextInt(), notificationBuilder.build())
        }
    }

    private fun saveNotificationToHistory(title: String, body: String) {
        val currentUserId = Firebase.auth.currentUser?.uid

        if (currentUserId == null) {
            Log.e(FCM_DEBUG_TAG, "SAVE FAILED: userId is NULL. User must be logged in to receive history.")
            return
        }

        Log.d(FCM_DEBUG_TAG, "SAVE ATTEMPT: Saving for userId: $currentUserId")

        val notificationPayload = hashMapOf(
            "userId" to currentUserId,            
            "type" to "SYSTEM_ADMIN",             
            "title" to title,                     
            "body" to body,                       
            "message" to body,                    
            "isRead" to false,
            "createdAt" to FieldValue.serverTimestamp(),
            "actorId" to null,
            "actorName" to "Study-S",
            "actorAvatarUrl" to null,
            "postId" to null,
            "postImageUrl" to null
        )

        Firebase.firestore.collection("notifications")
            .add(notificationPayload)
            .addOnSuccessListener { documentReference ->
                Log.d(FCM_DEBUG_TAG, ">>> SUCCESS! Admin Notification saved to ROOT 'notifications' collection. ID: ${documentReference.id} <<<")
            }
            .addOnFailureListener { e ->
                Log.e(FCM_DEBUG_TAG, ">>> FIRESTORE SAVE FAILED! Error: ", e)
            }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM_TOKEN", "New FCM Token generated: $token")
        sendTokenToServer(token)
    }

    private fun sendTokenToServer(token: String) {
        Firebase.auth.currentUser?.uid?.let { userId ->
            val userDocRef = Firebase.firestore.collection("users").document(userId)
            userDocRef.update("fcmToken", token)
                .addOnSuccessListener {
                    Log.d("FCM_TOKEN", "FCM token updated successfully for user: $userId")
                }
                .addOnFailureListener { e ->
                    Log.e("FCM_TOKEN", "Error updating FCM token", e)
                }
        }
    }

    private fun showNotification(title: String, body: String) {
        val channelId = "default_notification_channel"
        val channelName = "Thông báo chung"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Thay bằng icon của bạn
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(this)) {
            if (ActivityCompat.checkSelfPermission(this@MyFirebaseMessagingService, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
            val notificationId = Random().nextInt()
            notify(notificationId, builder.build())
            Log.d(FCM_DEBUG_TAG, "Notification shown with ID: $notificationId")
        }
    }
}
