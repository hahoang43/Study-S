package com.example.study_s

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.study_s.ui.navigation.NavGraph
import com.example.study_s.ui.theme.Study_STheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // BƯỚC 2: Kích hoạt chế độ hiển thị tràn cạnh (edge-to-edge)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            Study_STheme {
                // BƯỚC 1: Bọc nội dung trong một Surface
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavGraph(navController = navController)
                }
            }
        }
    }
}
