// BẮT ĐẦU FILE: data/repository/AuthRepository.kt
package com.example.study_s.data.repository

import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    // Biến này đã có, giữ nguyên
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    // ======================================================================
    // ✅ THÊM HÀM CÒN THIẾU VÀO ĐÂY
    // ======================================================================
    /**
     * Tải lại dữ liệu người dùng hiện tại từ máy chủ Firebase.
     * Rất quan trọng để đảm bảo các màn hình luôn có thông tin mới nhất sau khi cập nhật.
     * Trả về một Result chứa FirebaseUser đã được làm mới.
     */
    suspend fun reloadCurrentUser(): Result<FirebaseUser?> {
        return try {
            // Yêu cầu Firebase tải lại dữ liệu của người dùng hiện tại
            auth.currentUser?.reload()?.await()
            // Sau khi tải lại, auth.currentUser sẽ chứa thông tin mới nhất
            Result.success(auth.currentUser)
        } catch (e: Exception) {
            // Nếu có lỗi (ví dụ: mất mạng), trả về failure
            Result.failure(e)
        }
    }
    // ======================================================================
    // CÁC HÀM CŨ CỦA BẠN ĐƯỢC GIỮ NGUYÊN HOÀN TOÀN
    // ======================================================================

    /**
     * Đăng nhập bằng email và mật khẩu.
     * Trả về `Result<FirebaseUser>` để ViewModel có thể lấy thông tin người dùng.
     */
    suspend fun signIn(email: String, pass: String): Result<FirebaseUser> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, pass).await()
            val firebaseUser = authResult.user ?: throw IllegalStateException("Firebase user is null after sign in")
            Result.success(firebaseUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Đăng nhập Firebase bằng Google ID Token */
    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user ?: throw IllegalStateException("Firebase user is null after Google sign in")
            Result.success(firebaseUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Đăng ký người dùng mới bằng email và mật khẩu.
     * Trả về một Result chứa FirebaseUser nếu thành công để ViewModel có thể lấy uid.
     */
    suspend fun signUp(email: String, pass: String): Result<FirebaseUser> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, pass).await()
            val firebaseUser = authResult.user ?: throw IllegalStateException("Firebase user is null after sign up")
            Result.success(firebaseUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun linkPassword(password: String): Result<FirebaseUser> {
        return try {
            val user = auth.currentUser
                ?: return Result.failure(Exception("Không có người dùng nào đang đăng nhập."))
            val credential = EmailAuthProvider.getCredential(user.email!!, password)
            auth.currentUser!!.linkWithCredential(credential).await()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Đăng xuất Firebase */
    fun signOut() = auth.signOut()
}
// KẾT THÚC FILE
