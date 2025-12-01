package com.example.cs501_mealmapproject.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Today's Nutrition",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
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
            NutritionMetricCard(
                label = "Calories",
                consumed = uiState.caloriesConsumed.toFloat(),
                goal = uiState.caloriesGoal.toFloat(),
                unit = "cal",
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

        // Weight Progress Card
        if (uiState.currentWeight > 0 && uiState.goalWeight > 0) {
            item {
                Text(
                    text = "Weight Progress",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                WeightProgressCard(
                    currentWeight = uiState.currentWeight,
                    goalWeight = uiState.goalWeight
                )
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
    val progress = if (goal > 0) (consumed / goal).coerceIn(0f, 1f) else 0f
    val percentage = (progress * 100).roundToInt()

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
                consumed >= goal -> "Goal reached! 🎉"
                progress >= 0.75f -> "Almost there!"
                progress >= 0.5f -> "Halfway to goal"
                progress > 0f -> "Keep going!"
                else -> "No intake logged yet"
            }

            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WeightProgressCard(
    currentWeight: Float,
    goalWeight: Float
) {
    val difference = currentWeight - goalWeight
    val isGaining = difference < 0

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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Current Weight",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${currentWeight.roundToInt()} lbs",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Goal Weight",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${goalWeight.roundToInt()} lbs",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (isGaining) {
                    "${kotlin.math.abs(difference).roundToInt()} lbs to gain"
                } else {
                    "${kotlin.math.abs(difference).roundToInt()} lbs to lose"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                currentWeight = 165f,
                goalWeight = 150f,
                mealsLoggedToday = 3
            )
        )
    }
}
