package com.example.cs501_mealmapproject.ui.foodlog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cs501_mealmapproject.ui.scanner.BarcodeScannerView
import com.example.cs501_mealmapproject.ui.theme.CS501MealMapProjectTheme

@Composable
fun FoodLogScreen(
    modifier: Modifier = Modifier,
    foodLogViewModel: FoodLogViewModel = viewModel()
) {
    val uiState by foodLogViewModel.uiState.collectAsState()
    var showScanner by remember { mutableStateOf(false) }
    var showManualEntryDialog by remember { mutableStateOf(false) }

    if (showManualEntryDialog) {
        ManualEntryDialog(
            onDismiss = { showManualEntryDialog = false },
            onConfirm = { entry ->
                foodLogViewModel.addManualLog(
                    mealName = entry.mealName,
                    calories = entry.calories,
                    protein = entry.protein,
                    carbs = entry.carbs,
                    fat = entry.fat
                )
                showManualEntryDialog = false
            }
        )
    }

    if (showScanner) {
        Box(modifier = modifier.fillMaxSize()) {
            BarcodeScannerView(
                onBarcodeDetected = { barcode ->
                    android.util.Log.d("FoodLogScreen", "Barcode detected: $barcode")
                    foodLogViewModel.addLogFromBarcode(barcode)
                    android.util.Log.d("FoodLogScreen", "Fetching product info, closing scanner")
                    showScanner = false
                },
                modifier = Modifier.fillMaxSize()
            )
            
            // Visual indicator overlay
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Point camera at barcode",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "(Emulator camera may show black screen - this works on real devices)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            
            IconButton(
                onClick = { showScanner = false },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close scanner",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    } else {
        FoodLogContent(
            modifier = modifier,
            recentLogs = uiState.recentLogs,
            onScanBarcode = { showScanner = true },
            onAddLog = { showManualEntryDialog = true },
            onDeleteLog = { id -> foodLogViewModel.deleteLog(id) }
        )
    }
}

@Composable
private fun FoodLogContent(
    modifier: Modifier = Modifier,
    recentLogs: List<FoodLogEntry>,
    onScanBarcode: () -> Unit = {},
    onAddLog: () -> Unit = {},
    onDeleteLog: (Long) -> Unit = {}
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
                FilledTonalButton(onClick = onScanBarcode) {
                    Text("Scan barcode")
                }
                OutlinedButton(onClick = { onAddLog() }) {
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
                    text = "Recent logs",
                    style = MaterialTheme.typography.titleMedium
                )
                if (recentLogs.isEmpty()) {
                    Text(
                        text = "No logs yet. Scan a barcode or log manually to get started.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    recentLogs.forEach { entry ->
                        // Wrap content in a Row to add the delete button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = entry.meal,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "${entry.calories} cal | P: ${entry.protein}g | C: ${entry.carbs}g | F: ${entry.fat}g",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = entry.source,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            // Delete Button
                            IconButton(onClick = { onDeleteLog(entry.id) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete log",
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

@Preview(showBackground = true)
@Composable
private fun FoodLogScreenPreview() {
    CS501MealMapProjectTheme {
        FoodLogContent(
            recentLogs = listOf(
                FoodLogEntry(
                    meal = "Greek Yogurt Parfait",
                    calories = 250,
                    protein = 15f,
                    carbs = 30f,
                    fat = 8f,
                    source = "Manual entry"
                ),
                FoodLogEntry(
                    meal = "Chicken Breast",
                    calories = 165,
                    protein = 31f,
                    carbs = 0f,
                    fat = 3.6f,
                    source = "Scanned • Barcode: 123456"
                )
            ),
            onScanBarcode = {},
            onAddLog = {},
            onDeleteLog = {}
        )
    }
}
