package com.example.cs501_mealmapproject.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
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

@Composable
fun NutritionDashboardScreen(
    modifier: Modifier = Modifier,
    metrics: List<NutritionMetric> // The screen now requires a list of metrics to be passed in.
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Track progress without the spreadsheets",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "Daily streaks, macro breakdowns, and GPS-powered grocery nudges surface right when you need them.",
            style = MaterialTheme.typography.bodyMedium
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(metrics) { metric ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth(fraction = 0.8f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = metric.label,
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = metric.value,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = metric.status,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
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
                    text = "Engagement highlights",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Daily streak: 5 days • Notifications remind users near grocery stores to check their shopping list.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Upcoming goal: Hit protein target 4 days this week for a badge unlock.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

data class NutritionMetric(
    val label: String,
    val value: String,
    val status: String
)

@Preview(showBackground = true)
@Composable
private fun NutritionDashboardPreview() {
    // The preview now defines its own sample data to pass to the stateless screen.
    val previewMetrics = listOf(
        NutritionMetric(label = "Calories", value = "1,850 / 2,000", status = "92% of goal"),
        NutritionMetric(label = "Protein", value = "110g / 125g", status = "+2 day streak"),
        NutritionMetric(label = "Carbs", value = "210g / 230g", status = "on track"),
        NutritionMetric(label = "Fats", value = "60g / 70g", status = "slightly low")
    )
    CS501MealMapProjectTheme {
        NutritionDashboardScreen(metrics = previewMetrics)
    }
}
