package com.example.study_s.ui.screens.auth

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
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
    // === PHẦN LOGIC === (Giữ nguyên)
    val isLinkingAccount = nameFromGoogle.isNotEmpty() && emailFromGoogle.isNotEmpty()
    var fullName by remember { mutableStateOf(nameFromGoogle) }
    var userEmail by remember { mutableStateOf(emailFromGoogle) }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    val authState by authViewModel.state.collectAsState()
    val context = LocalContext.current

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

    // === PHẦN GIAO DIỆN ===
    Scaffold { paddingValues ->
        // Sử dụng Arrangement.Center để căn giữa nội dung theo chiều dọc
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Nhóm tiêu đề và mô tả để dễ quản lý
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isLinkingAccount) "Hoàn Tất Đăng Ký" else "Đăng Ký",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (isLinkingAccount) {
                    Text(
                        text = "Chào mừng bạn! Vui lòng đặt mật khẩu và điền các thông tin còn lại.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- Các trường nhập liệu ---
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Họ và tên") },
                placeholder = { Text("Tên của bạn") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLinkingAccount,
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = userEmail,
                onValueChange = { userEmail = it },
                label = { Text("Email") },
                placeholder = { Text("Nhập Email của bạn") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLinkingAccount,
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Mật khẩu") },
                placeholder = { Text("Tối thiểu 6 ký tự") },
                trailingIcon = {
                    val icon = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(imageVector = icon, contentDescription = "Toggle password visibility")
                    }
                },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Xác nhận mật khẩu") },
                placeholder = { Text("Nhập lại mật khẩu") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = password != confirmPassword && confirmPassword.isNotEmpty(),
                supportingText = {
                    if (password != confirmPassword && confirmPassword.isNotEmpty()) {
                        Text("Mật khẩu xác nhận không khớp.")
                    }
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // --- Nút đăng ký / Hoàn tất ---
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
                    .height(48.dp),
                enabled = authState !is AuthState.Loading,
            ) {
                if (authState is AuthState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(if (isLinkingAccount) "Hoàn Tất" else "Đăng Ký")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- Nút quay lại ---
            TextButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Quay lại")
            }
        }
    }
}

// Sửa lại Preview để không bị lỗi
@Preview(showBackground = true)
@Composable
fun RegisterScreenPreview() {
    // Để xem trước, chúng ta cần truyền vào các tham số giả
    RegisterScreen(
        navController = rememberNavController(),
        authViewModel = viewModel(factory = AuthViewModelFactory()), // Cần ViewModel giả
        nameFromGoogle = "", // Giả lập đăng ký thường
        emailFromGoogle = ""
    )
}
