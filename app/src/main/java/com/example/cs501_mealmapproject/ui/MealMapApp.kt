package com.example.cs501_mealmapproject.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.cs501_mealmapproject.ui.auth.SignInScreen
import com.example.cs501_mealmapproject.ui.model.AppUser
import com.example.cs501_mealmapproject.ui.navigation.MealMapDestination
import com.example.cs501_mealmapproject.ui.navigation.MealMapNavHost
import com.example.cs501_mealmapproject.ui.onboarding.OnboardingProfile
import com.example.cs501_mealmapproject.ui.onboarding.OnboardingScreen
import com.example.cs501_mealmapproject.ui.theme.CS501MealMapProjectTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealMapApp() {
    var user by remember { mutableStateOf<AppUser?>(null) }
    var onboardingProfile by remember { mutableStateOf<OnboardingProfile?>(null) }
    var onboardingComplete by rememberSaveable { mutableStateOf(false) }
    var showProfileMenu by remember { mutableStateOf(false) }

    when {
        user == null -> {
            SignInScreen(
                modifier = Modifier.fillMaxSize(),
                onSignInClick = {
                    // TODO: Replace stub with Google OAuth sign-in result.
                    user = AppUser(
                        id = "demo-user",
                        displayName = "Demo Student",
                        email = "demo@student.edu"
                    )
                    onboardingComplete = onboardingProfile != null
                }
            )
        }

        !onboardingComplete -> {
            OnboardingScreen(
                modifier = Modifier.fillMaxSize(),
                initialProfile = onboardingProfile,
                onSubmit = { profile ->
                    onboardingProfile = profile
                    onboardingComplete = true
                    // TODO: Persist onboardingProfile to repository scoped by user.id
                }
            )
        }

        else -> {
            val navController = rememberNavController()
            val backStackEntry by navController.currentBackStackEntryAsState()
            val destinations = MealMapDestination.primaryDestinations
            val currentRoute = backStackEntry?.destination?.route
            val activeDestination = destinations.firstOrNull { it.route == currentRoute }

            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(text = activeDestination?.label ?: "MealMap")
                        },
                        actions = {
                            user?.let { currentUser ->
                                ProfileMenuAction(
                                    user = currentUser,
                                    expanded = showProfileMenu,
                                    onExpandedChange = { showProfileMenu = it },
                                    onResetGoals = {
                                        onboardingComplete = false
                                        showProfileMenu = false
                                    },
                                    onSignOut = {
                                        onboardingProfile = null
                                        onboardingComplete = false
                                        user = null
                                        showProfileMenu = false
                                        // TODO: Sign out from FirebaseAuth/Google when hooked up.
                                    }
                                )
                            }
                        }
                    )
                },
                bottomBar = {
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
            ) { innerPadding ->
                MealMapNavHost(
                    navController = navController,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

@Composable
private fun ProfileMenuAction(
    user: AppUser,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onResetGoals: () -> Unit,
    onSignOut: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = user.displayName,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Box {
            IconButton(onClick = { onExpandedChange(true) }) {
                UserAvatar(name = user.displayName)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) }
            ) {
                DropdownMenuItem(
                    text = { Text("Reset goals") },
                    onClick = onResetGoals
                )
                DropdownMenuItem(
                    text = { Text("Sign out") },
                    onClick = onSignOut
                )
            }
        }
    }
}

@Composable
private fun UserAvatar(name: String) {
    val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .size(36.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer
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
