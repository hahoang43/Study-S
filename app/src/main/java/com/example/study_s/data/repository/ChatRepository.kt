package com.example.study_s.data.repository

import android.content.Context
import android.net.Uri
// ✅ BƯỚC 1: THÊM CÁC IMPORT CẦN THIẾT
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.study_s.data.model.MessageModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume

// ✅ BƯỚC 2: ĐỊNH NGHĨA DATA CLASS ĐỂ CHỨA KẾT QUẢ
/**
 * Data class để chứa thông tin trả về từ Cloudinary sau khi upload thành công.
 */
data class CloudinaryResult(
    val url: String,
    val resourceType: String, // "image", "video", "raw"
    val originalFilename: String,
    val bytes: Long
)

class ChatRepository (private val context: Context){

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val chatsCollection = db.collection("chats")

    // ✅ BƯỚC 3: BỔ SUNG HÀM UPLOAD FILE
    /**
     * Upload một file lên Cloudinary và trả về kết quả.
     * @param fileUri URI của file cần upload.
     * @return Result chứa CloudinaryResult nếu thành công, hoặc Exception nếu thất bại.
     */
    suspend fun uploadFile(fileUri: Uri): Result<CloudinaryResult> {
        return suspendCancellableCoroutine { continuation ->
            // Sử dụng MediaManager để upload file
            val requestId = MediaManager.get().upload(fileUri)
                .callback(object : UploadCallback {
                    override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                        val url = resultData?.get("secure_url") as? String ?: ""
                        val resourceType = resultData?.get("resource_type") as? String ?: "raw"
                        val originalFilename = resultData?.get("original_filename") as? String ?: "file"
                        val bytes = (resultData?.get("bytes") as? Number)?.toLong() ?: 0L

                        val cloudinaryResult = CloudinaryResult(
                            url = url,
                            resourceType = resourceType,
                            originalFilename = originalFilename,
                            bytes = bytes
                        )
                        // Chỉ resume coroutine nếu nó còn active
                        if (continuation.isActive) {
                            continuation.resume(Result.success(cloudinaryResult))
                        }
                    }

                    override fun onError(requestId: String?, error: ErrorInfo?) {
                        if (continuation.isActive) {
                            continuation.resume(Result.failure(Exception(error?.description ?: "Cloudinary upload failed")))
                        }
                    }

                    override fun onStart(requestId: String?) { /* Có thể log hoặc cập nhật UI */ }
                    override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) { /* Cập nhật tiến trình */ }
                    override fun onReschedule(requestId: String?, error: ErrorInfo?) { /* Xử lý nếu cần */ }
                }).dispatch()

