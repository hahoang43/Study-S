package com.example.study_s

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.example.study_s.ui.navigation.NavGraph
import com.example.study_s.ui.theme.Study_STheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Study_STheme {
                val navController = rememberNavController()
                NavGraph(navController = navController)
            }
        }
    }
}
