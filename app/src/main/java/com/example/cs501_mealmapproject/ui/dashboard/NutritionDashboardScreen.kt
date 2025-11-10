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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cs501_mealmapproject.ui.theme.CS501MealMapProjectTheme

@Composable
fun NutritionDashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: NutritionDashboardViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    NutritionDashboardContent(
        modifier = modifier,
        metrics = uiState.metrics
    )
}

// Composable function for dashboard. Currently using placeholder info
// TODO: Implement Dashboard with real data.
@Composable
private fun NutritionDashboardContent(
    modifier: Modifier = Modifier,
    metrics: List<NutritionMetric>
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Progress Tracking",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "TODO: Implement Dashboard",
            style = MaterialTheme.typography.titleMedium
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
                    text = "Engagement highlights PLACEHOLDER",
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

@Preview(showBackground = true)
@Composable
private fun NutritionDashboardPreview() {
    CS501MealMapProjectTheme {
        NutritionDashboardContent(metrics = NutritionDashboardUiState().metrics)
    }
}
