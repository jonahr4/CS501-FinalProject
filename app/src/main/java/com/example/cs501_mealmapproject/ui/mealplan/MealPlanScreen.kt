package com.example.cs501_mealmapproject.ui.mealplan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import android.util.Log
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.cs501_mealmapproject.ui.theme.CS501MealMapProjectTheme
import java.time.LocalDate

@Composable
fun MealPlanScreen(
    mealPlanViewModel: MealPlanViewModel,
    onNavigateToRecipes: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by mealPlanViewModel.uiState.collectAsState()
    MealPlanContent(
        modifier = modifier,
        plan = uiState.plan,
        onNavigateToRecipes = onNavigateToRecipes,
        onRemoveMeal = { date, mealType -> mealPlanViewModel.removeMeal(date, mealType) }
    )
}

@Composable
private fun MealPlanContent(
    modifier: Modifier = Modifier,
    plan: List<DailyMealPlan>,
    onNavigateToRecipes: () -> Unit = {},
    onRemoveMeal: (LocalDate, String) -> Unit = { _, _ -> }
) {
    
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
                            val isPlaceholder = mealSlot.recipeName == "Tap to add a recipe"
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { 
                                        // Make the whole row clickable. 
                                        // Ideally, we would pass the date/mealType to know WHERE to add the recipe.
                                        // For now, we navigate to recipes.
                                        onNavigateToRecipes() 
                                    }
                                    .padding(vertical = 4.dp), // Add some padding for touch target
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = mealSlot.mealType,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    Text(
                                        text = mealSlot.recipeName,
                                        style = if (isPlaceholder) 
                                            MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary)
                                        else 
                                            MaterialTheme.typography.bodyLarge
                                    )
                                }
                                
                                // Show delete button only if a meal is actually selected
                                if (!isPlaceholder) {
                                    IconButton(onClick = { onRemoveMeal(dailyPlan.date, mealSlot.mealType) }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Remove meal",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
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
        MealPlanContent(
            plan = previewWeekPlan(),
            onNavigateToRecipes = {}
        )
    }
}

private fun previewWeekPlan(): List<DailyMealPlan> {
    val today = LocalDate.now()
    return (0 until 7).map { offset ->
        val date = today.plusDays(offset.toLong())
        DailyMealPlan(
            date = date,
            meals = listOf(
                MealSlot("Breakfast", "Tap to add a recipe"),
                MealSlot("Lunch", "Quinoa bowl"),
                MealSlot("Dinner", "Tap to add a recipe")
            )
        )
    }
}
