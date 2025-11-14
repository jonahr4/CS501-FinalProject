package com.example.cs501_mealmapproject.ui.foodlog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
fun FoodLogScreen(
    modifier: Modifier = Modifier,
    foodLogViewModel: FoodLogViewModel = viewModel()
) {
    val uiState by foodLogViewModel.uiState.collectAsState()
    FoodLogContent(
        modifier = modifier,
        recentLogs = uiState.recentLogs
    )
}

// Composable function for foodLogging. Currently using placeholder info
// TODO: Implement Food Logging Functionality
@Composable
private fun FoodLogContent(
    modifier: Modifier = Modifier,
    recentLogs: List<FoodLogEntry>
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Log meals",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "Use the barcode scanner to autofill or log food manually.",
            style = MaterialTheme.typography.bodyMedium
        )
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
                    text = "Capture options",
                    style = MaterialTheme.typography.titleMedium
                )
                FilledTonalButton(onClick = { }) {
                    Text("Scan barcode")
                }
                OutlinedButton(onClick = { }) {
                    Text("Log manually")
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
                    text = "Recent logs PLACEHOLDER",
                    style = MaterialTheme.typography.titleMedium
                )
                recentLogs.forEach { entry ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = entry.meal,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = entry.source,
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
private fun FoodLogScreenPreview() {
    CS501MealMapProjectTheme {
        FoodLogContent(recentLogs = FoodLogUiState().recentLogs)
    }
}
