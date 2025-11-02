package com.example.study_s.ui.navigation
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.study_s.ui.screens.splash.SplashScreen
import com.example.study_s.ui.screens.home.HomeScreen
import com.example.study_s.ui.screens.library.LibraryScreen
import com.example.study_s.ui.screens.message.MessageListScreen
import com.example.study_s.ui.screens.schedule.ScheduleScreen
import com.example.study_s.ui.screens.notification.NotificationScreen
import com.example.study_s.ui.screens.search.SearchScreen
import com.example.study_s.ui.screens.profiles.EditProfileScreen
import com.example.study_s.ui.screens.profiles.ProfileScreen
import com.example.study_s.ui.screens.auth.LoginScreen
import com.example.study_s.ui.screens.auth.RegisterScreen
import com.example.study_s.ui.screens.auth.ForgotPasswordScreen
import com.example.study_s.ui.screens.auth.VerifyCodeScreen
import com.example.study_s.ui.screens.group.ChatGroupScreen
import com.example.study_s.ui.screens.group.GroupScreen
import com.example.study_s.ui.screens.post.NewPostScreen
import com.example.study_s.ui.screens.post.PostDetailScreen
import com.example.study_s.ui.screens.profile.StragerScreen
import com.example.study_s.ui.screens.settings.PolicyScreen
import com.example.study_s.ui.screens.settings.SupportScreen


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

        // 🔐 Auth Flow
        composable(Routes.Login) {
            LoginScreen(
                onLoginClick = { navController.navigate(Routes.Home) },
                onRegisterClick = { navController.navigate(Routes.Register) },
                onForgotPasswordClick = { navController.navigate(Routes.ForgotPassword) }
            )
        }
        composable(Routes.Register) { RegisterScreen(navController) }
        composable(Routes.ForgotPassword) { ForgotPasswordScreen(
            onBackToLogin = { navController.popBackStack() },
            onResetPassword = { _ -> navController.navigate(Routes.VerifyCode) }
        ) }
        composable(Routes.VerifyCode) { VerifyCodeScreen(navController) }

        // 🏠 Main Flow
        composable(Routes.Home) { HomeScreen(navController) }

        // Post
        composable(Routes.NewPost) { NewPostScreen() }
        composable(Routes.PostDetail) { PostDetailScreen() }

        // Profile
        composable(Routes.Profile) { ProfileScreen(navController) }
        composable(Routes.EditProfile) { EditProfileScreen(navController) }
        composable(Routes.OtherProfile) { StragerScreen() }

        // Group
        composable(Routes.GroupList) { GroupScreen(navController) }
        composable(Routes.GroupChat) { ChatGroupScreen() }

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