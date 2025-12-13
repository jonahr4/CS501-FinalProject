package com.example.cs501_mealmapproject.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi // Correct import for ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions // Correct import for KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.cs501_mealmapproject.ui.theme.CS501MealMapProjectTheme

// This function is composable for oboarding
// TODO: Implement onboarding so Values inputted get saved to user
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    modifier: Modifier = Modifier,
    initialProfile: OnboardingProfile? = null,
    onSubmit: (OnboardingProfile) -> Unit = {},
    onSignOut: () -> Unit = {}
) {
    val defaults = initialProfile ?: OnboardingProfile(
        calorieTarget = 2000,
        currentWeightLbs = 160f,
        goalWeightLbs = 150f,
        activityLevel = ActivityLevel.Moderate
    )
    fun Float.asUserString(fallback: String): String {
        if (this <= 0f) return fallback
        return if (this % 1f == 0f) this.toInt().toString() else this.toString()
    }

    var calorieGoal by rememberSaveable(initialProfile) { mutableStateOf(defaults.calorieTarget.takeIf { it > 0 }?.toString() ?: "2000") }
    var currentWeight by rememberSaveable(initialProfile) { mutableStateOf(defaults.currentWeightLbs.asUserString("160")) }
    var goalWeight by rememberSaveable(initialProfile) { mutableStateOf(defaults.goalWeightLbs.asUserString("150")) }
    var activityLevel by rememberSaveable(initialProfile) { mutableStateOf(defaults.activityLevel) }

    val isValid = calorieGoal.isNotBlank() && currentWeight.isNotBlank() && goalWeight.isNotBlank()
    var showProfileMenu by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topAppBar = {
            TopAppBar(
                title = { Text("Set Your Goals") },
                actions = {
                    IconButton(onClick = { showProfileMenu = true }) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Profile")
                    }
                    DropdownMenu(
                        expanded = showProfileMenu,
                        onDismissRequest = { showProfileMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Sign Out") },
                            onClick = {
                                showProfileMenu = false
                                onSignOut()
                            }
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
        Text(
            text = "Welcome to MealMap",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "This is currently a placeholer for the onboarding screen",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "Personalize your plan to generate smarter recommendations and accurate nutrition targets.",
            style = MaterialTheme.typography.bodyMedium
        )
        OutlinedTextField(
            value = calorieGoal,
            onValueChange = { calorieGoal = it.filter { char -> char.isDigit() } },
            label = { Text("Daily calorie goal") },
            keyboardOptions = KeyboardOptions.Default.copy( // Fully qualified name no longer needed
                keyboardType = KeyboardType.Number
            ),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = currentWeight,
            onValueChange = { currentWeight = it.filter { char -> char.isDigit() || char == '.' } },
            label = { Text("Current weight (lbs)") },
            keyboardOptions = KeyboardOptions.Default.copy( // Fully qualified name no longer needed
                keyboardType = KeyboardType.Number
            ),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = goalWeight,
            onValueChange = { goalWeight = it.filter { char -> char.isDigit() || char == '.' } },
            label = { Text("Goal weight (lbs)") },
            keyboardOptions = KeyboardOptions.Default.copy( // Fully qualified name no longer needed
                keyboardType = KeyboardType.Number
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Activity level",
                style = MaterialTheme.typography.titleMedium
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActivityLevel.entries.forEach { level ->
                    FilterChip(
                        selected = level == activityLevel,
                        onClick = { activityLevel = level },
                        label = { Text(level.label) },
                        colors = FilterChipDefaults.filterChipColors()
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                if (isValid) {
                    onSubmit(
                        OnboardingProfile(
                            calorieTarget = calorieGoal.toIntOrNull() ?: 0,
                            currentWeightLbs = currentWeight.toFloatOrNull() ?: 0f,
                            goalWeightLbs = goalWeight.toFloatOrNull() ?: 0f,
                            activityLevel = activityLevel
                        )
                    )
                }
            },
            enabled = isValid,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Continue")
        }
        Text(
            text = "You can revisit these settings later from the profile area.",
            style = MaterialTheme.typography.bodySmall
        )
    }
    }
}

data class OnboardingProfile(
    val calorieTarget: Int,
    val currentWeightLbs: Float,
    val goalWeightLbs: Float,
    val activityLevel: ActivityLevel
)

enum class ActivityLevel(val label: String) {
    Sedentary("Mostly seated"),
    Light("Lightly active"),
    Moderate("Moderately active"),
    Active("Very active")
}

@Preview(showBackground = true)
@Composable
private fun OnboardingScreenPreview() {
    CS501MealMapProjectTheme {
        OnboardingScreen()
    }
}
