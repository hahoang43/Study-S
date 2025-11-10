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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.study_s.ui.navigation.Routes
import com.example.study_s.viewmodel.AuthState
import com.example.study_s.viewmodel.AuthViewModel
import com.example.study_s.viewmodel.AuthViewModelFactory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    // ===== BƯỚC 1: SỬA LẠI CHỮ KÝ HÀM =====
    navController: NavController,
    authViewModel: AuthViewModel,
    nameFromGoogle: String,
    emailFromGoogle: String
) {
    // === PHẦN LOGIC ===

    // Kiểm tra xem đây có phải là luồng liên kết tài khoản từ Google không
    val isLinkingAccount = nameFromGoogle.isNotEmpty() && emailFromGoogle.isNotEmpty()

    // Khởi tạo state với giá trị từ Google (nếu có)
    var fullName by remember { mutableStateOf(nameFromGoogle) }
    var userEmail by remember { mutableStateOf(emailFromGoogle) }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") } // Thêm state cho ô xác nhận mật khẩu
    var showPassword by remember { mutableStateOf(false) }

    // Các trường thông tin thêm của bạn
    var school by remember { mutableStateOf("") }
    var major by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }

    val authState by authViewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // LaunchedEffect để xử lý kết quả và điều hướng
    LaunchedEffect(authState) {
        val state = authState
        if (state is AuthState.Success) {
            val message = if (isLinkingAccount) "Đặt mật khẩu thành công!" else "Đăng ký thành công!"
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            // Sau khi thành công, đi đến màn hình Home
            navController.navigate(Routes.Home) {
                popUpTo(0) { inclusive = true }
            }
            authViewModel.resetState()
        } else if (state is AuthState.Error) {
            Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
            authViewModel.resetState()
        }
    }

    // === PHẦN GIAO DIỆN ===

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // ===== BƯỚC 2: SỬA LẠI TIÊU ĐỀ CHO THÂN THIỆN HƠN =====
            Text(
                text = if (isLinkingAccount) "Hoàn Tất Đăng Ký" else "Đăng Ký",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
            if (isLinkingAccount) {
                Text(
                    "Chào mừng bạn! Vui lòng đặt mật khẩu và điền các thông tin còn lại.",
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- Các trường nhập liệu ---
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Họ và tên") },
                placeholder = { Text("Tên của bạn") },
                modifier = Modifier.fillMaxWidth(),
                // Vô hiệu hóa ô này nếu đang trong luồng từ Google
                enabled = !isLinkingAccount
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = userEmail,
                onValueChange = { userEmail = it },
                label = { Text("Email") },
                placeholder = { Text("Nhập Email của bạn") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                // Vô hiệu hóa ô này nếu đang trong luồng từ Google
                enabled = !isLinkingAccount
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Mật khẩu") },
                placeholder = { Text("Nhập vào đây (tối thiểu 6 ký tự)") },
                trailingIcon = {
                    val icon = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(imageVector = icon, contentDescription = "Toggle password visibility")
                    }
                },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Thêm ô Xác nhận mật khẩu
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Xác nhận mật khẩu") },
                placeholder = { Text("Nhập lại mật khẩu") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                isError = password != confirmPassword && confirmPassword.isNotEmpty()
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Các trường của bạn vẫn giữ nguyên
            OutlinedTextField(
                value = school,
                onValueChange = { school = it },
                label = { Text("Tên trường") },
                placeholder = { Text("Chọn ở đây") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = major,
                onValueChange = { major = it },
                label = { Text("Ngành học") },
                placeholder = { Text("Chọn ở đây") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = year,
                onValueChange = { year = it },
                label = { Text("Năm học") },
                placeholder = { Text("VD: 2025") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- Nút đăng ký / Hoàn tất ---
            // ===== BƯỚC 3: SỬA LẠI LOGIC NÚT NHẤN =====
            Button(
                onClick = {
                    // Kiểm tra chung cho cả hai trường hợp
                    if (password.length < 6) {
                        Toast.makeText(context, "Mật khẩu phải có ít nhất 6 ký tự.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (password != confirmPassword) {
                        Toast.makeText(context, "Mật khẩu xác nhận không khớp.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    // Gọi hàm tương ứng trong ViewModel
                    if (isLinkingAccount) {
                        // Gọi hàm đặt mật khẩu cho tài khoản Google
                        authViewModel.linkPasswordToCurrentUser(password)
                    } else {
                        // Đăng ký tài khoản thường
                        if (fullName.isBlank() || userEmail.isBlank()) {
                            Toast.makeText(context, "Vui lòng điền đủ họ tên và email.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        authViewModel.signUp(fullName, userEmail, password)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = authState !is AuthState.Loading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2196F3),
                    contentColor = Color.White
                )
            ) {
                if (authState is AuthState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White
                    )
                } else {
                    // Thay đổi text của nút cho phù hợp
                    Text(if (isLinkingAccount) "Hoàn Tất" else "Đăng Ký")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- Nút quay lại ---
            OutlinedButton(
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
