package com.example.cs501_mealmapproject.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.cs501_mealmapproject.ui.navigation.MealMapDestination
import com.example.cs501_mealmapproject.ui.navigation.MealMapNavHost
import com.example.cs501_mealmapproject.ui.theme.CS501MealMapProjectTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealMapApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destinations = MealMapDestination.primaryDestinations
    val currentRoute = backStackEntry?.destination?.route
    val activeDestination = destinations.firstOrNull { it.route == currentRoute }
    val shouldShowBars = currentRoute != null || backStackEntry == null

    Scaffold(
        topBar = {
            if (shouldShowBars) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(text = activeDestination?.label ?: "MealMap")
                    }
                )
            }
        },
        bottomBar = {
            if (shouldShowBars) {
                NavigationBar {
                    destinations.forEach { destination ->
                        val selected = destination == activeDestination || (activeDestination == null && destination == destinations.first())
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (!selected) {
                                    navController.navigate(destination.route) {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = destination.label
                                )
                            },
                            label = { Text(destination.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        MealMapNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MealMapAppPreview() {
    CS501MealMapProjectTheme {
        MealMapApp()
    }
}
