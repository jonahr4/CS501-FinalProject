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
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.cs501_mealmapproject.ui.theme.CS501MealMapProjectTheme

private val sampleMealPlan = listOf(
    DailyMealPlan(
        day = "Monday",
        meals = listOf(
            MealSlot("Breakfast", "Greek yogurt parfait"),
            MealSlot("Lunch", "Quinoa bowl with roasted veggies"),
            MealSlot("Dinner", "Lemon herb salmon")
        )
    ),
    DailyMealPlan(
        day = "Tuesday",
        meals = listOf(
            MealSlot("Breakfast", "Overnight oats"),
            MealSlot("Lunch", "Spicy chickpea wrap"),
            MealSlot("Dinner", "Sheet-pan chicken fajitas")
        )
    )
)

@Composable
fun MealPlanScreen(
    modifier: Modifier = Modifier,
    plan: List<DailyMealPlan> = sampleMealPlan
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Plan your week at a glance",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "Drag and drop recipes from discovery or log your own meals to keep the calendar aligned with your goals.",
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
                            text = dailyPlan.day,
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
                            text = "Add grocery items to auto-build the shopping list",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

data class DailyMealPlan(
    val day: String,
    val meals: List<MealSlot>
)

data class MealSlot(
    val mealType: String,
    val recipeName: String
)

@Preview(showBackground = true)
@Composable
private fun MealPlanScreenPreview() {
    CS501MealMapProjectTheme {
        MealPlanScreen()
    }
}
