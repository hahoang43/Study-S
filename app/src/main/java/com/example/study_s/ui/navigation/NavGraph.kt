package com.example.study_s.ui.navigation

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.study_s.ui.screens.FilePreviewScreen
import com.example.study_s.ui.screens.auth.ForgotPasswordScreen
import com.example.study_s.ui.screens.auth.GoogleAuthUiClient
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
import com.example.study_s.viewmodel.AuthState
import com.example.study_s.viewmodel.AuthViewModel
import com.example.study_s.viewmodel.AuthViewModelFactory
import kotlinx.coroutines.launch
import java.net.URLDecoder
import android.widget.Toast
import androidx.compose.runtime.remember

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.Splash
    ) {

        // 🌀 Splash
        composable(Routes.Splash) {
            SplashScreen(navController)
        }

        // 🔐 Auth Flow: Login
        composable(Routes.Login) {

            val viewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory())
            val authState by viewModel.state.collectAsState()
            val context = LocalContext.current
            val googleAuthUiClient = remember { GoogleAuthUiClient(context) }
            val scope = rememberCoroutineScope()
            val launcher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    scope.launch {
                        val signInResult = googleAuthUiClient.getSignInResultFromIntent(result.data)
                        signInResult.idToken?.let { token ->
                            viewModel.signInWithGoogle(token)
                        }
                    }
                }
            }

            // SỬA LẠI LAUNCHEDEFFECT NÀY
            LaunchedEffect(authState) {
                if (authState is AuthState.Success) {
                    // Khi ViewModel báo thành công -> thực hiện điều hướng
                    Toast.makeText(context, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()
                    navController.navigate(Routes.Home) {
                        popUpTo(0) { inclusive = true }
                    }
                    // QUAN TRỌNG: Dọn dẹp trạng thái sau khi đã xử lý xong
                    viewModel.resetState()
                }
            }

            // GỌI LoginScreen MỘT LẦN DUY NHẤT VỚI LOGIC ĐÚNG
            LoginScreen(
                authState = authState,
                onNavigateToRegister = { navController.navigate(Routes.Register) },
                onForgotPasswordClick = { navController.navigate(Routes.ForgotPassword) },
                onLoginClick = { email, password ->
                    viewModel.signInWithEmail(email, password)
                },
                onGoogleSignInClick = {
                    scope.launch {
                        // LUÔN ĐĂNG XUẤT KHỎI PHIÊN GOOGLE CŨ TRƯỚC
                        googleAuthUiClient.signOut()
                        launcher.launch(googleAuthUiClient.getSignInIntent())
                    }
                }
            )
            // XÓA BỎ HOÀN TOÀN LỜI GỌI LoginScreen THỨ HAI BỊ LẶP LẠI
        }

        composable(
            route = "${Routes.Register}?name={name}&email={email}",
            arguments = listOf(
                navArgument("name") { defaultValue = "" },
                navArgument("email") { defaultValue = "" }
            )
        ) { backStackEntry ->
            val name = backStackEntry.arguments?.getString("name") ?: ""
            val email = backStackEntry.arguments?.getString("email") ?: ""
            RegisterScreen(
                navController=navController,
                name=name,
                email=email
            )
        }
        composable(Routes.ForgotPassword) { ForgotPasswordScreen(
            onBackToLogin = { navController.popBackStack() },
            onResetPassword = { _ -> navController.navigate(Routes.VerifyCode) }
        ) }
        composable(Routes.VerifyCode) { VerifyCodeScreen(navController) }

        // 🏠 Main Flow
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

        // 📎 File Preview (Handles both upload and preview)
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
            arguments = listOf(
                navArgument("userId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")
            if (userId != null) {
                StragerScreen(navController = navController, userId = userId)
            } else {
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
