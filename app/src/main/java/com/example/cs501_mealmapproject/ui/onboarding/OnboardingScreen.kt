package com.example.cs501_mealmapproject.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.cs501_mealmapproject.ui.theme.CS501MealMapProjectTheme

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
        goalWeightLbs = 160f,
        activityLevel = ActivityLevel.Moderate
    )
    fun Float.asUserString(fallback: String): String {
        if (this <= 0f) return fallback
        return if (this % 1f == 0f) this.toInt().toString() else this.toString()
    }

    var currentWeight by rememberSaveable(initialProfile) { mutableStateOf(defaults.currentWeightLbs.asUserString("160")) }
    var goalWeight by rememberSaveable(initialProfile) { mutableStateOf(defaults.goalWeightLbs.asUserString("160")) }
    var activityLevel by rememberSaveable(initialProfile) { mutableStateOf(defaults.activityLevel) }

    val isValid = currentWeight.isNotBlank() && goalWeight.isNotBlank()
    
    // Calculate calorie target based on weight goal
    fun calculateCalorieTarget(): Int {
        val currentLbs = currentWeight.toFloatOrNull() ?: 160f
        val goalLbs = goalWeight.toFloatOrNull() ?: currentLbs
        
        // More realistic BMR estimate: ~10-11 cal per lb body weight is a common approximation
        // This gives a reasonable baseline without needing height/age/gender
        val baseBMR = currentLbs * 10.5f
        
        // Activity multiplier for TDEE (Total Daily Energy Expenditure)
        val activityMultiplier = when (activityLevel) {
            ActivityLevel.Sedentary -> 1.2f
            ActivityLevel.Light -> 1.375f
            ActivityLevel.Moderate -> 1.55f
            ActivityLevel.Active -> 1.725f
        }
        
        val tdee = baseBMR * activityMultiplier
        
        // Safe deficit/surplus: max 500-750 cal/day for sustainable progress
        // 500 cal/day = ~1 lb/week, 750 cal/day = ~1.5 lbs/week
        val weightDiff = goalLbs - currentLbs
        val calorieAdjustment = when {
            weightDiff < -30 -> -750   // Larger deficit for significant weight loss (~1.5 lb/week)
            weightDiff < 0 -> -500     // Moderate deficit for weight loss (~1 lb/week)
            weightDiff > 30 -> 500     // Moderate surplus for significant weight gain
            weightDiff > 0 -> 300      // Small surplus for lean gain
            else -> 0                  // Maintenance
        }
        
        // Never go below 1500 calories (unsafe for most adults)
        return (tdee + calorieAdjustment).toInt().coerceIn(1500, 4000)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "Welcome to MealMap",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "Set your weight goals and we'll calculate your daily calorie target automatically.",
            style = MaterialTheme.typography.bodyMedium
        )
        OutlinedTextField(
            value = currentWeight,
            onValueChange = { currentWeight = it.filter { char -> char.isDigit() || char == '.' } },
            label = { Text("Current weight (lbs)") },
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Number
            ),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = goalWeight,
            onValueChange = { goalWeight = it.filter { char -> char.isDigit() || char == '.' } },
            label = { Text("Goal weight (lbs)") },
            supportingText = {
                val currentLbs = currentWeight.toFloatOrNull() ?: 0f
                val goalLbs = goalWeight.toFloatOrNull() ?: 0f
                val diff = goalLbs - currentLbs
                val text = when {
                    diff < -0.5f -> "Lose ${String.format("%.1f", -diff)} lbs"
                    diff > 0.5f -> "Gain ${String.format("%.1f", diff)} lbs"
                    else -> "Maintain weight"
                }
                Text(text)
            },
            keyboardOptions = KeyboardOptions.Default.copy(
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
        
        // Show calculated calorie target
        val calculatedCalories = calculateCalorieTarget()
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Your daily calorie target",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "$calculatedCalories calories",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                val currentLbs = currentWeight.toFloatOrNull() ?: 0f
                val goalLbs = goalWeight.toFloatOrNull() ?: 0f
                val weightDiff = goalLbs - currentLbs
                val (goalText, rateText) = when {
                    weightDiff < -30 -> "for weight loss" to "~1.5 lbs/week"
                    weightDiff < 0 -> "for weight loss" to "~1 lb/week"
                    weightDiff > 30 -> "for weight gain" to "~1 lb/week"
                    weightDiff > 0 -> "for weight gain" to "~0.5 lb/week"
                    else -> "for maintenance" to null
                }
                Text(
                    text = "Calculated $goalText based on your activity level",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                if (rateText != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "This target is set for a safe, sustainable rate of $rateText",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                if (isValid) {
                    onSubmit(
                        OnboardingProfile(
                            calorieTarget = calculatedCalories,
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
