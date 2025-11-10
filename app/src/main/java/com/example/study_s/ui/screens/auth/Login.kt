// XÓA HẾT CÁC DÒNG IMPORT CŨ VÀ THAY BẰNG KHỐI NÀY
package com.example.study_s.ui.screens.auth

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.study_s.R
import com.example.study_s.ui.navigation.Routes
import com.example.study_s.viewmodel.AuthState
import com.example.study_s.viewmodel.AuthViewModel
import com.example.study_s.viewmodel.AuthViewModelFactory
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await // Rất quan trọng cho lỗi 'await'
import java.net.URLEncoder

// PHẦN CODE CÒN LẠI CỦA BẠN BẮT ĐẦU TỪ ĐÂY
// @Composable
// fun LoginScreen(...) { ... }


@Composable
fun LoginScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory())
) {

    // === PHẦN 1: KHAI BÁO STATE VÀ LOGIC ===

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val authState by authViewModel.state.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Google Sign-In Launcher
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            coroutineScope.launch {
                try {
                    val credentials = Identity.getSignInClient(context)
                        .getSignInCredentialFromIntent(result.data)
                    authViewModel.signInWithGoogle(credentials.googleIdToken ?: "")
                } catch (e: Exception) {
                    Toast.makeText(context, "Lỗi lấy thông tin Google: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // LaunchedEffect để xử lý kết quả đăng nhập và điều hướng
    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthState.Success -> {
                // ĐĂNG NHẬP THÀNH CÔNG
                Toast.makeText(context, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()

                // KIỂM TRA XEM NGƯỜI DÙNG CÓ MẬT KHẨU HAY CHƯA
                val user = FirebaseAuth.getInstance().currentUser
                val hasPasswordProvider = user?.providerData?.any { it.providerId == "password" } == true

                if (hasPasswordProvider) {
                    // Nếu đã có mật khẩu, vào thẳng Home
                    navController.navigate(Routes.Home) {
                        popUpTo(0) { inclusive = true }
                    }
                } else {
                    // Nếu chưa có mật khẩu (đăng nhập Google lần đầu),
                    // chuyển đến màn hình Register và truyền thông tin
                    val name = user?.displayName ?: ""
                    val email = user?.email ?: ""
                    val encodedName = URLEncoder.encode(name, "UTF-8")
                    val encodedEmail = URLEncoder.encode(email, "UTF-8")
                    navController.navigate("${Routes.Register}?name=$encodedName&email=$encodedEmail") {
                        popUpTo(0) { inclusive = true }
                    }
                }
                // Quan trọng: Reset lại state để không bị lặp lại hiệu ứng
                authViewModel.resetState()
            }
            is AuthState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                authViewModel.resetState()
            }
            else -> { /* Không làm gì với Idle và Loading */ }
        }
    }

    // === PHẦN 2: GIAO DIỆN (LẤY TỪ LoginScreenContent) ===

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header
            Text("Study-S", fontSize = 40.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(40.dp))
            Text("Đăng nhập", fontSize = 25.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Vui lòng nhập tên đăng nhập hoặc email",
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Email input
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Nhập tên đăng nhập hoặc email") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Password input
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Nhập mật khẩu") },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    val icon = if (passwordVisible) R.drawable.ic_visibility_off else R.drawable.ic_visibility
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            painter = painterResource(id = icon),
                            contentDescription = if (passwordVisible) "Ẩn mật khẩu" else "Hiện mật khẩu"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(4.dp))

            TextButton(
                onClick = { /* TODO: Điều hướng đến màn hình quên mật khẩu */ },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Quên mật khẩu", fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Login button
            OutlinedButton(
                onClick = { authViewModel.signInWithEmail(email, password) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color.Black),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFD6E6FF))
            ) {
                Text("Đăng nhập", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }

            if (authState is AuthState.Loading) {
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator(modifier = Modifier.size(32.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Google Login Button
            OutlinedButton(
                onClick = {
                    coroutineScope.launch {
                        try {
                            val googleSignInRequest = com.google.android.gms.auth.api.identity.BeginSignInRequest.builder()
                                .setGoogleIdTokenRequestOptions(
                                    com.google.android.gms.auth.api.identity.BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                                        .setSupported(true)
                                        .setServerClientId(context.getString(R.string.default_web_client_id))
                                        .setFilterByAuthorizedAccounts(false)
                                        .build()
                                )
                                .setAutoSelectEnabled(true)
                                .build()
                            val signInIntent = Identity.getSignInClient(context).beginSignIn(googleSignInRequest).await()
                            launcher.launch(IntentSenderRequest.Builder(signInIntent.pendingIntent.intentSender).build())
                        } catch (e: Exception) {
                            Toast.makeText(context, "Lỗi cấu hình Google Sign-In: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color.Gray),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFE8F0FE))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.google_logo),
                    contentDescription = "Google logo",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Đăng nhập bằng Google",
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Facebook button
            OutlinedButton(
                onClick = { /* TODO */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color.Black),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFD6E6FF))
            ) {
                Text("Đăng nhập bằng Facebook", color = Color.Black, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Chưa có tài khoản?", fontSize = 14.sp)
                TextButton(onClick = { navController.navigate(Routes.Register) }) {
                    Text("Đăng ký ngay")
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Illustration
            Image(
                painter = painterResource(id = R.drawable.hinh_avatar),
                contentDescription = "Student Illustration",
                modifier = Modifier
                    .size(180.dp)
                    .padding(bottom = 16.dp)
            )
        }
    }
}
