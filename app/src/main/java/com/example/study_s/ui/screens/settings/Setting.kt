package com.example.study_s.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.study_s.R
import com.example.study_s.data.repository.SettingsRepository
import com.example.study_s.ui.navigation.Routes
import com.example.study_s.viewmodel.*


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
    val currentUser by authViewModel.currentUser.collectAsState()
    val isDarkTheme by settingsViewModel.isDarkTheme.collectAsState()
    val deletionState by accountViewModel.deletionState.collectAsState()
    val errorMessage by accountViewModel.errorMessage.collectAsState()

    LaunchedEffect(Unit) {
        authViewModel.reloadCurrentUser()
        authViewModel.event.collect { event ->
            when (event) {
                is AuthEvent.OnSignOut -> {
                    navController.navigate(Routes.Login) { popUpTo(0) { inclusive = true } }
                }
            }
        }
    }

    LaunchedEffect(deletionState) {
        when (deletionState) {
            AccountDeletionState.SUCCESS -> {
                Toast.makeText(context, "Đã xóa tài khoản thành công.", Toast.LENGTH_LONG).show()
                navController.navigate(Routes.Login) { popUpTo(0) { inclusive = true } }
                accountViewModel.resetDeletionState()
            }
            AccountDeletionState.ERROR -> {
                errorMessage?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                accountViewModel.resetDeletionState()
            }
            else -> {}
        }
    }

    LaunchedEffect(actionState) {
        when (actionState) {
            is ProfileActionState.Success -> {
                Toast.makeText(context, actionState.message, Toast.LENGTH_LONG).show()
                showChangePasswordDialog = false
                profileViewModel.resetActionState()
                authViewModel.reloadCurrentUser()
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
            onConfirmClick = { old, new -> profileViewModel.changePassword(old, new) },
            isLoading = actionState is ProfileActionState.Loading
        )
    }

    if (deletionState == AccountDeletionState.REQUIRES_REAUTH ||
        deletionState == AccountDeletionState.DELETING ||
        deletionState == AccountDeletionState.ERROR_WRONG_PASSWORD) {
        ReauthenticationDialog(
            isLoading = deletionState == AccountDeletionState.DELETING,
            error = if (deletionState == AccountDeletionState.ERROR_WRONG_PASSWORD) errorMessage else null,
            onConfirm = { password -> accountViewModel.reauthenticateAndDeleteAccount(password) },
            onDismiss = { accountViewModel.resetDeletionState() }
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
            UserProfileSection(
                username = currentUser?.displayName ?: "Đang tải...",
                email = currentUser?.email ?: "",
                avatarUrl = currentUser?.photoUrl?.toString(),
                onProfileClicked = { /* No action */ }
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingsGroup(title = "Tài khoản") {
                SettingsItem(icon = Icons.Default.Person, title = "Chỉnh sửa hồ sơ") { navController.navigate(Routes.EditProfile) }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                SettingsItem(icon = Icons.Default.Lock, title = "Đổi mật khẩu") { showChangePasswordDialog = true }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                SettingsItem(icon = Icons.Default.BookmarkBorder, title = "Bài viết đã lưu") { navController.navigate(Routes.SavedPosts) }
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
                SettingsItem(icon = Icons.Default.Shield, title = "Chính sách bảo mật") { navController.navigate(Routes.Policy) }
            }
            SettingsGroup(title = "Hỗ trợ") {
                SettingsItem(icon = Icons.Default.ContactSupport, title = "Hỗ trợ") { navController.navigate(Routes.Support) }
            }
            SettingsGroup(title = "") {
                SettingsLogoutItem(icon = Icons.AutoMirrored.Filled.Logout, title = "Đăng xuất") { authViewModel.signOut() }
            }

            DangerZoneSection { accountViewModel.startAccountDeletionProcess() }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun UserProfileSection(username: String, email: String, avatarUrl: String?, onProfileClicked: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = "User Avatar",
            modifier = Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainer),
            contentScale = ContentScale.Crop,
            placeholder = painterResource(id = R.drawable.ic_profile),
            error = painterResource(id = R.drawable.ic_profile)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(username, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        if (title.isNotBlank()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
            )
        }
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                // SỬA: Dùng màu từ theme (xám đậm ở chế độ sáng, xám nhạt ở chế độ tối)
                .border(1.dp, MaterialTheme.colorScheme.secondary, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column { content() }
        }
    }
}

@Composable
fun SettingsItem(icon: ImageVector, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = title, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SettingsSwitchItem(icon: ImageVector, title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = title, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingsLogoutItem(icon: ImageVector, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = title, tint = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = title, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun DangerZoneSection(onDeleteAccountClicked: () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = "Vùng nguy hiểm",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                // SỬA: Dùng màu từ theme (xám đậm ở chế độ sáng, xám nhạt ở chế độ tối)
                .border(1.dp, MaterialTheme.colorScheme.secondary, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onDeleteAccountClicked)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = "Xóa tài khoản", tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Xóa tài khoản",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
                Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun ChangePasswordDialog(onDismissRequest: () -> Unit, onConfirmClick: (String, String) -> Unit, isLoading: Boolean) {
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Đổi mật khẩu") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
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
                    onValueChange = { newPassword = it; passwordError = null },
                    label = { Text("Mật khẩu mới (ít nhất 6 ký tự)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    isError = passwordError?.contains("mới") == true
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; passwordError = null },
                    label = { Text("Xác nhận mật khẩu mới") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    isError = passwordError?.contains("khớp") == true
                )
                if (passwordError != null) {
                    Text(passwordError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        newPassword.length < 6 -> passwordError = "Mật khẩu mới quá ngắn."
                        newPassword != confirmPassword -> passwordError = "Mật khẩu xác nhận không khớp."
                        else -> onConfirmClick(oldPassword, newPassword)
                    }
                },
                enabled = !isLoading
            ) { Text("Xác nhận") }
        },
        dismissButton = { TextButton(onClick = onDismissRequest) { Text("Hủy") } }
    )
}

@Composable
fun ReauthenticationDialog(isLoading: Boolean, error: String?, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Xác thực lại") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Để đảm bảo an toàn, vui lòng nhập lại mật khẩu của bạn để tiếp tục.")
                if (isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Mật khẩu") },
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(if (showPassword) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, "Toggle password visibility")
                        }
                    },
                    isError = error != null
                )
                if (error != null) {
                    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(password) }, enabled = !isLoading) { Text("Xác nhận") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}
