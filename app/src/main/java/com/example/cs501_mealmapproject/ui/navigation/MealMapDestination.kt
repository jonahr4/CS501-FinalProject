package com.example.cs501_mealmapproject.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.ui.graphics.vector.ImageVector

enum class MealMapDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    MealPlan(
        route = "meal_plan",
        label = "Plan",
        icon = Icons.Outlined.CalendarMonth
    ),
    Recipes(
        route = "recipes",
        label = "Recipes",
        icon = Icons.Outlined.RestaurantMenu
    ),
    FoodLog(
        route = "food_log",
        label = "Log",
        icon = Icons.Outlined.QrCodeScanner
    ),
    Dashboard(
        route = "dashboard",
        label = "Dashboard",
        icon = Icons.Outlined.BarChart
    ),
    Shopping(
        route = "shopping",
        label = "Shopping",
        icon = Icons.Outlined.ReceiptLong
    );

    companion object {
        val primaryDestinations: List<MealMapDestination> = values().toList()
    }
}
