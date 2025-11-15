package com.example.study_s
import android.Manifest // ✅ 1. THÊM IMPORT NÀY
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.example.study_s.data.repository.SettingsRepository
import com.example.study_s.ui.navigation.NavGraph
import com.example.study_s.ui.theme.Study_STheme

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Quyền đã được cấp
        } else {
            // Quyền bị từ chối
        }
    }

    private lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // ✅ 3. GỌI HÀM XIN QUYỀN NGAY KHI ACTIVITY ĐƯỢC TẠO
        askNotificationPermission()

        settingsRepository = SettingsRepository(this)

        setContent {
            val isDarkTheme by settingsRepository.isDarkTheme.collectAsStateWithLifecycle(null)
            Study_STheme(
                darkTheme = when (isDarkTheme) {
                    null -> isSystemInDarkTheme()
                    else -> isDarkTheme!!
                }
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavGraph(navController = navController)
                }
            }
        }
    }

    // ✅ 4. ĐỊNH NGHĨA HÀM XIN QUYỀN
    private fun askNotificationPermission() {
        // Chỉ áp dụng cho Android 13 (API 33, tên mã TIRAMISU) trở lên
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Hiển thị hộp thoại hệ thống để hỏi quyền POST_NOTIFICATIONS
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    Study_STheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val navController = rememberNavController()
            NavGraph(navController = navController)
        }
    }
}
