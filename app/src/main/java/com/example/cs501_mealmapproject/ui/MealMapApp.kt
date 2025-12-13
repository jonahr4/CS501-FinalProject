package com.example.cs501_mealmapproject.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.cs501_mealmapproject.ui.auth.SignInScreen
import com.example.cs501_mealmapproject.ui.mealplan.MealPlanViewModel
import com.example.cs501_mealmapproject.ui.model.AppUser
import com.example.cs501_mealmapproject.ui.navigation.MealMapDestination
import com.example.cs501_mealmapproject.ui.navigation.MealMapNavHost
import com.example.cs501_mealmapproject.ui.onboarding.OnboardingScreen
import com.example.cs501_mealmapproject.ui.theme.CS501MealMapProjectTheme
import android.util.Log

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealMapApp() {
    val sessionViewModel: SessionViewModel = viewModel()
    val sessionState by sessionViewModel.uiState.collectAsState()
    var showProfileMenu by remember { mutableStateOf(false) }

    when {
        sessionState.user == null -> {
            SignInScreen(
                modifier = Modifier.fillMaxSize(),
                sessionViewModel = sessionViewModel
            )
        }

        !sessionState.onboardingComplete -> {
            // Onboarding screen with sign-out option
            var showOnboardingMenu by remember { mutableStateOf(false) }
            
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = { Text("Set Your Goals") },
                        actions = {
                            Box {
                                IconButton(onClick = { showOnboardingMenu = true }) {
                                    Icon(
                                        imageVector = Icons.Filled.AccountCircle,
                                        contentDescription = "Profile",
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                DropdownMenu(
                                    expanded = showOnboardingMenu,
                                    onDismissRequest = { showOnboardingMenu = false }
                                ) {
                                    // Show user info
                                    sessionState.user?.let { user ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(
                                                        text = user.displayName,
                                                        style = MaterialTheme.typography.titleSmall
                                                    )
                                                    Text(
                                                        text = user.email ?: "",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            },
                                            onClick = { },
                                            enabled = false
                                        )
                                        HorizontalDivider()
                                    }
                                    DropdownMenuItem(
                                        text = { Text("Sign Out") },
                                        onClick = {
                                            showOnboardingMenu = false
                                            sessionViewModel.signOut()
                                        }
                                    )
                                }
                            }
                        }
                    )
                }
            ) { innerPadding ->
                OnboardingScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    initialProfile = sessionState.onboardingProfile,
                    onSubmit = { profile ->
                        sessionViewModel.completeOnboarding(profile)
                    }
                )
            }
        }

        else -> {
            val navController = rememberNavController()
            val backStackEntry by navController.currentBackStackEntryAsState()
            val destinations = MealMapDestination.primaryDestinations
            val currentRoute = backStackEntry?.destination?.route
            val activeDestination = destinations.firstOrNull { it.route == currentRoute }
            val mealPlanViewModel: MealPlanViewModel = viewModel()
            
            // Set the current user on the ViewModel to load user-specific data
            val currentUserId = sessionState.user?.id
            if (currentUserId != null) {
                LaunchedEffect(currentUserId) {
                    mealPlanViewModel.setCurrentUser(currentUserId)
                }
            }

            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(text = activeDestination?.label ?: "MealMap")
                        },
                        actions = {
                            Box {
                                IconButton(onClick = { showProfileMenu = true }) {
                                    Icon(
                                        imageVector = Icons.Filled.AccountCircle,
                                        contentDescription = "Profile",
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                DropdownMenu(
                                    expanded = showProfileMenu,
                                    onDismissRequest = { showProfileMenu = false }
                                ) {
                                    // Show user info at the top of the menu
                                    sessionState.user?.let { user ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(
                                                        text = user.displayName,
                                                        style = MaterialTheme.typography.titleSmall
                                                    )
                                                    Text(
                                                        text = user.email ?: "",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            },
                                            onClick = { /* No action, just display */ },
                                            enabled = false
                                        )
                                        HorizontalDivider()
                                    }
                                    DropdownMenuItem(
                                        text = { Text("Reset Goals") },
                                        onClick = {
                                            showProfileMenu = false
                                            sessionViewModel.resetOnboarding()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Sign Out") },
                                        onClick = {
                                            showProfileMenu = false
                                            sessionViewModel.signOut()
                                        }
                                    )
                                }
                            }
                        }
                    )
                },
                bottomBar = {
                    NavigationBar(
                        // Explicitly set the container color to override any tonal elevation.
                        containerColor = MaterialTheme.colorScheme.background
                    ) {
                        destinations.forEach { destination ->
                            val selected = destination == activeDestination || (activeDestination == null && destination == destinations.first())
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (!selected) {
                                        navController.navigate(destination.route) {
                                            popUpTo(MealMapDestination.MealPlan.route) {
                                                inclusive = false
                                            }
                                            launchSingleTop = true
                                        }
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = destination.icon,
                                        contentDescription = destination.label
                                    )
                                },
                                label = { Text(destination.label) },
                                // Explicitly set the indicator color to override defaults.
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            )
                        }
                    }
                }
            ) { innerPadding ->
                MealMapNavHost(
                    navController = navController,
                    mealPlanViewModel = mealPlanViewModel,
                    onboardingProfile = sessionState.onboardingProfile,
                    currentUserId = currentUserId,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

// Other composables remain the same...

