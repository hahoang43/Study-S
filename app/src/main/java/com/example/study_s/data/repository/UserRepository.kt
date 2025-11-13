package com.example.study_s.data.repository

import android.net.Uri
import android.util.Log
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

    // =======================================================================================
    // 1. CẬP NHẬT HÀM UPSERT USER PROFILE
    // =======================================================================================
    suspend fun upsertUserProfile(firebaseUser: FirebaseUser): Result<Unit> {
        return try {
            val userRef = usersCollection.document(firebaseUser.uid)
            val snapshot = userRef.get().await()

            if (snapshot.exists()) {
                val existingUser = snapshot.toObject(User::class.java)
                val updates = mutableMapOf<String, Any>()

                if ((existingUser?.name.isNullOrEmpty() || existingUser?.name == "New User") && !firebaseUser.displayName.isNullOrEmpty()) {
                    val newName = firebaseUser.displayName!!
                    updates["name"] = newName
                    updates["nameLowercase"] = newName.lowercase()
                    // ✅ Tạo và cập nhật searchKeywords khi có tên mới từ Google/Facebook
                    val keywords = newName.lowercase().split(" ").filter { it.isNotBlank() }.distinct()
                    updates["searchKeywords"] = keywords
                }
                // ... (các phần cập nhật avatar, email giữ nguyên)
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
                val newName = firebaseUser.displayName ?: "New User"
                val nameLowercase = newName.lowercase()
                // ✅ Tạo searchKeywords khi tạo user mới
                val keywords = nameLowercase.split(" ").filter { it.isNotBlank() }.distinct()

                val newUser = User(
                    userId = firebaseUser.uid,
                    name = newName,
                    email = firebaseUser.email ?: "",
                    avatarUrl = firebaseUser.photoUrl?.toString(),
                    createdAt = Date(),
                    bio = "Chào mừng đến với StudyS!",
                    nameLowercase = nameLowercase,
                    searchKeywords = keywords // <-- Gán từ khóa
                )
                userRef.set(newUser).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =======================================================================================
    // 2. CẬP NHẬT HÀM CREATE USER PROFILE
    // =======================================================================================
    suspend fun createUserProfile(userId: String, name: String, email: String): Result<Unit> {
        return try {
            val nameLowercase = name.lowercase()
            // ✅ Tạo searchKeywords khi tạo profile mới
            val keywords = nameLowercase.split(" ").filter { it.isNotBlank() }.distinct()

            val newUser = User(
                userId = userId,
                name = name,
                email = email,
                createdAt = Date(),
                bio = "Chào mừng đến với StudyS!",
                avatarUrl = null,
                nameLowercase = nameLowercase,
                searchKeywords = keywords // <-- Gán từ khóa
            )
            usersCollection.document(userId).set(newUser).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =======================================================================================
    // 3. CẬP NHẬT HÀM TÌM KIẾM
    // =======================================================================================
    suspend fun searchUsers(query: String): List<User> {
        val searchQuery = query.lowercase().trim()
        if (searchQuery.isBlank()) {
            return emptyList()
        }
        return try {
            // ✅ Thay đổi logic tìm kiếm để sử dụng 'whereArrayContains'
            val querySnapshot = usersCollection
                .whereArrayContains("searchKeywords", searchQuery)
                .limit(15)
                .get()
                .await()

            querySnapshot.toObjects(User::class.java)
        } catch (e: Exception) {
            Log.e("UserRepository", "Error searching users", e)
            emptyList()
        }
    }

    // =======================================================================================
    // 4. HÀM GET VÀ UPLOAD (GIỮ NGUYÊN)
    // =======================================================================================
    suspend fun getUserProfile(userId: String): Result<User?> {
        // ... (Giữ nguyên không đổi)
        return try {
            val document = usersCollection.document(userId).get().await()
            val user = document.toObject(User::class.java)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun uploadAvatar(userId: String, imageUri: Uri): Result<String> {
        // ... (Giữ nguyên không đổi)
        return try {
            val storageRef = storage.reference.child("avatars/$userId/profile.jpg")
            storageRef.putFile(imageUri).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =======================================================================================
    // 5. CẬP NHẬT HÀM UPDATE USER PROFILE
    // =======================================================================================
    suspend fun updateUserProfile(name: String, bio: String, newAvatarUri: Uri?): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(Exception("Người dùng chưa đăng nhập"))
        val userId = user.uid

        return try {
            val updateData = mutableMapOf<String, Any>()
            updateData["name"] = name
            updateData["bio"] = bio
            val nameLowercase = name.lowercase()
            updateData["nameLowercase"] = nameLowercase

            // ✅ Tạo và cập nhật lại searchKeywords khi người dùng đổi tên
            val keywords = nameLowercase.split(" ").filter { it.isNotBlank() }.distinct()
            updateData["searchKeywords"] = keywords

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

            // ... (Phần cập nhật auth profile giữ nguyên)
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

    // =======================================================================================
    // 6. CÁC HÀM FOLLOW/UNFOLLOW/GETTERS (GIỮ NGUYÊN)
    // =======================================================================================
    // ... (Toàn bộ code cho follow, unfollow, isFollowing, getFollowers, getFollowing giữ nguyên y như cũ)
    // ✅ SỬA LẠI HÀM FOLLOW
// =======================================================================================
    suspend fun followUser(targetUserId: String): Result<Unit> {
        val currentUserId = getCurrentUserId() ?: return Result.failure(Exception("User not logged in"))
        if (currentUserId == targetUserId) {
            return Result.failure(Exception("Cannot follow yourself"))
        }

        return try {
            // Lấy thông tin user trước khi thực hiện batch
            val currentUserDoc = usersCollection.document(currentUserId).get().await()
            val targetUserDoc = usersCollection.document(targetUserId).get().await()

            val currentUser = currentUserDoc.toObject(User::class.java) ?: return Result.failure(Exception("Current user not found"))
            val targetUser = targetUserDoc.toObject(User::class.java) ?: return Result.failure(Exception("Target user not found"))

            val batch = db.batch()

            // 1. Cập nhật sub-collection "following" của currentUser
            val followingRef = followsCollection.document(currentUserId).collection("following").document(targetUserId)
            val followingData = mapOf(
                "timestamp" to FieldValue.serverTimestamp(),
                "username" to targetUser.name,
                "avatarUrl" to (targetUser.avatarUrl ?: "")
            )
            batch.set(followingRef, followingData)

            // 2. Cập nhật sub-collection "followers" của targetUser
            val followerRef = followsCollection.document(targetUserId).collection("followers").document(currentUserId)
            val followerData = mapOf(
                "timestamp" to FieldValue.serverTimestamp(),
                "username" to currentUser.name,
                "avatarUrl" to (currentUser.avatarUrl ?: "")
            )
            batch.set(followerRef, followerData)

            // 3. Tăng số followingCount của currentUser
            val currentUserRef = usersCollection.document(currentUserId)
            batch.update(currentUserRef, "followingCount", FieldValue.increment(1))

            // 4. Tăng số followerCount của targetUser
            val targetUserRef = usersCollection.document(targetUserId)
            batch.update(targetUserRef, "followerCount", FieldValue.increment(1))

            // Thực hiện tất cả các thao tác
            batch.commit().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =======================================================================================
// ✅ SỬA LẠI HÀM UNFOLLOW
// =======================================================================================
    suspend fun unfollowUser(targetUserId: String): Result<Unit> {
        val currentUserId = getCurrentUserId() ?: return Result.failure(Exception("User not logged in"))

        return try {
            val batch = db.batch()

            // 1. Xóa khỏi sub-collection "following" của currentUser
            val followingRef = followsCollection.document(currentUserId).collection("following").document(targetUserId)
            batch.delete(followingRef)

            // 2. Xóa khỏi sub-collection "followers" của targetUser
            val followerRef = followsCollection.document(targetUserId).collection("followers").document(currentUserId)
            batch.delete(followerRef)

            // 3. Giảm số followingCount của currentUser
            val currentUserRef = usersCollection.document(currentUserId)
            batch.update(currentUserRef, "followingCount", FieldValue.increment(-1))

            // 4. Giảm số followerCount của targetUser
            val targetUserRef = usersCollection.document(targetUserId)
            batch.update(targetUserRef, "followerCount", FieldValue.increment(-1))

            // Thực hiện tất cả các thao tác
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
