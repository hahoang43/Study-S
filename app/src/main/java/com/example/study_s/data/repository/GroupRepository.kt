package com.example.study_s.data.repository

import android.util.Log
import com.example.study_s.data.model.Group
import com.example.study_s.data.model.User
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class GroupRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {
    private val groupsRef = db.collection("groups")
    private val usersRef = db.collection("users")

    // ... các hàm createGroup, getGroupById, joinGroup, leaveGroup... đã đúng, giữ nguyên ...
    suspend fun createGroup(group: Group) {
        val newGroupRef = groupsRef.document()
        val finalGroup = group.copy(groupId = newGroupRef.id)
        newGroupRef.set(finalGroup).await()
    }

    suspend fun getGroupById(groupId: String): Group? {
        if (groupId.isBlank()) {
            Log.e("GroupRepository", "getGroupById được gọi với groupId rỗng!")
            return null
        }
        val snapshot = groupsRef.document(groupId).get().await()
        return snapshot.toObject(Group::class.java)
    }

    suspend fun joinGroup(groupId: String, userId: String) {
        if (groupId.isEmpty() || userId.isEmpty()) {
            Log.e("GroupRepository", "joinGroup bị gọi với groupId hoặc userId rỗng.")
            throw IllegalArgumentException("Group ID và User ID không được rỗng.")
        }
        val group = getGroupById(groupId)
        if (group?.bannedUsers?.contains(userId) == true) {
            throw Exception("Bạn đã bị chặn khỏi nhóm này và không thể tham gia")
        }
        groupsRef.document(groupId).update("members", FieldValue.arrayUnion(userId)).await()
    }

    // ... các hàm khác ...
    suspend fun leaveGroup(groupId: String, userId: String) {
        groupsRef.document(groupId).update("members", FieldValue.arrayRemove(userId)).await()
    }

    suspend fun deleteGroup(groupId: String) {
        groupsRef.document(groupId).delete().await()
    }

    suspend fun getMemberDetails(memberIds: List<String>): List<User> {
        if (memberIds.isEmpty()) return emptyList()
        val snapshot = usersRef.whereIn("userId", memberIds).get().await()
        return snapshot.toObjects(User::class.java)
    }

    suspend fun removeMemberFromGroup(groupId: String, userId: String) {
        groupsRef.document(groupId).update("members", FieldValue.arrayRemove(userId)).await()
        usersRef.document(userId).update("joinedGroups", FieldValue.arrayRemove(groupId)).await()
    }

    suspend fun banUser(groupId: String, userId: String) {
        groupsRef.document(groupId).update("members", FieldValue.arrayRemove(userId)).await()
        groupsRef.document(groupId).update("bannedUsers", FieldValue.arrayUnion(userId)).await()
    }

    suspend fun unbanUser(groupId: String, userId: String) {
        groupsRef.document(groupId).update("bannedUsers", FieldValue.arrayRemove(userId)).await()
    }

    // =========================================================================
    // ✅ PHẦN SỬA LỖI QUAN TRỌNG
    // =========================================================================

    /**
     * Hàm này lấy TẤT CẢ các nhóm.
     * Đã được sửa để luôn trả về groupId chính xác.
     */
    suspend fun getAllGroups(): List<Group> {
        return try {
            val snapshot = groupsRef.get().await()
            // Áp dụng logic map để đảm bảo có groupId
            snapshot.documents.mapNotNull { document ->
                document.toObject(Group::class.java)?.copy(groupId = document.id)
            }
        } catch (e: Exception) {
            Log.e("GroupRepository", "Error getting all groups", e)
            emptyList()
        }
    }

    /**
     * Hàm này lấy các nhóm mà một người dùng đã tham gia.
     * Đã được sửa để luôn trả về groupId chính xác.
     */
    suspend fun getUserGroups(userId: String): List<Group> {
        return try {
            val snapshot = groupsRef
                .whereArrayContains("members", userId)
                .get()
                .await()
            // Áp dụng logic map để đảm bảo có groupId
            snapshot.documents.mapNotNull { document ->
                document.toObject(Group::class.java)?.copy(groupId = document.id)
            }
        } catch (e: Exception) {
            Log.e("GroupRepository", "Error getting user groups", e)
            emptyList()
        }
    }


    /**
     * Hàm tìm kiếm nhóm theo query.
     * Logic này đã đúng và được giữ nguyên.
     */
    suspend fun searchGroups(query: String): List<Group> {
        // 1. Chuẩn hóa và lấy từ khóa tìm kiếm từ người dùng
        val searchQuery = query.lowercase().trim()
        if (searchQuery.isBlank()) {
            return emptyList()
        }

        return try {
            // =========================================================================
            // ✅ THAY ĐỔI LOGIC TRUY VẤN TÌM KIẾM
            // =========================================================================
            // 2. Sử dụng `whereArrayContains` để tìm kiếm bất kỳ tài liệu nào
            //    có mảng `searchKeywords` chứa từ khóa người dùng nhập vào.
            val snapshot = groupsRef
                .whereArrayContains("searchKeywords", searchQuery)
                .limit(20) // Giới hạn số lượng kết quả
                .get()
                .await()
            // =========================================================================

            // 3. Logic xử lý kết quả để đảm bảo có `groupId` vẫn giữ nguyên
            snapshot.documents.mapNotNull { document ->
                document.toObject(Group::class.java)?.copy(groupId = document.id)
            }

        } catch (e: Exception) {
            Log.e("GroupRepository", "Error searching groups with keywords", e)
            emptyList()
        }
    }

    // =========================================================================
}
