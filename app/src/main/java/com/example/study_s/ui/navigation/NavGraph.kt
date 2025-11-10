package com.example.study_s.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.study_s.ui.screens.FilePreviewScreen
import com.example.study_s.ui.screens.auth.ForgotPasswordScreen
import com.example.study_s.ui.screens.auth.LoginScreen
import com.example.study_s.ui.screens.auth.RegisterScreen
import com.example.study_s.ui.screens.auth.VerifyCodeScreen
import com.example.study_s.ui.screens.group.ChatGroupScreen
import com.example.study_s.ui.screens.group.GroupCreateScreen
import com.example.study_s.ui.screens.group.GroupScreen
import com.example.study_s.ui.screens.home.HomeScreen
import com.example.study_s.ui.screens.library.LibraryScreen
import com.example.study_s.ui.screens.message.MessageListScreen
import com.example.study_s.ui.screens.notification.NotificationScreen
import com.example.study_s.ui.screens.post.NewPostScreen
import com.example.study_s.ui.screens.post.PostDetailScreen
import com.example.study_s.ui.screens.profiles.StragerScreen
import com.example.study_s.ui.screens.profiles.EditProfileScreen
import com.example.study_s.ui.screens.profiles.ProfileScreen
import com.example.study_s.ui.screens.schedule.ScheduleScreen
import com.example.study_s.ui.screens.search.SearchScreen
import com.example.study_s.ui.screens.settings.PolicyScreen
import com.example.study_s.ui.screens.settings.SupportScreen
import com.example.study_s.ui.screens.splash.SplashScreen
import com.example.study_s.viewmodel.AuthViewModel
import com.example.study_s.viewmodel.AuthViewModelFactory
import java.net.URLDecoder

@Composable
fun NavGraph(navController: NavHostController) {
    // AuthViewModel sẽ được chia sẻ cho các màn hình Auth nếu cần
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory())

    NavHost(
        navController = navController,
        startDestination = Routes.Splash
    ) {

        // 🌀 Splash
        composable(Routes.Splash) {
            SplashScreen(navController)
        }

        // 🔐 Auth Flow: Login
        // ====================================================================
        // SỬA LẠI KHỐI LOGIN: TRỞ NÊN CỰC KỲ ĐƠN GIẢN
        // ====================================================================
        composable(Routes.Login) {
            // LoginScreen mới đã tự chứa tất cả logic.
            // Chúng ta chỉ cần gọi nó và truyền NavController + ViewModel vào.
            LoginScreen(
                navController = navController,
                authViewModel = authViewModel
            )
        }

        // ====================================================================
        // SỬA LẠI KHỐI REGISTER: ĐỂ NHẬN DỮ LIỆU TỪ GOOGLE
        // ====================================================================
        composable(
            route = "${Routes.Register}?name={name}&email={email}",
            arguments = listOf(
                navArgument("name") {
                    type = NavType.StringType
                    defaultValue = "" // Giá trị mặc định khi không có dữ liệu truyền vào
                },
                navArgument("email") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val name = backStackEntry.arguments?.getString("name") ?: ""
            val email = backStackEntry.arguments?.getString("email") ?: ""
            // Giải mã URL để lấy lại các ký tự đặc biệt (dấu cách, @, ...)
            val decodedName = URLDecoder.decode(name, "UTF-8")
            val decodedEmail = URLDecoder.decode(email, "UTF-8")

            RegisterScreen(
                navController = navController,
                authViewModel = authViewModel,
                nameFromGoogle = decodedName,  // Truyền tên đã giải mã
                emailFromGoogle = decodedEmail // Truyền email đã giải mã
            )
        }

        composable(Routes.ForgotPassword) {
            ForgotPasswordScreen(
                onBackToLogin = { navController.popBackStack() },
                onResetPassword = { _ -> navController.navigate(Routes.VerifyCode) }
            )
        }
        composable(Routes.VerifyCode) { VerifyCodeScreen(navController) }

        // 🏠 Main Flow (Giữ nguyên không thay đổi)
        composable(Routes.Home) { HomeScreen(navController) }

        // Post
        composable(Routes.NewPost) { NewPostScreen(navController) }
        composable(
            route = "${Routes.PostDetail}/{postId}",
            arguments = listOf(navArgument("postId") { type = NavType.StringType })
        ) { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId") ?: ""
            PostDetailScreen(postId = postId, navController = navController)
        }

        // 📎 File Preview
        composable(
            route = "${Routes.FilePreview}?fileUrl={fileUrl}&fileName={fileName}",
            arguments = listOf(
                navArgument("fileUrl") { type = NavType.StringType; nullable = true },
                navArgument("fileName") { type = NavType.StringType; nullable = true }
            )
        ) { backStackEntry ->
            val fileUrl = backStackEntry.arguments?.getString("fileUrl")?.let {
                URLDecoder.decode(it, "UTF-8")
            }
            val fileName = backStackEntry.arguments?.getString("fileName")
            FilePreviewScreen(navController, fileUrl, fileName)
        }

        // Profile
        composable(Routes.Profile) { ProfileScreen(navController) }
        composable(Routes.EditProfile) { EditProfileScreen(navController) }
        composable(
            route = "${Routes.OtherProfile}/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")
            if (userId != null) {
                StragerScreen(navController = navController, userId = userId)
            }
        }

        // Group
        composable(Routes.GroupList) { GroupScreen(navController) }
        composable(
            route = "${Routes.GroupChat}/{groupId}",
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
            ChatGroupScreen(navController = navController, groupId = groupId)
        }
        composable(Routes.GroupCreate) { GroupCreateScreen(navController) }

        // Message
        composable(Routes.Message) { MessageListScreen() }

        // Library
        composable(Routes.Library) { LibraryScreen(navController) }

        // Schedule
        composable(Routes.Schedule) { ScheduleScreen() }

        // Notification
        composable(Routes.Notification) { NotificationScreen() }

        // Search
        composable(Routes.Search) { SearchScreen() }

        // Settings
        composable(Routes.Policy) { PolicyScreen(navController) }
        composable(Routes.Support) { SupportScreen(navController) }
    }
}
