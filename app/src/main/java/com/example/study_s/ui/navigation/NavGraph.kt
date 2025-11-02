package com.example.study_s.ui.navigation
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.study_s.ui.screens.splash.SplashScreen
import com.example.study_s.ui.screens.auth.*
import com.example.study_s.ui.screens.home.HomeScreen
import com.example.study_s.ui.screens.post.*
import com.example.study_s.ui.screens.profile.*
import com.example.study_s.ui.screens.group.*
import com.example.study_s.ui.screens.library.LibraryScreen
import com.example.study_s.ui.screens.message.MessageListScreen
import com.example.study_s.ui.screens.schedule.ScheduleScreen
import com.example.study_s.ui.screens.notification.NotificationScreen
import com.example.study_s.ui.screens.search.SearchScreen
import com.example.study_s.ui.screens.settings.*
import com.example.study_s.ui.screens.admin.dashboard.AdminDashboardScreen
import com.example.study_s.ui.screens.admin.usermanagement.*
import com.example.study_s.ui.screens.profiles.EditProfileScreen
import com.example.study_s.ui.screens.profiles.ProfileScreen

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
        composable(Routes.Login) { LoginScreen(navController as () -> Unit) }
        composable(Routes.Register) { RegisterScreen(navController) }
        composable(Routes.ForgotPassword) { ForgotPasswordScreen(navController) }
        composable(Routes.VerifyCode) { VerifyCodeScreen(navController) }

        // 🏠 Main Flow
        composable(Routes.Home) { HomeScreen(navController) }

        // Post
        composable(Routes.NewPost) {NewPostScreen(navController) }
        composable(Routes.PostDetail) { PostDetailScreen(navController) }

        // Profile
        composable(Routes.Profile) { ProfileScreen(navController) }
        composable(Routes.EditProfile) { EditProfileScreen(navController) }
        composable(Routes.OtherProfile) { StragerScreen(navController) }

        // Group
        composable(Routes.GroupList) { GroupScreen(navController) }
        composable(Routes.GroupChat) { ChatGroupScreen()
        // Message
        composable(Routes.Message) { MessageListScreen(navController) }

        // Library
        composable(Routes.Library) { LibraryScreen(navController) }

        // Schedule
        composable(Routes.Schedule) { ScheduleScreen(navController) }

        // Notification
        composable(Routes.Notification) { NotificationScreen(navController) }

        // Search
        composable(Routes.Search) { SearchScreen(navController) }

        // Settings
        composable(Routes.Policy) { PolicyScreen(navController) }
        composable(Routes.Support) { SupportScreen(navController) }


    }
}

}
}