            // Hủy request upload nếu coroutine bị hủy
            continuation.invokeOnCancellation {
                MediaManager.get().cancelRequest(requestId)
            }
        }
    }


    private fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    /**
     * Lấy hoặc tạo một cuộc trò chuyện mới giữa người dùng hiện tại và người dùng mục tiêu.
     * Trả về ID của cuộc trò chuyện.
     */
    suspend fun getOrCreateChat(targetUserId: String): Result<String> {
        val currentUserId = getCurrentUserId() ?: return Result.failure(Exception("User not logged in"))
        // Sắp xếp ID để đảm bảo ID cuộc trò chuyện là duy nhất và nhất quán
        val members = listOf(currentUserId, targetUserId).sorted()
        val chatId = members.joinToString("_")

        return try {
            val chatDocRef = chatsCollection.document(chatId)
            val chatDoc = chatDocRef.get().await()

            if (!chatDoc.exists()) {
                val chatData = mapOf(
                    "members" to members,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "lastMessage" to null
                )
                chatDocRef.set(chatData).await()
            }
            Result.success(chatId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Gửi một tin nhắn mới vào cuộc trò chuyện và cập nhật lastMessage.
     * Hàm này có thể xử lý mọi loại tin nhắn (text, image, file).
     */
    suspend fun sendMessage(chatId: String, message: MessageModel): Result<Unit> {
        return try {
            val currentUserId = getCurrentUserId() ?: return Result.failure(Exception("User not logged in"))
            val targetUserId = getTargetUserId(chatId, currentUserId)

            // 1. Tạo tin nhắn với trạng thái readBy ban đầu
            val messageWithReadStatus = message.copy(
                senderId = currentUserId, // Đảm bảo senderId luôn đúng
                readBy = mapOf(currentUserId to true, targetUserId to false)
            )

            // 2. Thêm tin nhắn vào sub-collection "messages"
            val messageDocRef = chatsCollection.document(chatId)
                .collection("messages")
                .add(messageWithReadStatus)
                .await()

            // 3. Cập nhật lastMessage với ID và trạng thái đọc
            val finalMessage = messageWithReadStatus.copy(id = messageDocRef.id)
            chatsCollection.document(chatId).update("lastMessage", finalMessage).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Lắng nghe các tin nhắn trong một cuộc trò chuyện theo thời gian thực.
     */
    fun getMessages(chatId: String): Flow<List<MessageModel>> = callbackFlow {
        val listenerRegistration = chatsCollection.document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error) // Đóng flow nếu có lỗi
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    // Ánh xạ document sang object Message, bao gồm cả ID của document
                    val messages = snapshot.documents.mapNotNull { doc ->
                        doc.toObject<MessageModel>()?.copy(id = doc.id)
                    }
                    trySend(messages) // Gửi danh sách tin nhắn mới
                }
            }
        // Hủy lắng nghe khi flow bị đóng
        awaitClose { listenerRegistration.remove() }
    }

    /**
     * Xóa một tin nhắn và cập nhật lại lastMessage.
     */
    suspend fun deleteMessage(chatId: String, messageId: String): Result<Unit> {
        return try {
            val messageRef = chatsCollection.document(chatId).collection("messages").document(messageId)
            messageRef.delete().await()
            updateLastMessageAfterAction(chatId) // Cập nhật lại tin nhắn cuối cùng
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteChat(chatId: String): Result<Unit> {
        return try {
            val chatDocRef = chatsCollection.document(chatId)

            // Xóa tất cả các tin nhắn trong cuộc trò chuyện
            val messagesSnapshot = chatDocRef.collection("messages").get().await()
            val batch = db.batch()
            for (document in messagesSnapshot.documents) {
                batch.delete(document.reference)
            }
            batch.commit().await()

            // Xóa chính tài liệu trò chuyện
            chatDocRef.delete().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sửa nội dung của một tin nhắn và cập nhật lại lastMessage nếu cần.
     */
    suspend fun editMessage(chatId: String, messageId: String, newContent: String): Result<Unit> {
        return try {
            val messageRef = chatsCollection.document(chatId).collection("messages").document(messageId)
            messageRef.update("content", newContent).await()
            updateLastMessageAfterAction(chatId, messageId) // Cập nhật lại tin nhắn cuối cùng
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Cập nhật lastMessage của cuộc trò chuyện sau một hành động (sửa, xóa).
     */
    private suspend fun updateLastMessageAfterAction(chatId: String, editedMessageId: String? = null) {
        // Lấy tin nhắn cuối cùng thực tế từ sub-collection
        val lastMessageQuery = chatsCollection.document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .await()

        val lastMessage = lastMessageQuery.documents.firstOrNull()?.let { doc ->
            doc.toObject<MessageModel>()?.copy(id = doc.id)
        }

        // Nếu tin nhắn bị sửa là tin nhắn cuối cùng, chúng ta cần cập nhật lại nội dung của nó
        if (editedMessageId != null && lastMessage?.id == editedMessageId) {
            val updatedLastMessage = lastMessage.copy(content = chatsCollection.document(chatId)
                .collection("messages").document(editedMessageId).get().await()
                .getString("content") ?: lastMessage.content)
            chatsCollection.document(chatId).update("lastMessage", updatedLastMessage).await()
        } else {
            // Nếu xóa hoặc sửa tin nhắn cũ hơn, chỉ cần lấy tin nhắn cuối cùng mới nhất
            chatsCollection.document(chatId).update("lastMessage", lastMessage).await()
        }
    }

    /**
     * Helper function để lấy ID của người dùng còn lại trong cuộc trò chuyện.
     */
    private suspend fun getTargetUserId(chatId: String, currentUserId: String): String {
        val chatDoc = chatsCollection.document(chatId).get().await()
        val members = chatDoc.get("members") as? List<String> ?: emptyList()
        return members.firstOrNull { it != currentUserId } ?: ""
    }
    fun getFileType(fileUri: Uri): String? {
        return context.contentResolver.getType(fileUri)
    }
}
