package com.example.study_s.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

// Sử dụng dependency injection sau này sẽ tốt hơn, tạm thời khởi tạo trực tiếp
class UserRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(email: String, password: String, username: String): Result<Unit> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val userId = authResult.user?.uid
            if (userId != null) {
                // Lưu thông tin user vào Firestore
                val user = mapOf(
                    "uid" to userId,
                    "username" to username,
                    "email" to email,
                    "profileImageUrl" to "" // ảnh đại diện mặc định
                )
                db.collection("users").document(userId).set(user).await()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Could not create user"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        auth.signOut()
    }
}
