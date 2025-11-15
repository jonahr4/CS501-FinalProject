package com.example.cs501_mealmapproject

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.cs501_mealmapproject.ui.MealMapApp
import com.example.cs501_mealmapproject.ui.theme.CS501MealMapProjectTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The enableEdgeToEdge() call has been removed to allow our custom theme to control system bar colors.
        setContent {
            CS501MealMapProjectTheme {
                MealMapApp()
            }
        }
    }
}
