package com.example.cs501_mealmapproject.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cs501_mealmapproject.ui.onboarding.OnboardingProfile
import com.example.cs501_mealmapproject.ui.theme.CS501MealMapProjectTheme
import kotlin.math.roundToInt

@Composable
fun NutritionDashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: NutritionDashboardViewModel = viewModel(),
    onboardingProfile: OnboardingProfile? = null
) {
    LaunchedEffect(onboardingProfile) {
        viewModel.setOnboardingProfile(onboardingProfile)
    }

    val uiState by viewModel.uiState.collectAsState()
    NutritionDashboardContent(
        modifier = modifier,
        uiState = uiState
    )
}

@Composable
private fun NutritionDashboardContent(
    modifier: Modifier = Modifier,
    uiState: NutritionDashboardUiState
) {
    var showGoalDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today's Nutrition",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { showGoalDialog = true }) {
                    Text("How goals are calculated")
                }
            }
        }

        item {
            Text(
                text = "${uiState.mealsLoggedToday} meals logged today",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Calories Card
        item {
            CalorieRingCard(
                consumed = uiState.caloriesConsumed.toFloat(),
                goal = uiState.caloriesGoal.toFloat(),
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Macronutrients Title
        item {
            Text(
                text = "Macronutrients",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Protein Card
        item {
            NutritionMetricCard(
                label = "Protein",
                consumed = uiState.proteinConsumed,
                goal = uiState.proteinGoal,
                unit = "g",
                color = MaterialTheme.colorScheme.tertiary
            )
        }

        // Carbs Card
        item {
            NutritionMetricCard(
                label = "Carbs",
                consumed = uiState.carbsConsumed,
                goal = uiState.carbsGoal,
                unit = "g",
                color = MaterialTheme.colorScheme.secondary
            )
        }

        // Fat Card
        item {
            NutritionMetricCard(
                label = "Fat",
                consumed = uiState.fatConsumed,
                goal = uiState.fatGoal,
                unit = "g",
                color = MaterialTheme.colorScheme.error
            )
        }

    }

    if (showGoalDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            confirmButton = {
                TextButton(onClick = { showGoalDialog = false }) { Text("Close") }
            },
            title = { Text("How goals are calculated", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Calories = base goal × activity factor", style = MaterialTheme.typography.bodyLarge)
                    Text("• Protein ≈ 0.8g per lb of current weight", style = MaterialTheme.typography.bodyLarge)
                    Text("• Fat ≈ 30% of adjusted calories (9 cal/g)", style = MaterialTheme.typography.bodyLarge)
                    Text("• Carbs use remaining calories (4 cal/g)", style = MaterialTheme.typography.bodyLarge)
                    Text("• Activity factors: Sedentary 0.9, Light 1.0, Moderate 1.05, Active 1.15", style = MaterialTheme.typography.bodyLarge)
                }
            }
        )
    }
}

@Composable
private fun CalorieRingCard(
    consumed: Float,
    goal: Float,
    color: androidx.compose.ui.graphics.Color
) {
    val ratio = if (goal > 0) consumed / goal else 0f
    val ringProgress = ratio.coerceIn(0f, 1f)
    val percentage = (ratio * 100).roundToInt()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Calories",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { ringProgress },
                        modifier = Modifier.size(200.dp),
                        strokeWidth = 16.dp,
                        color = color,
                        trackColor = MaterialTheme.colorScheme.surface
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${consumed.roundToInt()} / ${goal.roundToInt()}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "calories",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$percentage% of goal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = color
                    )
                    val status = when {
                        ratio > 1.1f -> "Exceeds too much"
                        ratio > 1f -> "Over by ${((ratio - 1f) * 100).roundToInt()}%"
                        consumed >= goal -> "Goal reached"
                        ratio >= 0.75f -> "Almost there"
                        ratio >= 0.5f -> "Keep it up"
                        ratio > 0f -> "Just starting"
                        else -> "No intake logged"
                    }
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (ratio > 1.1f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun NutritionMetricCard(
    label: String,
    consumed: Float,
    goal: Float,
    unit: String,
    color: androidx.compose.ui.graphics.Color
) {
    val ratio = if (goal > 0) consumed / goal else 0f
    val progress = ratio.coerceIn(0f, 1f)
    val percentage = (ratio * 100).roundToInt()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                text = "$percentage%",
                style = MaterialTheme.typography.titleSmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp)),
                color = color,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${consumed.roundToInt()} $unit",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Goal: ${goal.roundToInt()} $unit",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Status message
            val statusText = when {
                ratio > 1.1f -> "Exceeds too much"
                ratio > 1f -> "Over goal"
                consumed >= goal -> "Goal reached! 🎉"
                ratio >= 0.75f -> "Almost there!"
                ratio >= 0.5f -> "Halfway to goal"
                ratio > 0f -> "Keep going!"
                else -> "No intake logged yet"
            }

            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                color = if (ratio > 1.1f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NutritionDashboardPreview() {
    CS501MealMapProjectTheme {
        NutritionDashboardContent(
            uiState = NutritionDashboardUiState(
                caloriesConsumed = 1650,
                caloriesGoal = 2000,
                proteinConsumed = 120f,
                proteinGoal = 150f,
                carbsConsumed = 180f,
                carbsGoal = 200f,
                fatConsumed = 55f,
                fatGoal = 67f,
                mealsLoggedToday = 3
            )
        )
    }
}
