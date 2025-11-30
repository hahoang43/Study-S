// ĐƯỜNG DẪN: ui/screens/auth/RegisterScreen.kt

package com.example.study_s.ui.screens.auth

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.study_s.R
import com.example.study_s.ui.navigation.Routes
import com.example.study_s.viewmodel.AuthState
import com.example.study_s.viewmodel.AuthViewModel
import com.example.study_s.viewmodel.AuthViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    nameFromGoogle: String,
    emailFromGoogle: String
) {
    // === PHẦN LOGIC ===
    val isLinkingAccount = nameFromGoogle.isNotEmpty() && emailFromGoogle.isNotEmpty()
    var fullName by remember { mutableStateOf(nameFromGoogle) }
    var userEmail by remember { mutableStateOf(emailFromGoogle) }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    val authState by authViewModel.state.collectAsState()
    val context = LocalContext.current

    // LaunchedEffect xử lý kết quả (Giữ nguyên)
    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthState.Success -> {
                val message = if (isLinkingAccount) "Đặt mật khẩu thành công!" else "Đăng ký thành công!"
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                navController.navigate(Routes.Home) {
                    popUpTo(0) { inclusive = true }
                }
                authViewModel.resetState()
            }
            is AuthState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                authViewModel.resetState()
            }
            else -> {}
        }
    }

    // === PHẦN GIAO DIỆN MỚI ===
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo_study),
            contentDescription = "Background",
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.08f),
            contentScale = ContentScale.Crop
        )

        Scaffold(
            containerColor = Color.Transparent, // Làm nền Scaffold trong suốt để thấy ảnh nền
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // --- Phần tiêu đề ---
                Text(
                    text = if (isLinkingAccount) "Hoàn Tất Hồ Sơ" else "Tạo tài khoản",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (isLinkingAccount) "Chỉ một bước nữa thôi, hãy đặt mật khẩu cho tài khoản của bạn."
                    else "Chào mừng bạn đến với Study-S!",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(40.dp))

                // --- Các trường nhập liệu (phong cách mới) ---
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Họ và tên") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Full Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLinkingAccount,
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = userEmail,
                    onValueChange = { userEmail = it },
                    label = { Text("Email") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLinkingAccount,
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Mật khẩu") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password") },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = "Toggle password visibility"
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))

                // ✅ Ô xác nhận mật khẩu đã có con mắt
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Xác nhận mật khẩu") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Confirm Password") },
                    trailingIcon = {
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(
                                imageVector = if (confirmPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = "Toggle confirm password visibility"
                            )
                        }
                    },
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    isError = password != confirmPassword && confirmPassword.isNotEmpty(),
                    supportingText = {
                        if (password != confirmPassword && confirmPassword.isNotEmpty()) {
                            Text("Mật khẩu xác nhận không khớp.")
                        }
                    }
                )

                Spacer(modifier = Modifier.height(40.dp))

                // --- Nút đăng ký ---
                Button(
                    onClick = {
                        if (password.length < 6) {
                            Toast.makeText(context, "Mật khẩu phải có ít nhất 6 ký tự.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (password != confirmPassword) {
                            Toast.makeText(context, "Mật khẩu xác nhận không khớp.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (isLinkingAccount) {
                            authViewModel.linkPasswordToCurrentUser(password)
                        } else {
                            if (fullName.isBlank() || userEmail.isBlank()) {
                                Toast.makeText(context, "Vui lòng điền đủ họ tên và email.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            authViewModel.signUp(fullName, userEmail, password)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(50)), // Bo tròn mạnh
                    enabled = authState !is AuthState.Loading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00897B) // Màu xanh teal giống ảnh mẫu
                    )
                ) {
                    if (authState is AuthState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    } else {
                        Text(
                            text = if (isLinkingAccount) "HOÀN TẤT" else "ĐĂNG KÝ",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // --- Nút quay lại ---
                TextButton(onClick = { navController.popBackStack() }) {
                    Text(
                        "Quay lại",
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// Sửa lại Preview để xem giao diện mới
@Preview(showBackground = true, name = "Register - Normal")
@Composable
fun RegisterScreenPreview() {
    MaterialTheme {
        RegisterScreen(
            navController = rememberNavController(),
            authViewModel = viewModel(factory = AuthViewModelFactory()),
            nameFromGoogle = "",
            emailFromGoogle = ""
        )
    }
}

@Preview(showBackground = true, name = "Register - Google Link")
@Composable
fun RegisterScreenLinkPreview() {
    MaterialTheme {
        RegisterScreen(
            navController = rememberNavController(),
            authViewModel = viewModel(factory = AuthViewModelFactory()),
            nameFromGoogle = "Son Goku",
            emailFromGoogle = "goku.saiyan@dbz.com"
        )
    }
}
