package com.example.study_s.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.study_s.ui.screens.library.UploadFileScreen
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
import com.example.study_s.ui.screens.profiles.FollowListScreen
import com.example.study_s.ui.screens.profiles.StragerProfileScreen
import com.example.study_s.ui.screens.profiles.EditProfileScreen
import com.example.study_s.ui.screens.profiles.ProfileScreen
import com.example.study_s.ui.screens.schedule.ScheduleScreen
import com.example.study_s.ui.screens.search.SearchScreen
import com.example.study_s.ui.screens.settings.PolicyScreen
import com.example.study_s.ui.screens.settings.SupportScreen
import com.example.study_s.ui.screens.splash.SplashScreen
import com.example.study_s.viewmodel.AuthViewModel
import com.example.study_s.viewmodel.AuthViewModelFactory
import com.example.study_s.ui.screens.post.MyPostsScreen
import java.net.URLDecoder
import com.example.study_s.ui.screens.settings.SettingScreen
import com.example.study_s.ui.screens.post.SavedPostsScreen

@Composable
fun NavGraph(navController: NavHostController) {
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory())

    NavHost(
        navController = navController,
        startDestination = Routes.Splash
    ) {
        composable(Routes.MyPosts) {
            MyPostsScreen(navController)
        }

        // 🌀 Splash
        composable(Routes.Splash) {
            SplashScreen(navController)
        }

        // 🔐 Auth
        composable(Routes.Login) {
            LoginScreen(navController = navController, authViewModel = authViewModel)
        }

        composable(
            route = "${Routes.Register}?name={name}&email={email}",
            arguments = listOf(
                navArgument("name") { type = NavType.StringType; defaultValue = "" },
                navArgument("email") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val name = backStackEntry.arguments?.getString("name") ?: ""
            val email = backStackEntry.arguments?.getString("email") ?: ""
            val decodedName = URLDecoder.decode(name, "UTF-8")
            val decodedEmail = URLDecoder.decode(email, "UTF-8")
            RegisterScreen(
                navController = navController,
                authViewModel = authViewModel,
                nameFromGoogle = decodedName,
                emailFromGoogle = decodedEmail
            )
        }

        composable(Routes.ForgotPassword) {
            ForgotPasswordScreen(
                onBackToLogin = { navController.popBackStack() },
                onResetPassword = { _ -> navController.navigate(Routes.VerifyCode) }
            )
        }

        composable(Routes.VerifyCode) { VerifyCodeScreen(navController) }

        // 🏠 Home
        composable(Routes.Home) { HomeScreen(navController) }

        // 📝 Post
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
            UploadFileScreen(navController, fileUrl, fileName)
        }

        // 👤 Profile cá nhân
        composable(Routes.Profile) { ProfileScreen(navController) }
        composable(Routes.EditProfile) { EditProfileScreen(navController) }

        // ✅ 👇 THÊM ROUTE XEM HỒ SƠ NGƯỜI KHÁC (STRANGER SCREEN)
        composable(
            route = "strager/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            StragerProfileScreen(navController = navController, userId = userId)
        }

        // 👥 Group
        composable(Routes.GroupList) { GroupScreen(navController) }

        composable(
            route = "${Routes.GroupChat}/{groupId}",
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
            ChatGroupScreen(navController = navController, groupId = groupId)
        }

        composable(Routes.GroupCreate) { GroupCreateScreen(navController) }

        // 💬 Message
        composable(Routes.Message) { MessageListScreen() }

        // 📚 Library
        composable(Routes.Library) { LibraryScreen(navController) }
        composable(Routes.UploadFile) { UploadFileScreen(navController = navController, fileUrl = null, fileName = null) }

        // 📅 Schedule
        composable(Routes.Schedule) { ScheduleScreen(navController) }

        // 🔔 Notification
        composable(Routes.Notification) { NotificationScreen() }

        // 🔍 Search
        composable(Routes.Search) { SearchScreen() }

        // ⚙️ Settings
        composable(Routes.Settings) {SettingScreen(navController)}
        composable(Routes.Policy) { PolicyScreen(navController) }
        composable(Routes.Support) { SupportScreen(navController) }
        composable(Routes.SavedPosts) {
            SavedPostsScreen(navController = navController)
        }

        // Thêm điểm đến mới cho FollowListScreen
        composable(
            route = "${Routes.FollowList}/{userId}/{listType}",
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType },
                navArgument("listType") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            val listType = backStackEntry.arguments?.getString("listType") ?: ""
            FollowListScreen(navController = navController, userId = userId, listType = listType)
        }
    }
}
