package com.example.study_s.data.repository

import com.example.study_s.data.model.Group
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

    suspend fun joinGroup(groupId: String, userId: String) {
        val groupDoc = groupsRef.document(groupId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(groupDoc)
            val members = snapshot.get("members") as? List<String> ?: emptyList()
            if (!members.contains(userId)) {
                transaction.update(groupDoc, "members", members + userId)
            }
        }.await()
    }

    suspend fun getUserGroups(userId: String): List<Group> {
        val snapshot = groupsRef
            .whereArrayContains("members", userId)
            .get()
            .await()
        return snapshot.toObjects(Group::class.java)
    }
}
