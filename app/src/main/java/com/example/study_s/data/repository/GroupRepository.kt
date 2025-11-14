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

    suspend fun createGroup(group: Group): String {
        val newGroupRef = groupsRef.document()
        newGroupRef.set(group).await()
        return newGroupRef.id
    }

    suspend fun getGroupById(groupId: String): Group? {
        if (groupId.isBlank()) {
            Log.e("GroupRepository", "getGroupById called with blank groupId!")
            return null
        }
        val snapshot = groupsRef.document(groupId).get().await()
        return snapshot.toObject(Group::class.java)
    }

    suspend fun joinGroup(groupId: String, userId: String) {
        if (groupId.isEmpty() || userId.isEmpty()) {
            Log.e("GroupRepository", "joinGroup called with empty groupId or userId.")
            throw IllegalArgumentException("Group ID and User ID cannot be empty.")
        }
        val group = getGroupById(groupId)
        if (group?.bannedUsers?.contains(userId) == true) {
            throw Exception("You have been banned from this group and cannot join.")
        }
        groupsRef.document(groupId).update("members", FieldValue.arrayUnion(userId)).await()
    }

    suspend fun leaveGroup(groupId: String, userId: String) {
        groupsRef.document(groupId).update("members", FieldValue.arrayRemove(userId)).await()
    }

    suspend fun deleteGroup(groupId: String) {
        if (groupId.isBlank()) {
            Log.e("GroupRepository", "deleteGroup called with blank groupId!")
            return
        }
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

    suspend fun getAllGroups(): List<Group> {
        return try {
            val snapshot = groupsRef.get().await()
            snapshot.toObjects(Group::class.java)
        } catch (e: Exception) {
            Log.e("GroupRepository", "Error getting all groups", e)
            emptyList()
        }
    }

    suspend fun getUserGroups(userId: String): List<Group> {
        return try {
            val snapshot = groupsRef
                .whereArrayContains("members", userId)
                .get()
                .await()
            snapshot.toObjects(Group::class.java)
        } catch (e: Exception) {
            Log.e("GroupRepository", "Error getting user groups", e)
            emptyList()
        }
    }

    suspend fun searchGroups(query: String): List<Group> {
        val searchQuery = query.lowercase().trim()
        if (searchQuery.isBlank()) {
            return emptyList()
        }

        return try {
            val snapshot = groupsRef
                .whereArrayContains("searchKeywords", searchQuery)
                .limit(20)
                .get()
                .await()
            snapshot.toObjects(Group::class.java)
        } catch (e: Exception) {
            Log.e("GroupRepository", "Error searching groups with keywords", e)
            emptyList()
        }
    }
}
