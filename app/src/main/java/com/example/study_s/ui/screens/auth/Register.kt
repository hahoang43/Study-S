package com.example.study_s.ui.screens.auth

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.study_s.ui.navigation.Routes
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.study_s.viewmodel.AuthViewModel // NOTE: Đã thêm thư viện này
import com.example.study_s.viewmodel.AuthViewModelFactory // NOTE: Đã thêm thư viện này
import kotlinx.coroutines.launch // NOTE: Đã thêm thư viện này
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.study_s.viewmodel.AuthState
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    navController: NavController,
    viewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory()),
    name: String? = null,
    email: String? = null
) {
    var fullName by remember { mutableStateOf(name ?: "") }
    var userEmail by remember { mutableStateOf(email ?: "") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var school by remember { mutableStateOf("") }
    var major by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    // NOTE: Đã thêm các state để quản lý trạng thái giao diện
    val authState by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var isLoading by remember { mutableStateOf(false) }

    // NOTE: Đã thêm LaunchedEffect để theo dõi và xử lý thay đổi trạng thái từ ViewModel
    LaunchedEffect(authState) {
        isLoading = authState is AuthState.Loading // Cập nhật trạng thái loading

        val state = authState
        if (state is AuthState.Success) {
            // Đăng ký thành công, chuyển đến màn hình Home và xóa backstack
            navController.navigate(Routes.Home) {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
            }
        } else if (state is AuthState.Error) {
            // Có lỗi, hiển thị thông báo
            scope.launch {
                snackbarHostState.showSnackbar(state.message)
            }
        }
    }
    // NOTE: Đã thêm Scaffold để có thể chứa thanh thông báo lỗi (Snackbar)
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // NOTE: Đã sử dụng paddingValues từ Scaffold
                .padding(horizontal = 24.dp) // NOTE: Đã sửa lại padding từ 24.dp thành horizontal = 24.dp
                .verticalScroll(rememberScrollState()), // NOTE: Đã thêm thanh cuộn
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Study-S",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- Các trường nhập liệu (Giữ nguyên) ---
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Họ và tên") },
                placeholder = { Text("Tên của bạn") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = userEmail,
                onValueChange = { userEmail = it },
                label = { Text("Email") },
                placeholder = { Text("Nhập Email của bạn") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Mật khẩu") },
                placeholder = { Text("Nhập vào đây") },
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
            OutlinedTextField(
                value = school,
                onValueChange = { school = it },
                label = { Text("Tên trường") },
                placeholder = { Text("Chọn ở đây") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = major,
                onValueChange = { major = it },
                label = { Text("Ngành học") },
                placeholder = { Text("Chọn ở đây") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = year,
                onValueChange = { year = it },
                label = { Text("Năm học") },
                placeholder = { Text("VD: 2025") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- Nút đăng ký ---
            Button(
                // NOTE: Đã sửa lại logic onClick để gọi ViewModel
                onClick = {
                    // Kiểm tra dữ liệu đầu vào trước khi gọi đăng ký
                    if (fullName.isNotBlank() && userEmail.isNotBlank() && password.length >= 6) {
                        viewModel.signUp(fullName, userEmail, password)
                    } else {
                        // Thông báo cho người dùng nếu nhập liệu chưa hợp lệ
                        scope.launch {
                            snackbarHostState.showSnackbar("Vui lòng điền đủ họ tên, email và mật khẩu (tối thiểu 6 ký tự).")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading, // NOTE: Đã thêm để vô hiệu hóa nút khi đang tải
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2196F3), // Màu xanh dương
                    contentColor = Color.White           // Màu chữ trắng
                )
            ) {
                // NOTE: Đã sửa lại nội dung của nút để hiển thị vòng quay loading
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White
                    )
                } else {
                    Text("Đăng ký")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- Nút quay lại ---
            OutlinedButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2196F3), // Màu xanh dương
                    contentColor = Color.White           // Màu chữ trắng
                )
            ) {
                Text("Quay lại đăng nhập")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
@Preview(showBackground = true)
@Composable
fun RegisterScreenPreview() {
    RegisterScreen(navController = rememberNavController())
}
