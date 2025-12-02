// BẮT ĐẦU FILE: data/repository/UserRepository.kt
package com.example.study_s.data.repository // Đảm bảo đây là package chính xác của bạn

import android.net.Uri
import android.system.Os
import android.util.Log
import com.example.study_s.data.model.FollowUserDataModel
import com.example.study_s.data.model.UserModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.flow
// import com.google.firebase.storage.FirebaseStorage // <<-- ❌ KHÔNG DÙNG NỮA
import kotlinx.coroutines.tasks.await
import java.util.Date
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose

class UserRepository {

    private val db = FirebaseFirestore.getInstance()

    // private val storage = FirebaseStorage.getInstance() // <<-- ❌ KHÔNG DÙNG NỮA
    private val auth = FirebaseAuth.getInstance()
    private val usersCollection = db.collection("users")
    private val followsCollection = db.collection("follows")

    // ✅ KHỞI TẠO IMAGE REPOSITORY ĐỂ SỬ DỤNG
    private val imageRepository = ImageRepository()

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    // =======================================================================================
    // CÁC HÀM KHÁC GIỮ NGUYÊN (upsert, createUserProfile, searchUsers, getProfile...)
    // =======================================================================================

    suspend fun upsertUserProfile(firebaseUser: FirebaseUser): Result<Unit> {
        return try {
            val userRef = usersCollection.document(firebaseUser.uid)
            val snapshot = userRef.get().await()

            if (snapshot.exists()) {
                val existingUserModel = snapshot.toObject(UserModel::class.java)
                val updates = mutableMapOf<String, Any>()

                if ((existingUserModel?.name.isNullOrEmpty() || existingUserModel?.name == "New User") && !firebaseUser.displayName.isNullOrEmpty()) {
                    val newName = firebaseUser.displayName!!
                    updates["name"] = newName
                    updates["nameLowercase"] = newName.lowercase()
                    val keywords = newName.lowercase().split(" ").filter { it.isNotBlank() }.distinct()
                    updates["searchKeywords"] = keywords
                }
                if (existingUserModel?.avatarUrl.isNullOrEmpty() && firebaseUser.photoUrl != null) {
                    updates["avatarUrl"] = firebaseUser.photoUrl.toString()
                }

                if (existingUserModel?.email != firebaseUser.email && !firebaseUser.email.isNullOrEmpty()) {
                    updates["email"] = firebaseUser.email!!
                }

                if (updates.isNotEmpty()) {
                    userRef.update(updates).await()
                }
            } else {
                val newName = firebaseUser.displayName ?: "New User"
                val nameLowercase = newName.lowercase()
                val keywords = nameLowercase.split(" ").filter { it.isNotBlank() }.distinct()

                val newUserModel = UserModel(
                    userId = firebaseUser.uid,
                    name = newName,
                    email = firebaseUser.email ?: "",
                    avatarUrl = firebaseUser.photoUrl?.toString(),
                    createdAt = Date(),
                    bio = "Chào mừng đến với StudyS!",
                    nameLowercase = nameLowercase,
                    searchKeywords = keywords
                )
                userRef.set(newUserModel).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createUserProfile(userId: String, name: String, email: String): Result<Unit> {
        return try {
            val nameLowercase = name.lowercase()
            val keywords = nameLowercase.split(" ").filter { it.isNotBlank() }.distinct()

            val newUserModel = UserModel(
                userId = userId,
                name = name,
                email = email,
                createdAt = Date(),
                bio = "Chào mừng đến với StudyS!",
                avatarUrl = null,
                nameLowercase = nameLowercase,
                searchKeywords = keywords
            )
            usersCollection.document(userId).set(newUserModel).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchUsers(query: String): List<UserModel> {
        val searchQuery = query.lowercase().trim()
        if (searchQuery.isBlank()) {
            return emptyList()
        }
        return try {
            val querySnapshot = usersCollection
                .whereArrayContains("searchKeywords", searchQuery)
                .limit(15)
                .get()
                .await()
            querySnapshot.toObjects(UserModel::class.java)
        } catch (e: Exception) {
            Log.e("UserRepository", "Error searching users", e)
            emptyList()
        }
    }

    suspend fun getUserProfile(userId: String): Result<UserModel?> {
        return try {
            val document = usersCollection.document(userId).get().await()
            val userModel = document.toObject(UserModel::class.java)
            Result.success(userModel)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // private suspend fun uploadAvatar(userId: String, imageUri: Uri): Result<String> { ... }
    // <<-- ❌ KHÔNG DÙNG HÀM NÀY NỮA, CÓ THỂ XÓA BỎ

    // =======================================================================================
    // 5. ✅ HÀM UPDATEUSERPROFILE ĐÃ ĐƯỢC "CẤY GHÉP" LOGIC CLOUDINARY
    // =======================================================================================
    suspend fun updateUserProfile(name: String, bio: String, newAvatarUri: Uri?): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(Exception("Người dùng chưa đăng nhập"))
        val userId = user.uid

        return try {
            // BƯỚC 1: XỬ LÝ ẢNH MỚI BẰNG CLOUDINARY
            // Nếu có ảnh mới (newAvatarUri khác null), tải nó lên Cloudinary và lấy URL.
            // Nếu không, newCloudinaryUrl sẽ là null.
            val newCloudinaryUrl = if (newAvatarUri != null) {
                imageRepository.uploadImage(newAvatarUri).getOrThrow()
            } else {
                null
            }

            // BƯỚC 2: CHUẨN BỊ DỮ LIỆU CẬP NHẬT
            val updateData = mutableMapOf<String, Any>()
            updateData["name"] = name
            updateData["bio"] = bio
            val nameLowercase = name.lowercase()
            updateData["nameLowercase"] = nameLowercase
            val keywords = nameLowercase.split(" ").filter { it.isNotBlank() }.distinct()
            updateData["searchKeywords"] = keywords

            // Chỉ thêm "avatarUrl" vào map nếu có URL mới từ Cloudinary
            newCloudinaryUrl?.let { url ->
                updateData["avatarUrl"] = url
            }

            // BƯỚC 3: CẬP NHẬT DỮ LIỆU LÊN FIRESTORE
            usersCollection.document(userId).update(updateData).await()

            // BƯỚC 4: CẬP NHẬT THÔNG TIN TRÊN FIREBASE AUTH (Tùy chọn nhưng nên có)
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .apply {
                    // Cập nhật cả PhotoUri trên Auth nếu có
                    newCloudinaryUrl?.let { setPhotoUri(Uri.parse(it)) }
                }
                .build()
            user.updateProfile(profileUpdates).await()

            Result.success(Unit)
        } catch (e: Exception) {
            // Bắt lỗi từ Cloudinary hoặc Firestore
            Result.failure(e)
        }
    }


    // =======================================================================================
    // 6. CÁC HÀM FOLLOW/UNFOLLOW/GETTERS (GIỮ NGUYÊN)
    // =======================================================================================
    suspend fun updateFcmToken(userId: String, token: String) {
        try {
            usersCollection.document(userId).update("fcmToken", token).await()
            Log.d("UserRepository", "FCM token updated successfully for user $userId")
        } catch (e: Exception) {
            Log.e("UserRepository", "Error updating FCM token", e)
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

            val currentUserModel = currentUserDoc.toObject(UserModel::class.java) ?: return Result.failure(Exception("Current user not found"))
            val targetUserModel = targetUserDoc.toObject(UserModel::class.java) ?: return Result.failure(Exception("Target user not found"))

            val batch = db.batch()

            val followingRef = followsCollection.document(currentUserId).collection("following").document(targetUserId)
            val followingData = mapOf(
                "timestamp" to FieldValue.serverTimestamp(),
                "username" to targetUserModel.name,
                "avatarUrl" to (targetUserModel.avatarUrl ?: "")
            )
            batch.set(followingRef, followingData)

            val followerRef = followsCollection.document(targetUserId).collection("followers").document(currentUserId)
            val followerData = mapOf(
                "timestamp" to FieldValue.serverTimestamp(),
                "username" to currentUserModel.name,
                "avatarUrl" to (currentUserModel.avatarUrl ?: "")
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

    suspend fun getFollowers(userId: String): Result<List<FollowUserDataModel>> {
        return try {
            val snapshot = followsCollection.document(userId).collection("followers").get().await()
            val users = snapshot.documents.mapNotNull { doc ->
                val username = doc.getString("username") ?: ""
                val avatarUrl = doc.getString("avatarUrl")
                FollowUserDataModel(userId = doc.id, username = username, avatarUrl = avatarUrl)
            }
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFollowing(userId: String): Result<List<FollowUserDataModel>> {
        return try {
            val snapshot = followsCollection.document(userId).collection("following").get().await()
            val users = snapshot.documents.mapNotNull { doc ->
                val username = doc.getString("username") ?: ""
                val avatarUrl = doc.getString("avatarUrl")
                FollowUserDataModel(userId = doc.id, username = username, avatarUrl = avatarUrl)
            }
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Checks if two users mutually follow each other.
     * @param targetUserId The ID of the other user.
     * @return Result<Boolean> containing true if they mutually follow, false otherwise.
     */
    suspend fun checkMutualFollow(targetUserId: String): Result<Boolean> {
        val currentUserId = getCurrentUserId() ?: return Result.failure(Exception("User not logged in"))
        return try {
            val currentUserIsFollowingTarget = isFollowing(targetUserId).getOrDefault(false)
            if (!currentUserIsFollowingTarget) return Result.success(false)

            // Check if target user is following the current user
            val targetIsFollowingCurrentUser = try {
                val doc = followsCollection.document(targetUserId).collection("following").document(currentUserId).get().await()
                doc.exists()
            } catch (e: Exception) {
                false
            }

            Result.success(targetIsFollowingCurrentUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    // ✅✅✅ HÀM MỚI: XÓA TÀI KHOẢN VÀ DỌN DẸP DỮ LIỆU ✅✅✅
    /**
     * Xóa tài khoản người dùng và tất cả dữ liệu liên quan.
     * Yêu cầu người dùng phải được xác thực lại trước khi gọi hàm này.
     */
    suspend fun deleteUserAccount(): Result<Unit> {
        val currentUser = auth.currentUser ?: return Result.failure(Exception("Không tìm thấy người dùng."))
        val userId = currentUser.uid

        return try {
            // =================================================================
            // BƯỚC 1: DỌN DẸP DỮ LIỆU TRÊN FIRESTORE (VÀ STORAGE NẾU CẦN)
            // =================================================================
            // Đây là bước tùy chọn nhưng rất nên làm để database được sạch sẽ.
            // Ví dụ: Xóa document của user trong collection "users"
            usersCollection.document(userId).delete().await()

            // Ví dụ: Xóa collection "following" và "followers" của user
            // (Phần này cần Cloud Function để xóa sub-collection hiệu quả,
            // nhưng tạm thời ta có thể bỏ qua nếu phức tạp)

            // Ví dụ: Xóa các bài viết của user (cần PostRepository)
            // val postRepo = PostRepository()
            // postRepo.deleteAllPostsFromUser(userId)

            // =================================================================
            // BƯỚC 2: XÓA TÀI KHOẢN TRÊN FIREBASE AUTHENTICATION (QUAN TRỌNG NHẤT)
            // =================================================================
            // Hành động này sẽ xóa vĩnh viễn user khỏi hệ thống đăng nhập.
            currentUser.delete().await()

            // Nếu mọi thứ thành công
            Result.success(Unit)
        } catch (e: Exception) {
            // Bắt các lỗi có thể xảy ra (ví dụ: cần xác thực lại, lỗi mạng...)
            Log.e("UserRepository", "Lỗi khi xóa tài khoản: ", e)
            Result.failure(e)
        }
    }
    fun getUserDocumentRef(userId: String): DocumentReference {
        return usersCollection.document(userId)
    }
    fun getWhoBlockedUserFlow(userId: String): Flow<List<String>> {
        // Chúng ta cần truy vấn tất cả người dùng để xem ai đã chặn `userId`.
        // Đây là một truy vấn tốn kém và không được khuyến nghị trên quy mô lớn.
        //
        // THAY VÀO ĐÓ, chúng ta sẽ tối ưu bằng cách tạo một trường mới trong tài liệu người dùng.
        // Ví dụ, khi UserA chặn UserB, chúng ta sẽ cập nhật:
        // 1. users/UserA -> blockedUsers: [..., "UserB"]
        // 2. users/UserB -> blockedBy: [..., "UserA"]
        //
        // Tuy nhiên, để đơn giản và không thay đổi cấu trúc DB quá nhiều,
        // chúng ta sẽ kiểm tra trực tiếp từ `targetUser`.
        // Cách này hiệu quả hơn nhiều.
        return flow {
            // Hàm này sẽ được triển khai trong ViewModel một cách hiệu quả hơn
            // bằng cách chỉ đọc tài liệu của targetUser.
            // Để trống ở đây và xử lý trong ViewModel để tránh phức tạp hóa Repository.
        }
    }
    fun getUserProfileFlow(userId: String): Flow<UserModel?> = callbackFlow {
        // Lấy tham chiếu đến document của người dùng
        val documentRef = usersCollection.document(userId)

        // Đăng ký một listener để lắng nghe sự thay đổi trên document này
        val subscription = documentRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                // Nếu có lỗi, đóng Flow với một exception
                close(error)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                // Nếu document tồn tại, chuyển đổi nó thành đối tượng UserModel
                val user = snapshot.toObject(UserModel::class.java)
                // Phát ra (emit) đối tượng user mới nhất.
                // trySend sẽ không block nếu không có ai lắng nghe.
                trySend(user).isSuccess
            } else {
                // Nếu document không tồn tại hoặc bị xóa, phát ra giá trị null
                trySend(null).isSuccess
            }
        }

        // Khối `awaitClose` sẽ được gọi khi Flow bị hủy (khi ViewModel bị hủy).
        // Chúng ta cần hủy đăng ký listener để tránh rò rỉ bộ nhớ.
        awaitClose {
            subscription.remove()
        }
    }
}

// KẾT THÚC FILE
