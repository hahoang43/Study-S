package com.example.study_s.ui.screens.auth

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.study_s.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.tasks.await

data class GoogleSignInResult(
    val idToken: String?,
    val name: String?,
    val email: String?
)

class GoogleAuthUiClient(private val context: Context) {

    private val tag = "GoogleAuthUiClient"

    private val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(context.getString(R.string.web_client_id))
        .requestEmail()
        .requestProfile()
        .build()

    private val googleSignInClient: GoogleSignInClient = GoogleSignIn.getClient(context, gso)

    /** Lấy intent để mở cửa sổ chọn tài khoản Google */
    fun getSignInIntent(): Intent = googleSignInClient.signInIntent

    /** Lấy thông tin người dùng từ kết quả Intent trả về */
    fun getSignInResultFromIntent(data: Intent?): GoogleSignInResult {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account: GoogleSignInAccount? = task.getResult(ApiException::class.java)
            Log.d(tag, "Đăng nhập Google thành công.")
            GoogleSignInResult(
                idToken = account?.idToken,
                name = account?.displayName,
                email = account?.email
            )
        } catch (e: ApiException) {
            Log.e(tag, "Google Sign-In thất bại: ${e.statusCode}")
            GoogleSignInResult(null, null, null)
        } catch (e: Exception) {
            Log.e(tag, "Lỗi chung khi lấy thông tin đăng nhập: ${e.message}")
            GoogleSignInResult(null, null, null)
        }
    }

    /** Lấy thông tin tài khoản đã đăng nhập gần nhất một cách an toàn */
    fun getSignedInUser(): GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(context)


    /** Đăng xuất khỏi Google */
    suspend fun signOut() {
        try {
            googleSignInClient.signOut().await()
            Log.d(tag, "Đăng xuất Google thành công.")
        } catch (e: Exception) {
            Log.e(tag, "Lỗi khi đăng xuất Google: ${e.message}")
        }
    }
}
