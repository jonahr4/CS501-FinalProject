package com.example.cs501_mealmapproject.ui.mealplan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.BrunchDining
import androidx.compose.material.icons.outlined.DinnerDining
import androidx.compose.material.icons.outlined.LunchDining
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import android.util.Log
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.cs501_mealmapproject.ui.theme.CS501MealMapProjectTheme
import java.time.LocalDate

@Composable
fun MealPlanScreen(
    mealPlanViewModel: MealPlanViewModel,
    onNavigateToRecipes: (LocalDate, String) -> Unit = { _, _ -> },
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
    onNavigateToRecipes: (LocalDate, String) -> Unit = { _, _ -> },
    onRemoveMeal: (LocalDate, String) -> Unit = { _, _ -> }
) {
    var pendingDeletion by remember { mutableStateOf<Pair<LocalDate, String>?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Plan your week",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Assign meals to each day to keep groceries and nutrition aligned.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(plan) { dailyPlan ->
                val isToday = dailyPlan.date == LocalDate.now()
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFC8E6C9) // Darker mint green background
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = dailyPlan.date.format(MealPlanDayFormatter),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            if (isToday) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .background(
                                            MaterialTheme.colorScheme.primary,
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Today",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                        
                        dailyPlan.meals.forEach { mealSlot ->
                            val isPlaceholder = mealSlot.recipeName == "Tap to add a recipe"
                            val mealColor = getMealColor(mealSlot.mealType)
                            val mealIcon = getMealIcon(mealSlot.mealType)
                            
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNavigateToRecipes(dailyPlan.date, mealSlot.mealType) },
                                colors = CardDefaults.cardColors(
                                    containerColor = mealColor.copy(alpha = 0.4f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(mealColor),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = mealIcon,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = mealSlot.mealType,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = mealSlot.recipeName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (isPlaceholder) 
                                                    getPlaceholderColor(mealSlot.mealType) // Darker than icon
                                                else 
                                                    MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                    
                                    if (!isPlaceholder) {
                                        IconButton(onClick = { pendingDeletion = dailyPlan.date to mealSlot.mealType }) {
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

    pendingDeletion?.let { (date, mealType) ->
        AlertDialog(
            onDismissRequest = { pendingDeletion = null },
            title = { Text("Remove meal?") },
            text = { Text("Remove $mealType on ${date.format(MealPlanDayFormatter)}?") },
            confirmButton = {
                TextButton(onClick = {
                    onRemoveMeal(date, mealType)
                    pendingDeletion = null
                }) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeletion = null }) {
                    Text("No")
                }
            }
        )
    }
}

@Composable
private fun getMealColor(mealType: String): Color = when (mealType) {
    "Breakfast" -> Color(0xFF66BB6A) // Light-medium green
    "Lunch" -> Color(0xFF4CAF50)     // Medium green
    "Dinner" -> Color(0xFF2E7D32)    // Dark green
    else -> Color(0xFF66BB6A)
}

@Composable
private fun getPlaceholderColor(mealType: String): Color = when (mealType) {
    "Breakfast" -> Color(0xFF388E3C) // Darker than breakfast icon
    "Lunch" -> Color(0xFF2E7D32)     // Darker than lunch icon
    "Dinner" -> Color(0xFF1B5E20)    // Darker than dinner icon
    else -> Color(0xFF388E3C)
}

@Composable
private fun getMealIcon(mealType: String): ImageVector = when (mealType) {
    "Breakfast" -> Icons.Outlined.BrunchDining
    "Lunch" -> Icons.Outlined.LunchDining
    "Dinner" -> Icons.Outlined.DinnerDining
    else -> Icons.Outlined.LunchDining
}

@Preview(showBackground = true)
@Composable
private fun MealPlanScreenPreview() {
    CS501MealMapProjectTheme {
        MealPlanContent(
            plan = previewWeekPlan(),
            onNavigateToRecipes = { _, _ -> }
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
