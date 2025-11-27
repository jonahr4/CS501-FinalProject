package com.example.cs501_mealmapproject.ui.mealplan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import android.util.Log
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.cs501_mealmapproject.ui.theme.CS501MealMapProjectTheme
import java.time.LocalDate

@Composable
fun MealPlanScreen(
    mealPlanViewModel: MealPlanViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by mealPlanViewModel.uiState.collectAsState()
    MealPlanContent(
        modifier = modifier,
        plan = uiState.plan
    )
}

// Composable function for Plan Screen. Currently using placeholder info
// TODO: Implement Planning functionality
@Composable
private fun MealPlanContent(
    modifier: Modifier = Modifier,
    plan: List<DailyMealPlan>
) {
    Log.d("MealPlanUI", "MealPlanContent recomposed with plan size=${plan.size}")
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Plan your week",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "Assign meals to each day to keep groceries and nutrition aligned.",
            style = MaterialTheme.typography.bodyMedium
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(plan) { dailyPlan ->
                Card(
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
                            text = dailyPlan.date.format(MealPlanDayFormatter),
                            style = MaterialTheme.typography.titleMedium
                        )
                        dailyPlan.meals.forEach { mealSlot ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = mealSlot.mealType,
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Text(
                                    text = mealSlot.recipeName,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap a recipe to add it here from the Recipes tab",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MealPlanScreenPreview() {
    CS501MealMapProjectTheme {
        MealPlanContent(plan = previewWeekPlan())
    }
}

private fun previewWeekPlan(): List<DailyMealPlan> {
    val today = LocalDate.now()
    return (0 until 7).map { offset ->
        val date = today.plusDays(offset.toLong())
        DailyMealPlan(
            date = date,
            meals = listOf(
                MealSlot("Breakfast", "Greek yogurt parfait"),
                MealSlot("Lunch", "Quinoa bowl"),
                MealSlot("Dinner", "Oven roasted salmon")
            )
        )
    }
}
