package com.example.study_s.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun ForgotPasswordScreen(
    onBackToLogin: () -> Unit = {},
    onResetPassword: (String) -> Unit = {}
) {
    var email by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // 1. TẠO MỘT COLUMN MỚI ĐỂ NHÓM 2 TIÊU ĐỀ LẠI VỚI NHAU
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter) // Căn cả cụm này ra giữa, trên cùng
                .padding(top = 90.dp),      // Đẩy cả cụm này xuống một chút
            horizontalAlignment = Alignment.CenterHorizontally // Căn các phần tử con trong Column ra giữa
        ) {
            // Tiêu đề "Study-S"
            Text(
                text = "Study-S",
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(40.dp))
            // Tiêu đề "Quên mật khẩu"
            Text(
                text = "Quên mật khẩu",
                fontSize = 30.sp,
                fontWeight = FontWeight.Medium,

            )
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hướng dẫn
            Text(
                text = "Vui lòng nhập email để lấy mã xác nhận",
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Ô nhập email
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Nhập email để nhận mã xác nhận") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { onResetPassword(email) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF60ABD0),
                    contentColor = Color.White)
            ) {
                Text("Lấy lại mật khẩu")
            }



            Spacer(modifier = Modifier.height(12.dp))

            // Nút Quay lại đăng nhập
            OutlinedButton(
                onClick = { onBackToLogin() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Quay lại đăng nhập")

            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun ForgotPasswordPreview() {
    ForgotPasswordScreen()
}
