package com.example.cs501_mealmapproject.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.cs501_mealmapproject.ui.dashboard.NutritionDashboardScreen
import com.example.cs501_mealmapproject.ui.dashboard.NutritionDashboardViewModel
import com.example.cs501_mealmapproject.ui.foodlog.FoodLogScreen
import com.example.cs501_mealmapproject.ui.mealplan.MealPlanScreen
import com.example.cs501_mealmapproject.ui.recipes.RecipeDiscoveryScreen
import com.example.cs501_mealmapproject.ui.shopping.ShoppingListScreen

@Composable
fun MealMapNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = MealMapDestination.MealPlan.route,
        modifier = modifier
    ) {
        composable(MealMapDestination.MealPlan.route) {
            MealPlanScreen()
        }
        composable(MealMapDestination.Recipes.route) {
            RecipeDiscoveryScreen()
        }
        composable(MealMapDestination.FoodLog.route) {
            FoodLogScreen()
        }
        composable(MealMapDestination.Dashboard.route) {
            // Get an instance of the ViewModel using the official library.
            val viewModel: NutritionDashboardViewModel = viewModel()
            // Collect the state from the ViewModel's StateFlow in a lifecycle-aware manner.
            val metrics by viewModel.metrics.collectAsStateWithLifecycle()
            // Pass the collected state down to the stateless screen.
            NutritionDashboardScreen(metrics = metrics)
        }
        composable(MealMapDestination.Shopping.route) {
            ShoppingListScreen()
        }
    }
}
