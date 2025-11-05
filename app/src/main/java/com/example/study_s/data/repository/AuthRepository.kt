package com.example.study_s.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    val currentUser get() = auth.currentUser

    /** Đăng nhập Firebase bằng Google ID Token */
    suspend fun signInWithGoogle(idToken: String): Result<Unit> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // NOTE: THÊM HÀM CÒN THIẾU NÀY VÀO
    /**
     * Đăng ký người dùng mới bằng email và mật khẩu.
     * Trả về một Result chứa FirebaseUser nếu thành công để ViewModel có thể lấy uid.
     */
    suspend fun signUp(email: String, pass: String): Result<FirebaseUser> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, pass).await()
            // Nếu authResult hoặc user là null, ném ra một ngoại lệ để khối catch bắt được
            val firebaseUser = authResult.user ?: throw IllegalStateException("Firebase user is null after sign up")
            Result.success(firebaseUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Đăng xuất Firebase */
    fun signOut() = auth.signOut()
}
