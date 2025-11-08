package com.example.study_s.data.repository

import android.util.Log
import com.example.study_s.data.model.Group
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class GroupRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val groupsRef = db.collection("groups")

    suspend fun createGroup(group: Group) {
        groupsRef.document(group.groupId).set(group).await()
    }

    suspend fun getAllGroups(): List<Group> {
        val snapshot = groupsRef.get().await()
        return snapshot.toObjects(Group::class.java)
    }

    suspend fun getGroupById(groupId: String): Group? {
        val snapshot = groupsRef.document(groupId).get().await()
        return snapshot.toObject(Group::class.java)
    }

    suspend fun joinGroup(groupId: String, userId: String) {
        // --- BẮT ĐẦU SỬA LỖI ---
        // Thêm kiểm tra để đảm bảo groupId không rỗng    if (groupId.isEmpty()) {
        // Ghi lại lỗi hoặc trả về một trạng thái lỗi để ViewModel xử lý
        Log.e("GroupRepository", "Attempted to join group with an empty groupId.")
        // Có thể throw một Exception tường minh hơn hoặc trả về một Result.Failure
        throw IllegalArgumentException("Group ID cannot be empty.")
    }

    suspend fun leaveGroup(groupId: String, userId: String) {
        groupsRef.document(groupId).update("members", FieldValue.arrayRemove(userId)).await()
    }

    suspend fun deleteGroup(groupId: String) {
        groupsRef.document(groupId).delete().await()
    }

    suspend fun getUserGroups(userId: String): List<Group> {
        val snapshot = groupsRef
            .whereArrayContains("members", userId)
            .get()
            .await()
        return snapshot.toObjects(Group::class.java)
    }
}
