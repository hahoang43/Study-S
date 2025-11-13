package com.example.study_s.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.study_s.data.repository.GroupRepository
import com.example.study_s.data.repository.LibraryRepository
import com.example.study_s.data.repository.PostRepository
import com.example.study_s.data.repository.UserRepository
import com.example.study_s.ui.screens.auth.ForgotPasswordScreen
import com.example.study_s.ui.screens.auth.LoginScreen
import com.example.study_s.ui.screens.auth.RegisterScreen
import com.example.study_s.ui.screens.auth.VerifyCodeScreen
import com.example.study_s.ui.screens.group.ChatGroupScreen
import com.example.study_s.ui.screens.group.GroupCreateScreen
import com.example.study_s.ui.screens.group.GroupScreen
import com.example.study_s.ui.screens.home.HomeScreen
import com.example.study_s.ui.screens.library.FilePreviewScreen
import com.example.study_s.ui.screens.library.LibraryScreen
import com.example.study_s.ui.screens.library.UploadFileScreen
import com.example.study_s.ui.screens.message.MessageListScreen
import com.example.study_s.ui.screens.notification.NotificationScreen
import com.example.study_s.ui.screens.post.MyPostsScreen
import com.example.study_s.ui.screens.post.NewPostScreen
import com.example.study_s.ui.screens.post.PostDetailScreen
import com.example.study_s.ui.screens.post.SavedPostsScreen
import com.example.study_s.ui.screens.profiles.EditProfileScreen
import com.example.study_s.ui.screens.profiles.FollowListScreen
import com.example.study_s.ui.screens.profiles.ProfileScreen
import com.example.study_s.ui.screens.profiles.StragerProfileScreen
import com.example.study_s.ui.screens.schedule.ScheduleScreen
import com.example.study_s.ui.screens.search.SearchScreen
import com.example.study_s.ui.screens.settings.PolicyScreen
import com.example.study_s.ui.screens.settings.SettingScreen
import com.example.study_s.ui.screens.settings.SupportScreen
import com.example.study_s.ui.screens.splash.SplashScreen
import com.example.study_s.viewmodel.AuthViewModel
import com.example.study_s.viewmodel.AuthViewModelFactory
import com.example.study_s.viewmodel.SearchViewModel
import com.example.study_s.viewmodel.SearchViewModelFactory
import java.net.URLDecoder

@Composable
fun NavGraph(navController: NavHostController) {
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory())
    val userRepository = remember { UserRepository() }
    val postRepository = remember { PostRepository() }
    val groupRepository = remember { GroupRepository() }
    val libraryRepository = remember { LibraryRepository() }

    NavHost(
        navController = navController,
        startDestination = Routes.Splash
    ) {

        // ========== CÁC ROUTE CƠ BẢN & AUTH ==========
        composable(Routes.Splash) { SplashScreen(navController) }
        composable(Routes.Login) { LoginScreen(navController = navController, authViewModel = authViewModel) }
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
            RegisterScreen(navController, authViewModel, decodedName, decodedEmail)
        }
        composable(Routes.ForgotPassword) {
            ForgotPasswordScreen(
                onBackToLogin = { navController.popBackStack() },
                onResetPassword = { _ -> navController.navigate(Routes.VerifyCode) }
            )
        }
        composable(Routes.VerifyCode) { VerifyCodeScreen(navController) }


        // ========== CÁC MÀN HÌNH CHÍNH ==========
        composable(Routes.Home) { HomeScreen(navController) }
        composable(Routes.Message) { MessageListScreen() }
        composable(Routes.Notification) { NotificationScreen() }
        composable(Routes.Schedule) { ScheduleScreen(navController) }


        // ========== BÀI VIẾT (POST) ==========
        composable(Routes.NewPost) { NewPostScreen(navController) }
        composable(Routes.MyPosts) { MyPostsScreen(navController) }
        composable(Routes.SavedPosts) { SavedPostsScreen(navController = navController) }
        composable(
            route = "${Routes.PostDetail}/{postId}",
            arguments = listOf(navArgument("postId") { type = NavType.StringType })
        ) { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId") ?: ""
            PostDetailScreen(postId = postId, navController = navController)
        }


        // ========== HỒ SƠ (PROFILE) ==========
        composable(Routes.Profile) { ProfileScreen(navController) }
        composable(Routes.EditProfile) { EditProfileScreen(navController) }
        composable(
            route = "${Routes.OtherProfile}/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            StragerProfileScreen(navController = navController, userId = userId)
        }
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


        // ========== NHÓM (GROUP) ==========
        composable(Routes.GroupList) { GroupScreen(navController) }
        composable(Routes.GroupCreate) { GroupCreateScreen(navController) }
        // KHỐI DUY NHẤT VÀ CHÍNH XÁC cho màn hình chat của nhóm.
        composable(
            route = "${Routes.GroupChat}/{groupId}",
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
            ChatGroupScreen(navController = navController, groupId = groupId)
        }


        // ========== THƯ VIỆN (LIBRARY) & XEM TRƯỚC FILE ==========
        composable(Routes.Library) { LibraryScreen(navController) }
        composable(Routes.UploadFile) { UploadFileScreen(navController = navController, fileUrl = null, fileName = null) }
        // KHỐI DUY NHẤT VÀ CHÍNH XÁC cho màn hình xem trước file.
        composable(
            route = "${Routes.FilePreview}?fileUrl={fileUrl}&fileName={fileName}",
            arguments = listOf(
                navArgument("fileUrl") { type = NavType.StringType },
                navArgument("fileName") { type = NavType.StringType; nullable = true }
            )
        ) { backStackEntry ->
            val fileUrl = backStackEntry.arguments?.getString("fileUrl")?.let { URLDecoder.decode(it, "UTF-8") }
            val fileName = backStackEntry.arguments?.getString("fileName")
            if (fileUrl != null) {
                FilePreviewScreen(navController, fileUrl = fileUrl, fileName = fileName)
            }
        }


        // ========== TÌM KIẾM (SEARCH) ==========
        composable(Routes.Search) {
            val searchFactory = SearchViewModelFactory(userRepository, postRepository, groupRepository, libraryRepository)
            val searchViewModel: SearchViewModel = viewModel(factory = searchFactory)
            SearchScreen(navController = navController, viewModel = searchViewModel)
        }


        // ========== CÀI ĐẶT (SETTINGS) ==========
        composable(Routes.Settings) { SettingScreen(navController) }
        composable(Routes.Policy) { PolicyScreen(navController) }
        composable(Routes.Support) { SupportScreen(navController) }
    }
}
