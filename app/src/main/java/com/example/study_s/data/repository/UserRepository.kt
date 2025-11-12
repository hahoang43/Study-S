// file: data/repository/UserRepository.kt
package com.example.study_s.data.repository

import android.net.Uri
import com.example.study_s.data.model.FollowUserData
import com.example.study_s.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.Date

class UserRepository {

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val usersCollection = db.collection("users")
    private val followsCollection = db.collection("follows")

    private fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    suspend fun upsertUserProfile(firebaseUser: FirebaseUser): Result<Unit> {
        return try {
            val userRef = usersCollection.document(firebaseUser.uid)
            val snapshot = userRef.get().await()

            if (snapshot.exists()) {
                val existingUser = snapshot.toObject(User::class.java)
                val updates = mutableMapOf<String, Any>()

                if ((existingUser?.name.isNullOrEmpty() || existingUser?.name == "New User") && !firebaseUser.displayName.isNullOrEmpty()) {
                    updates["name"] = firebaseUser.displayName!!
                }

                if (existingUser?.avatarUrl.isNullOrEmpty() && firebaseUser.photoUrl != null) {
                    updates["avatarUrl"] = firebaseUser.photoUrl.toString()
                }

                if (existingUser?.email != firebaseUser.email && !firebaseUser.email.isNullOrEmpty()) {
                    updates["email"] = firebaseUser.email!!
                }

                if (updates.isNotEmpty()) {
                    userRef.update(updates).await()
                }
            } else {
                val newUser = User(
                    userId = firebaseUser.uid,
                    name = firebaseUser.displayName ?: "New User",
                    email = firebaseUser.email ?: "",
                    avatarUrl = firebaseUser.photoUrl?.toString(),
                    createdAt = Date(),
                    bio = "Chào mừng đến với StudyS!"
                )
                userRef.set(newUser).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createUserProfile(userId: String, name: String, email: String): Result<Unit> {
        return try {
            val newUser = User(
                userId = userId,
                name = name,
                email = email,
                createdAt = Date(),
                bio = "Chào mừng đến với StudyS!",
                avatarUrl = null
            )
            usersCollection.document(userId).set(newUser).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserProfile(userId: String): Result<User?> {
        return try {
            val document = usersCollection.document(userId).get().await()
            val user = document.toObject(User::class.java)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun uploadAvatar(userId: String, imageUri: Uri): Result<String> {
        return try {
            val storageRef = storage.reference.child("avatars/$userId/profile.jpg")
            storageRef.putFile(imageUri).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserProfile(name: String, bio: String, newAvatarUri: Uri?): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(Exception("Người dùng chưa đăng nhập"))
        val userId = user.uid

        return try {
            val updateData = mutableMapOf<String, Any>()
            updateData["name"] = name
            updateData["bio"] = bio

            var newAvatarUrl: String? = null

            if (newAvatarUri != null) {
                uploadAvatar(userId, newAvatarUri).onSuccess { downloadUrl ->
                    updateData["avatarUrl"] = downloadUrl
                    newAvatarUrl = downloadUrl
                }.onFailure {
                    return Result.failure(it)
                }
            }

            usersCollection.document(userId).update(updateData).await()

            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .apply {
                    newAvatarUrl?.let { setPhotoUri(Uri.parse(it)) }
                }
                .build()

            user.updateProfile(profileUpdates).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun followUser(targetUserId: String): Result<Unit> {
        val currentUserId = getCurrentUserId() ?: return Result.failure(Exception("User not logged in"))
        if (currentUserId == targetUserId) {
            return Result.failure(Exception("Cannot follow yourself"))
        }

        return try {
            val currentUserDoc = usersCollection.document(currentUserId).get().await()
            val targetUserDoc = usersCollection.document(targetUserId).get().await()

            val currentUser = currentUserDoc.toObject(User::class.java) ?: return Result.failure(Exception("Current user not found"))
            val targetUser = targetUserDoc.toObject(User::class.java) ?: return Result.failure(Exception("Target user not found"))

            val batch = db.batch()

            val followingRef = followsCollection.document(currentUserId).collection("following").document(targetUserId)
            val followingData = mapOf(
                "timestamp" to FieldValue.serverTimestamp(),
                "username" to targetUser.name,
                "avatarUrl" to (targetUser.avatarUrl ?: "")
            )
            batch.set(followingRef, followingData)

            val followerRef = followsCollection.document(targetUserId).collection("followers").document(currentUserId)
            val followerData = mapOf(
                "timestamp" to FieldValue.serverTimestamp(),
                "username" to currentUser.name,
                "avatarUrl" to (currentUser.avatarUrl ?: "")
            )
            batch.set(followerRef, followerData)

            val currentUserRef = usersCollection.document(currentUserId)
            batch.update(currentUserRef, "followingCount", FieldValue.increment(1))

            val targetUserRef = usersCollection.document(targetUserId)
            batch.update(targetUserRef, "followerCount", FieldValue.increment(1))

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unfollowUser(targetUserId: String): Result<Unit> {
        val currentUserId = getCurrentUserId() ?: return Result.failure(Exception("User not logged in"))

        return try {
            val batch = db.batch()

            val followingRef = followsCollection.document(currentUserId).collection("following").document(targetUserId)
            batch.delete(followingRef)

            val followerRef = followsCollection.document(targetUserId).collection("followers").document(currentUserId)
            batch.delete(followerRef)

            val currentUserRef = usersCollection.document(currentUserId)
            batch.update(currentUserRef, "followingCount", FieldValue.increment(-1))

            val targetUserRef = usersCollection.document(targetUserId)
            batch.update(targetUserRef, "followerCount", FieldValue.increment(-1))

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isFollowing(targetUserId: String): Result<Boolean> {
        val currentUserId = getCurrentUserId() ?: return Result.success(false)
        return try {
            val doc = followsCollection.document(currentUserId).collection("following").document(targetUserId).get().await()
            Result.success(doc.exists())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun getFollowers(userId: String): Result<List<FollowUserData>> {
        return try {
            val snapshot = followsCollection.document(userId).collection("followers").get().await()
            val users = snapshot.documents.mapNotNull { doc ->
                val username = doc.getString("username") ?: ""
                val avatarUrl = doc.getString("avatarUrl")
                FollowUserData(userId = doc.id, username = username, avatarUrl = avatarUrl)
            }
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFollowing(userId: String): Result<List<FollowUserData>> {
        return try {
            val snapshot = followsCollection.document(userId).collection("following").get().await()
            val users = snapshot.documents.mapNotNull { doc ->
                val username = doc.getString("username") ?: ""
                val avatarUrl = doc.getString("avatarUrl")
                FollowUserData(userId = doc.id, username = username, avatarUrl = avatarUrl)
            }
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
