package com.example.orderlistapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.example.orderlistapp.ui.screens.MainDashboard
import com.example.orderlistapp.ui.screens.SplashScreen
import com.example.orderlistapp.ui.theme.OrderlistAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OrderlistAppTheme {
                var showSplash by remember { mutableStateOf(true) }

                if (showSplash) {
                    SplashScreen(onSplashComplete = { showSplash = false })
                } else {
                    MainDashboard()
                }
            }
        }
    }
}