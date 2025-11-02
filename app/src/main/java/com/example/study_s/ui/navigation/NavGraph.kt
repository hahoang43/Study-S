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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.study_s.ui.screens.auth.ForgotPasswordScreen
import com.example.study_s.ui.screens.auth.GoogleAuthUiClient
import com.example.study_s.ui.screens.auth.LoginScreen
import com.example.study_s.ui.screens.auth.RegisterScreen
import com.example.study_s.ui.screens.auth.VerifyCodeScreen
import com.example.study_s.ui.screens.group.ChatGroupScreen
import com.example.study_s.ui.screens.group.GroupScreen
import com.example.study_s.ui.screens.home.HomeScreen
import com.example.study_s.ui.screens.library.LibraryScreen
import com.example.study_s.ui.screens.message.MessageListScreen
import com.example.study_s.ui.screens.notification.NotificationScreen
import com.example.study_s.ui.screens.post.NewPostScreen
import com.example.study_s.ui.screens.post.PostDetailScreen
import com.example.study_s.ui.screens.profile.StragerScreen
import com.example.study_s.ui.screens.profiles.EditProfileScreen
import com.example.study_s.ui.screens.profiles.ProfileScreen
import com.example.study_s.ui.screens.schedule.ScheduleScreen
import com.example.study_s.ui.screens.search.SearchScreen
import com.example.study_s.ui.screens.settings.PolicyScreen
import com.example.study_s.ui.screens.settings.SupportScreen
import com.example.study_s.ui.screens.splash.SplashScreen
import com.example.study_s.viewmodel.AuthState
import com.example.study_s.viewmodel.AuthViewModel
import kotlinx.coroutines.launch


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
            val viewModel: AuthViewModel = viewModel()
            val state by viewModel.state.collectAsState()
            val context = LocalContext.current
            val googleAuthUiClient by lazy { GoogleAuthUiClient(context) }
            val scope = rememberCoroutineScope()

            val launcher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    scope.launch {
                        val signInResult = googleAuthUiClient.getSignInResultFromIntent(result.data)
                        if (signInResult.idToken != null) {
                            viewModel.signInWithGoogle(signInResult.idToken)
                        }
                    }
                }
            }

            LaunchedEffect(state) {
                if (state is AuthState.Success) {
                    val signedInUser = googleAuthUiClient.getSignedInUser()
                    val name = signedInUser?.displayName ?: ""
                    val email = signedInUser?.email ?: ""
                    navController.navigate(
                        "${Routes.Register}?name=$name&email=$email"
                    ) {
                        popUpTo(Routes.Login) { inclusive = true }
                    }
                }
            }

            LoginScreen(
                onNavigateToHome = { navController.navigate(Routes.Home) },
                onNavigateToRegister = { navController.navigate(Routes.Register) },
                onForgotPasswordClick = { navController.navigate(Routes.ForgotPassword) },
                onLoginClick = { email, password ->
                    // TODO: Implement email/password login
                    navController.navigate(Routes.Home)
                },
                onGoogleSignInClick = {
                    scope.launch {
                        googleAuthUiClient.signOut() // Đăng xuất trước
                        launcher.launch(googleAuthUiClient.getSignInIntent())
                    }
                },
                viewModel = viewModel
            )
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
            RegisterScreen(navController, name, email)
        }
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
