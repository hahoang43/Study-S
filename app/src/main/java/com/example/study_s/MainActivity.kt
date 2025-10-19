package com.example.study_s

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.study_s.ui.screens.splash.SplashScreen
import com.example.study_s.ui.screens.policy.PolicyScreen
import com.example.study_s.ui.screens.support.SupportScreen
import com.example.study_s.ui.theme.Study_STheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Study_STheme {
                val navController = rememberNavController()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "splash" // màn đầu tiên khi mở app
                    ) {
                        composable("splash") { SplashScreen(navController) }
                        composable("policy") { PolicyScreen(navController) }
                        composable("support") { SupportScreen(navController) }
                    }
                }
            }
        }
    }
}
