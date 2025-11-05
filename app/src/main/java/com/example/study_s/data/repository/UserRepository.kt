// file: data/repository/UserRepository.kt
package com.example.study_s.data.repository

import android.net.Uri
import com.example.study_s.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.Date // SỬA: Thêm import cho Date

class UserRepository {

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()
    // SỬA: Thêm khai báo cho usersCollection
    private val usersCollection = db.collection("users")

    // Lấy ID của người dùng hiện tại
    private fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    /**
     * Tạo một document hồ sơ mới trên Firestore cho người dùng vừa đăng ký.
     * @param userId ID của người dùng mới.
     * @param name Tên người dùng.
     * @param email Email người dùng.
     * @return Result<Unit> cho biết thành công hay thất bại.
     */
    suspend fun createUserProfile(userId: String, name: String, email: String): Result<Unit> {
        return try {
            // Tạo một đối tượng User mới với các thông tin cơ bản
            val newUser = User(
                userId = userId,
                name = name,
                email = email,
                createdAt = Date(), // Ngày giờ hiện tại
                bio = "Chào mừng đến với StudyS!", // Tiểu sử mặc định
                avatarUrl = null // Chưa có ảnh đại diện
            )
            // Dùng userId làm ID cho document và lưu đối tượng newUser vào Firestore
            usersCollection.document(userId).set(newUser).await()
            Result.success(Unit) // Trả về thành công
        } catch (e: Exception) {
            Result.failure(e) // Trả về lỗi nếu có
        }
    }

    // SỬA: Di chuyển các hàm vào bên trong class
    // 1. Lấy thông tin người dùng từ Firestore
    suspend fun getUserProfile(userId: String): Result<User?> {
        return try {
            val document = usersCollection.document(userId).get().await()
            val user = document.toObject(User::class.java)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 2. Tải ảnh đại diện lên Firebase Storage
    private suspend fun uploadAvatar(userId: String, imageUri: Uri): Result<String> {
        return try {
            val storageRef = storage.reference.child("avatars/$userId/profile.jpg")
            // Tải file lên
            storageRef.putFile(imageUri).await()
            // Lấy URL của ảnh đã tải lên
            val downloadUrl = storageRef.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 3. Cập nhật thông tin cá nhân
    suspend fun updateUserProfile(name: String, bio: String, newAvatarUri: Uri?): Result<Unit> {
        val userId = getCurrentUserId() ?: return Result.failure(Exception("Người dùng chưa đăng nhập"))

        return try {
            val updateData = mutableMapOf<String, Any>()
            updateData["name"] = name
            updateData["bio"] = bio

            // Nếu có ảnh mới, tải lên và lấy URL
            if (newAvatarUri != null) {
                uploadAvatar(userId, newAvatarUri).onSuccess { downloadUrl ->
                    updateData["avatarUrl"] = downloadUrl
                }.onFailure {
                    // Nếu tải ảnh thất bại, trả về lỗi ngay lập tức
                    return Result.failure(it)
                }
            }

            // Cập nhật dữ liệu trên Firestore
            usersCollection.document(userId).update(updateData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} // <-- SỬA: Đây là dấu ngoặc nhọn kết thúc class, tất cả các hàm phải nằm trên dòng này
