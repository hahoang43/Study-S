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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    navController: NavController,
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
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

        // --- Họ và tên ---
        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text("Họ và tên") },
            placeholder = { Text("Tên của bạn") },
            modifier = Modifier.fillMaxWidth()
        )

        // --- Email ---
        OutlinedTextField(
            value = userEmail,
            onValueChange = { userEmail = it },
            label = { Text("Email") },
            placeholder = { Text("Nhập Email của bạn") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        // --- Mật khẩu ---
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

        // --- Tên trường ---
        OutlinedTextField(
            value = school,
            onValueChange = { school = it },
            label = { Text("Tên trường") },
            placeholder = { Text("Chọn ở đây") },
            modifier = Modifier.fillMaxWidth()
        )

        // --- Ngành học ---
        OutlinedTextField(
            value = major,
            onValueChange = { major = it },
            label = { Text("Ngành học") },
            placeholder = { Text("Chọn ở đây") },
            modifier = Modifier.fillMaxWidth()
        )

        // --- Năm học ---
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
            onClick = {
                // TODO: Thêm logic xử lý đăng ký ở đây (ví dụ: gọi ViewModel)
                navController.navigate(Routes.Home) {
                    popUpTo(Routes.Login) { inclusive = true }
                }
            },
            modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2196F3), // Màu xanh dương
            contentColor = Color.White           // Màu chữ trắng
        ) ){
            Text("Đăng ký")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- Nút quay lại ---
        OutlinedButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.fillMaxWidth() ,
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
@Preview(showBackground = true)
@Composable
fun RegisterScreenPreview() {
    RegisterScreen(navController = rememberNavController())
}
