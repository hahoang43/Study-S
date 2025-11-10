package com.example.study_s.ui.screens.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.study_s.R
import com.example.study_s.viewmodel.AuthState
import com.example.study_s.viewmodel.AuthViewModel

import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

@Composable
fun LoginScreen(
    authState: AuthState,
    onNavigateToRegister: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onLoginClick: (String, String) -> Unit,
    onGoogleSignInClick: () -> Unit,

) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val context = LocalContext.current

    // Hiển thị thông báo lỗi từ ViewModel
    if (authState is AuthState.Error) {
        // ... Đoạn code này giờ sẽ hoạt động vì 'context' đã tồn tại
        LaunchedEffect(authState) {
            Toast.makeText(context, authState.message, Toast.LENGTH_LONG).show()
        }
    }
    LoginScreenContent(
        email = email,
        password = password,
        onEmailChange = { email = it },
        onPasswordChange = { password = it },
        onLoginClick = {
            onLoginClick(email, password)
        },
        onForgotPasswordClick = onForgotPasswordClick,
        onFacebookLoginClick = { /* TODO: Facebook login */ },
        onGoogleLoginClick = onGoogleSignInClick,
        onNavigateToRegister = onNavigateToRegister,
        isLoading = authState is AuthState.Loading
    )
}

@Composable
private fun LoginScreenContent(
    email: String,
    password: String,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onFacebookLoginClick: () -> Unit,
    onGoogleLoginClick: () -> Unit,
    onNavigateToRegister: () -> Unit,
    isLoading: Boolean
) {
    var passwordVisible by remember { mutableStateOf(false) }

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
                onValueChange = onEmailChange,
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
                onValueChange = onPasswordChange,
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
                onClick = onForgotPasswordClick,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Quên mật khẩu", fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Login button
            OutlinedButton(
                onClick = onLoginClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color.Black),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFD6E6FF))
            ) {
                Text("Đăng nhập", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }

            if (isLoading) {
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator(modifier = Modifier.size(32.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Google Login Button
            OutlinedButton(
                onClick = onGoogleLoginClick,
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
                onClick = onFacebookLoginClick,
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
                TextButton(onClick = onNavigateToRegister) {
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
