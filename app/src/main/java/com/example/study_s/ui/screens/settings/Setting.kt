package com.example.study_s.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.study_s.data.repository.SettingsRepository
import com.example.study_s.ui.navigation.Routes
import com.example.study_s.ui.theme.Study_STheme
import com.example.study_s.viewmodel.AuthEvent
import com.example.study_s.viewmodel.AuthViewModel
import com.example.study_s.viewmodel.AuthViewModelFactory
import com.example.study_s.viewmodel.ProfileActionState
import com.example.study_s.viewmodel.ProfileViewModel
import com.example.study_s.viewmodel.ProfileViewModelFactory
import com.example.study_s.viewmodel.SettingsViewModel
import com.example.study_s.viewmodel.SettingsViewModelFactory
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.VisualTransformation
import coil.compose.AsyncImage
import com.example.study_s.R
import com.example.study_s.viewmodel.AccountViewModel
import com.example.study_s.viewmodel.AccountDeletionState
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.DeleteForever
import com.example.study_s.viewmodel.AccountViewModelFactory

import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
@Composable
fun SettingScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory()),
    profileViewModel: ProfileViewModel = viewModel(factory = ProfileViewModelFactory()),
    accountViewModel: AccountViewModel = viewModel(factory = AccountViewModelFactory()),
    settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(SettingsRepository(LocalContext.current)))
) {
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    val actionState = profileViewModel.actionState
    val context = LocalContext.current
    // ✅ BƯỚC 1: Lắng nghe thông tin người dùng từ AuthViewModel
    val currentUser by authViewModel.currentUser.collectAsState()

    val isDarkTheme by settingsViewModel.isDarkTheme.collectAsState()
    // ✅ LẮNG NGHE TRẠNG THÁI TỪ ACCOUNT VIEWMODEL
    val deletionState by accountViewModel.deletionState.collectAsState()
    val errorMessage by accountViewModel.errorMessage.collectAsState()

    // ✅ BƯỚC 2: Yêu cầu làm mới dữ liệu mỗi khi màn hình được mở
    LaunchedEffect(Unit) {
        authViewModel.reloadUserData()

        authViewModel.event.collect { event ->
            when (event) {
                is AuthEvent.OnSignOut -> {
                    navController.navigate(Routes.Login) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }
    }
    // ✅ XỬ LÝ SỰ KIỆN XÓA TÀI KHOẢN
    LaunchedEffect(deletionState) {
        when (deletionState) {
            AccountDeletionState.SUCCESS -> {
                Toast.makeText(context, "Đã xóa tài khoản thành công.", Toast.LENGTH_LONG).show()
                navController.navigate(Routes.Login) {
                    popUpTo(0) { inclusive = true }
                }
                accountViewModel.resetDeletionState()
            }

            AccountDeletionState.ERROR -> {
                errorMessage?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                accountViewModel.resetDeletionState() // Reset để người dùng có thể thử lại
            }
            // Không làm gì với các trạng thái khác để dialog tự xử lý
            else -> {}
        }
    }


    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            onDismissRequest = { showChangePasswordDialog = false },
            onConfirmClick = { old, new -> profileViewModel.changePassword(old, new) },
            isLoading = actionState is ProfileActionState.Loading
        )
    }

    LaunchedEffect(actionState) {
        when (actionState) {
            is ProfileActionState.Success -> {
                Toast.makeText(context, actionState.message, Toast.LENGTH_LONG).show()
                showChangePasswordDialog = false
                profileViewModel.resetActionState()
                authViewModel.reloadUserData()
            }

            is ProfileActionState.Failure -> {
                Toast.makeText(context, actionState.message, Toast.LENGTH_LONG).show()
                profileViewModel.resetActionState()
            }

            else -> {}
        }
    }

    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            onDismissRequest = { showChangePasswordDialog = false },
            onConfirmClick = { old, new ->
                profileViewModel.changePassword(old, new)
            },
            isLoading = actionState is ProfileActionState.Loading
        )
    }
    // ✅✅✅ DÁN ĐOẠN CODE NÀY VÀO ĐÂY ✅✅✅
    // KIỂM TRA TRẠNG THÁI VÀ HIỂN THỊ DIALOG XÁC THỰC LẠI
    if (deletionState == AccountDeletionState.REQUIRES_REAUTH ||
        deletionState == AccountDeletionState.DELETING ||
        deletionState == AccountDeletionState.ERROR_WRONG_PASSWORD) {
        ReauthenticationDialog(
            isLoading = deletionState == AccountDeletionState.DELETING,
            error = if (deletionState == AccountDeletionState.ERROR_WRONG_PASSWORD) errorMessage else null,
            onConfirm = { password ->
                accountViewModel.reauthenticateAndDeleteAccount(password)
            },
            onDismiss = {
                accountViewModel.resetDeletionState()
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // ✅ BƯỚC 3: Truyền dữ liệu động từ `currentUser` vào `UserProfileSection`
            UserProfileSection(
                username = currentUser?.displayName ?: "Đang tải...",
                email = currentUser?.email ?: "",
                avatarUrl = currentUser?.photoUrl?.toString(), // Truyền URL có thể null
                onProfileClicked = { /* Tạm thời không làm gì khi nhấn vào đây */ }
            )

            // ... (Phần còn lại của code giữ nguyên y như cũ)
            Spacer(modifier = Modifier.height(16.dp))
            SettingsGroup(title = "Tài khoản") {
                SettingsItem(
                    icon = Icons.Default.Person,
                    title = "Chỉnh sửa hồ sơ",
                    onClick = { navController.navigate(Routes.EditProfile) }
                )
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp), thickness = 0.5.dp)
                SettingsItem(
                    icon = Icons.Default.Lock,
                    title = "Đổi mật khẩu",
                    onClick = { showChangePasswordDialog = true }
                )
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp), thickness = 0.5.dp)
                SettingsItem(
                    icon = Icons.Default.BookmarkBorder,
                    title = "Bài viết đã lưu",
                    onClick = { navController.navigate(Routes.SavedPosts) }
                )
            }

            SettingsGroup(title = "Giao diện") {
                SettingsSwitchItem(
                    icon = Icons.Default.ColorLens,
                    title = "Chế độ tối",
                    checked = isDarkTheme ?: isSystemInDarkTheme(),
                    onCheckedChange = { settingsViewModel.setDarkTheme(it) }
                )
            }

            SettingsGroup(title = "Khác") {
                SettingsItem(
                    icon = Icons.Default.Shield,
                    title = "Chính sách bảo mật",
                    onClick = { navController.navigate(Routes.Policy) }
                )
            }

            SettingsGroup(title = "Hỗ trợ") {
                SettingsItem(
                    icon = Icons.Default.Shield,
                    title = "Hỗ trợ",
                    onClick = { navController.navigate(Routes.Support) }
                )
            }
            SettingsGroup(title = "") {
                SettingsLogoutItem(
                    icon = Icons.AutoMirrored.Filled.Logout,
                    title = "Đăng xuất",
                    onClick = { authViewModel.signOut() }
                )
            }
            DangerZoneSection(
                onDeleteAccountClicked = {
                    accountViewModel.startAccountDeletionProcess()
                }
            )

            Spacer(modifier = Modifier.height(32.dp)) // Thêm khoảng trống ở dưới
        }
    }
}



