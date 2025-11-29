package com.example.study_s.data.repository

import com.example.study_s.data.model.ChatModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ChatListRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val chatsCollection = db.collection("chats")

    // ✅ SỬA 1: Hợp nhất cách lấy user ID
    private fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    suspend fun markChatAsRead(chatId: String) {
        val userId = getCurrentUserId() // Sử dụng hàm đã định nghĩa
        if (userId != null) {
            // Đường dẫn "lastMessage.readBy.USER_ID" sẽ cập nhật giá trị của user đó trong map
            chatsCollection.document(chatId)
                .update("lastMessage.readBy.$userId", true)
                .await() // Dùng await() để đảm bảo coroutine chờ thao tác hoàn tất
        }
    }

    suspend fun deleteChats(chatIds: Set<String>) {
        val batch = db.batch()
        chatIds.forEach {
            val docRef = chatsCollection.document(it)
            batch.delete(docRef)
        }
        batch.commit().await()
    }

    fun getChats(): Flow<List<ChatModel>> = callbackFlow {
        val currentUserId = getCurrentUserId()
        if (currentUserId == null) {
            close(Exception("User not logged in")) // Thoát sớm nếu user chưa đăng nhập
            return@callbackFlow
        }

        val listenerRegistration = chatsCollection
            .whereArrayContains("members", currentUserId)
            // ✅ SỬA 2: Đổi tên trường timestamp cho đúng với cấu trúc của bạn (nếu cần)
            .orderBy("lastMessage.timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                // Firestore sẽ báo lỗi ở dòng này nếu data class không khớp
                val chats = snapshot?.toObjects(ChatModel::class.java) ?: emptyList()
                trySend(chats)
            }

        awaitClose { listenerRegistration.remove() }
    }
}
