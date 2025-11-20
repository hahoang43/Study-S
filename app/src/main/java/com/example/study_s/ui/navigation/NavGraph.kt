package com.example.study_s.ui.navigation

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
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
import com.example.study_s.ui.screens.library.LibraryScreen
import com.example.study_s.ui.screens.library.UploadFileScreen
import com.example.study_s.ui.screens.message.MessageListScreen
import com.example.study_s.ui.screens.notification.NotificationScreen
import com.example.study_s.ui.screens.post.EditPostScreen
import com.example.study_s.ui.screens.post.MyPostsScreen
import com.example.study_s.ui.screens.post.PostDetailScreen
import com.example.study_s.ui.screens.post.PostScreen
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
import com.example.study_s.viewmodel.PostViewModel
import com.example.study_s.viewmodel.SearchViewModel
import com.example.study_s.viewmodel.SearchViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import java.net.URLDecoder

@Composable
fun NavGraph(navController: NavHostController, windowSizeClass: WindowSizeClass) {
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory())
    val postViewModel: PostViewModel = viewModel()
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
        composable(Routes.Login) {
            LoginScreen(
                navController = navController,
                authViewModel = authViewModel
            )
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
            RegisterScreen(navController, authViewModel, decodedName, decodedEmail)
        }
        composable(Routes.ForgotPassword) {
            ForgotPasswordScreen(
                onBackToLogin = { navController.popBackStack() },
                onResetPassword = { _ -> navController.navigate(Routes.VerifyCode) }
            )
        }
        composable(Routes.VerifyCode) { VerifyCodeScreen(navController) }
        postGraph(navController, postViewModel, windowSizeClass)


        // ========== CÁC MÀN HÌNH CHÍNH ==========
        composable(Routes.Message) { MessageListScreen() }
        composable(Routes.Notification) {
            NotificationScreen(navController = navController)
        }
        composable(Routes.Schedule) { ScheduleScreen(navController) }


        // ========== BÀI VIẾT (POST) ==========
        composable(Routes.NewPost) { PostScreen(navController) }
        composable(Routes.SavedPosts) { SavedPostsScreen(navController = navController) }
        composable(
            route = "${Routes.PostDetail}/{postId}",
            arguments = listOf(navArgument("postId") { type = NavType.StringType })
        ) { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId") ?: ""
            PostDetailScreen(postId = postId, navController = navController)
        }

     // ========== HỒ SƠ (PROFILE) ==========
        composable(
            route = "${Routes.Profile}?userId={userId}",
            arguments = listOf(
                navArgument("userId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

            if (userId != null && userId != currentUserId) {
                StragerProfileScreen(navController = navController, userId = userId)
            } else {
                ProfileScreen(
                    navController = navController,
                    onNavigateToFollowList = { uid, listType ->
                        navController.navigate("${Routes.FollowList}/$uid/$listType")
                    },
                    windowSizeClass = windowSizeClass
                )
            }
        }
        composable(Routes.EditProfile) { EditProfileScreen(navController) }
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
        composable(
            route = "${Routes.GroupChat}/{groupId}",
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
            ChatGroupScreen(navController = navController, groupId = groupId)
        }


        // ========== THƯ VIỆN (LIBRARY) ==========
        composable(Routes.Library) { LibraryScreen(navController) }
        composable(Routes.UploadFile) {
            UploadFileScreen(navController = navController, fileUrl = null, fileName = null)
        }


        // ========== TÌM KIẾM (SEARCH) ==========
        composable(Routes.Search) {
            val searchFactory = SearchViewModelFactory(
                userRepository,
                postRepository,
                groupRepository,
                libraryRepository
            )
            val searchViewModel: SearchViewModel = viewModel(factory = searchFactory)
            SearchScreen(navController = navController, viewModel = searchViewModel)
        }


        // ========== CÀI ĐẶT (SETTINGS) ==========
        composable(Routes.Settings) { SettingScreen(navController) }
        composable(Routes.Policy) { PolicyScreen(navController) }
        composable(Routes.Support) { SupportScreen(navController) }
    }
}
fun NavGraphBuilder.postGraph(navController: NavHostController, postViewModel: PostViewModel, windowSizeClass: WindowSizeClass) {
    navigation(startDestination = Routes.Home, route = "post_flow") {

        composable(Routes.Home) {
            HomeScreen(navController = navController, viewModel = postViewModel, windowSizeClass = windowSizeClass)
        }

        composable(Routes.NewPost) {
            PostScreen(navController, viewModel = postViewModel)
        }

        composable(
            route = "${Routes.PostDetail}/{postId}",
            arguments = listOf(navArgument("postId") { type = NavType.StringType })
        ) { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId") ?: ""
            PostDetailScreen(postId = postId, navController = navController, viewModel = postViewModel)
        }

        composable(
            route = "${Routes.EditPost}/{postId}",
            arguments = listOf(navArgument("postId") { type = NavType.StringType })
        ) { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId")
            if (postId != null) {
                EditPostScreen(
                    navController = navController,
                    viewModel = postViewModel,
                    postId = postId
                )
            } else {
                navController.popBackStack()
            }
        }

        composable(Routes.MyPosts) {
            MyPostsScreen(navController, viewModel = postViewModel)
        }
        composable(Routes.SavedPosts) {
            SavedPostsScreen(navController = navController, viewModel = postViewModel)
        }
    }
}