@Composable
fun ChangePasswordDialog(
    onDismissRequest: () -> Unit,
    onConfirmClick: (String, String) -> Unit,
    isLoading: Boolean
) {
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Đổi mật khẩu") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = oldPassword,
                    onValueChange = { oldPassword = it },
                    label = { Text("Mật khẩu cũ") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = {
                        newPassword = it
                        if (it.length >= 6) passwordError = null
                    },
                    label = { Text("Mật khẩu mới (ít nhất 6 ký tự)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    isError = passwordError != null
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        if (it == newPassword) passwordError = null
                    },
                    label = { Text("Xác nhận mật khẩu mới") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    isError = passwordError != null
                )

                passwordError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newPassword.length < 6) {
                        passwordError = "Mật khẩu mới quá ngắn."
                    } else if (newPassword != confirmPassword) {
                        passwordError = "Mật khẩu xác nhận không khớp."
                    } else {
                        passwordError = null
                        onConfirmClick(oldPassword, newPassword)
                    }
                },
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Xác nhận")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest, enabled = !isLoading) {
                Text("Hủy")
            }
        }
    )
}

@Composable
fun UserProfileSection(
    username: String,
    email: String,
    avatarUrl: String?, // Chấp nhận URL có thể là null
    onProfileClicked: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onProfileClicked)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = "User Avatar",
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
            placeholder = painterResource(id = R.drawable.ic_profile),
            error = painterResource(id = R.drawable.ic_profile)
        )
        Spacer(modifier = Modifier.size(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = username,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

    }
}


@Composable
fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        if (title.isNotEmpty()) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.size(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.size(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun SettingsLogoutItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.size(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f)
        )
    }
}
@Composable
fun DangerZoneSection(onDeleteAccountClicked: () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onDeleteAccountClicked)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.DeleteForever,
                contentDescription = "Xóa tài khoản",
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.size(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Xóa tài khoản",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Hành động này không thể hoàn tác.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}


@Composable
fun ReauthenticationDialog(
    isLoading: Boolean,
    error: String?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    // Dùng LaunchedEffect để reset lỗi khi dialog được mở lại
    LaunchedEffect(error) {
        // Có thể thêm logic ở đây nếu cần
    }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text("Xác thực lại") },
        text = {
            Column {
                Text("Vì lý do bảo mật, vui lòng nhập lại mật khẩu của bạn để xóa tài khoản vĩnh viễn.")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Mật khẩu") },
                    singleLine = true,
                    isError = error != null,
                    supportingText = { if (error != null) Text(error, color = MaterialTheme.colorScheme.error) },
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val image = if (isPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(image, "Toggle password visibility")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                if (isLoading) {
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(password) },
                enabled = !isLoading && password.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Xác nhận Xóa")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Hủy")
            }
        }
    )
}
@Preview(showBackground = true, name = "Light Mode")
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, name = "Dark Mode")
@Composable
fun SettingScreenPreview() {
    Study_STheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            SettingScreen(navController = rememberNavController())
        }
    }
}
