package com.example.cs501_mealmapproject.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.cs501_mealmapproject.ui.dashboard.NutritionDashboardScreen
import com.example.cs501_mealmapproject.ui.foodlog.FoodLogScreen
import com.example.cs501_mealmapproject.ui.mealplan.MealPlanScreen
import com.example.cs501_mealmapproject.ui.mealplan.MealPlanViewModel
import com.example.cs501_mealmapproject.ui.recipes.RecipeDiscoveryScreen
import com.example.cs501_mealmapproject.ui.shopping.ShoppingListScreen

@Composable
fun MealMapNavHost(
    navController: NavHostController,
    mealPlanViewModel: MealPlanViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = MealMapDestination.MealPlan.route,
        modifier = modifier
    ) {
        composable(MealMapDestination.MealPlan.route) {
            MealPlanScreen(mealPlanViewModel = mealPlanViewModel)
        }
        composable(MealMapDestination.Recipes.route) {
            RecipeDiscoveryScreen(mealPlanViewModel = mealPlanViewModel)
        }
        composable(MealMapDestination.FoodLog.route) {
            FoodLogScreen()
        }
        composable(MealMapDestination.Dashboard.route) {
            NutritionDashboardScreen()
        }
        composable(MealMapDestination.Shopping.route) {
            ShoppingListScreen()
        }
    }
}
