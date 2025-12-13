package com.example.cs501_mealmapproject.ui.foodlog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cs501_mealmapproject.data.nutrition.FoodItem
import com.example.cs501_mealmapproject.ui.scanner.BarcodeScannerView

@Composable
fun FoodLogScreen(
    modifier: Modifier = Modifier,
    foodLogViewModel: FoodLogViewModel = viewModel(),
    plannedMeals: List<PlannedMealForLog> = emptyList()
) {
    val uiState by foodLogViewModel.uiState.collectAsState()
    var showScanner by remember { mutableStateOf(false) }
    var showManualEntryDialog by remember { mutableStateOf(false) }
    var showFoodSearch by remember { mutableStateOf(false) }
    var selectedMealType by remember { mutableStateOf(MealType.SNACK) }
    var selectedFoodItem by remember { mutableStateOf<FoodItem?>(null) }

    // Dialog for logging a selected food item
    selectedFoodItem?.let { food ->
        LogFoodDialog(
            foodItem = food,
            initialMealType = selectedMealType,
            onDismiss = { selectedFoodItem = null },
            onConfirm = { servings, mealType ->
                foodLogViewModel.logFoodItem(food, servings, mealType)
                selectedFoodItem = null
                showFoodSearch = false
                foodLogViewModel.clearSearch()
            }
        )
    }

    if (showManualEntryDialog) {
        EnhancedManualEntryDialog(
            onDismiss = { showManualEntryDialog = false },
            onConfirm = { entry ->
                foodLogViewModel.addManualLog(
                    mealName = entry.mealName,
                    calories = entry.calories,
                    protein = entry.protein,
                    carbs = entry.carbs,
                    fat = entry.fat,
                    mealType = entry.mealType
                )
                showManualEntryDialog = false
            }
        )
    }

    if (showScanner) {
        Box(modifier = modifier.fillMaxSize()) {
            BarcodeScannerView(
                onBarcodeDetected = { barcode ->
                    foodLogViewModel.addLogFromBarcode(barcode, selectedMealType)
                    showScanner = false
                },
                modifier = Modifier.fillMaxSize()
            )
            
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
    } else if (showFoodSearch) {
        FoodSearchScreen(
            uiState = uiState,
            onSearch = { query -> foodLogViewModel.searchFoods(query) },
            onFoodSelected = { food -> selectedFoodItem = food },
            onClose = { 
                showFoodSearch = false
                foodLogViewModel.clearSearch()
            },
            modifier = modifier
        )
    } else {
        FoodLogContent(
            modifier = modifier,
            recentLogs = uiState.recentLogs,
            plannedMeals = plannedMeals,
            onScanBarcode = { mealType ->
                selectedMealType = mealType
                showScanner = true
            },
            onSearchFood = { 
                showFoodSearch = true 
            },
            onAddManual = { showManualEntryDialog = true },
            onDeleteLog = { id -> foodLogViewModel.deleteLog(id) },
            onLogPlannedMeal = { meal ->
                foodLogViewModel.logMealFromPlan(
                    recipeName = meal.recipeName,
                    mealType = meal.mealType
                )
            }
        )
    }
}

@Composable
private fun FoodSearchScreen(
    uiState: FoodLogUiState,
    onSearch: (String) -> Unit,
    onFoodSelected: (FoodItem) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
            Text(
                text = "Search Foods",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Search input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { 
                searchQuery = it
                onSearch(it)
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search for food (e.g., chicken, apple, rice)") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { 
                        searchQuery = ""
                        onSearch("")
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Search results
        when {
            uiState.isSearching -> {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.searchError != null -> {
                Text(
                    text = "Search failed: ${uiState.searchError}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            uiState.searchResults.isEmpty() && searchQuery.length >= 2 -> {
                Text(
                    text = "No results found for \"$searchQuery\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            searchQuery.length < 2 -> {
                Text(
                    text = "Enter at least 2 characters to search",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.searchResults) { food ->
                        FoodSearchResultItem(
                            food = food,
                            onClick = { onFoodSelected(food) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FoodSearchResultItem(
    food: FoodItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = food.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            food.brand?.let { brand ->
                Text(
                    text = brand,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "${food.calories} cal",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "P: ${food.protein.toInt()}g",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "C: ${food.carbs.toInt()}g",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "F: ${food.fat.toInt()}g",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                text = "Per ${food.servingSize}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LogFoodDialog(
    foodItem: FoodItem,
    initialMealType: MealType,
    onDismiss: () -> Unit,
    onConfirm: (servings: Float, mealType: MealType) -> Unit
) {
    var servings by remember { mutableFloatStateOf(1f) }
    var selectedMealType by remember { mutableStateOf(initialMealType) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Food") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = foodItem.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                foodItem.brand?.let { brand ->
                    Text(
                        text = brand,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                HorizontalDivider()
                
                // Serving size adjustment
                Text(
                    text = "Servings",
                    style = MaterialTheme.typography.labelLarge
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { if (servings > 0.5f) servings -= 0.5f }
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease")
                    }
                    
                    Text(
                        text = "%.1f".format(servings),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.width(60.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    
                    IconButton(
                        onClick = { servings += 0.5f }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase")
                    }
                    
                    Text(
                        text = "× ${foodItem.servingSize}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Calculated nutrition
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Nutrition (for ${servings} serving${if (servings != 1f) "s" else ""})",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Calories: ${(foodItem.calories * servings).toInt()}")
                            Text("Protein: ${(foodItem.protein * servings).toInt()}g")
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Carbs: ${(foodItem.carbs * servings).toInt()}g")
                            Text("Fat: ${(foodItem.fat * servings).toInt()}g")
                        }
                    }
                }
                
                HorizontalDivider()
                
                // Meal type selection
                Text(
                    text = "Log to",
                    style = MaterialTheme.typography.labelLarge
                )
                
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MealType.entries.forEach { mealType ->
                        FilterChip(
                            selected = mealType == selectedMealType,
                            onClick = { selectedMealType = mealType },
                            label = { Text(mealType.displayName) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(servings, selectedMealType) }) {
                Text("Log Food")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun FoodLogContent(
    modifier: Modifier = Modifier,
    recentLogs: List<FoodLogEntry>,
    plannedMeals: List<PlannedMealForLog>,
    onScanBarcode: (MealType) -> Unit,
    onSearchFood: () -> Unit,
    onAddManual: () -> Unit,
    onDeleteLog: (Long) -> Unit,
    onLogPlannedMeal: (PlannedMealForLog) -> Unit
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Log Food",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "Track what you eat to monitor your nutrition",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Quick actions card
        item {
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
                        text = "Add Food",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    // Primary action - Search foods
                    OutlinedButton(
                        onClick = onSearchFood,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Search Food Database")
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onScanBarcode(MealType.SNACK) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Scan Barcode")
                        }
                        OutlinedButton(
                            onClick = onAddManual,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Log Manually")
                        }
                    }
                }
            }
        }
        
        // Planned meals section (if any)
        if (plannedMeals.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "From Your Meal Plan",
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        plannedMeals.forEach { meal ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onLogPlannedMeal(meal) }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = meal.recipeName,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = meal.mealType.displayName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Log",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Recent logs
        item {
            Text(
                text = "Recent Logs",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        
        if (recentLogs.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = "No logs yet. Start by searching for food or scanning a barcode!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(recentLogs, key = { it.id }) { entry ->
                FoodLogEntryCard(
                    entry = entry,
                    onDelete = { onDeleteLog(entry.id) }
                )
            }
        }
    }
}

@Composable
private fun FoodLogEntryCard(
    entry: FoodLogEntry,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = entry.meal,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    // Meal type chip
                    Text(
                        text = entry.mealType.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                
                Text(
                    text = "${entry.calories} cal • P: ${entry.protein.toInt()}g • C: ${entry.carbs.toInt()}g • F: ${entry.fat.toInt()}g",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                if (entry.servings != 1f) {
                    Text(
                        text = "${entry.servings} × ${entry.servingSize}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Text(
                    text = entry.source,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * Data class for planned meals that can be logged
 */
data class PlannedMealForLog(
    val recipeName: String,
    val mealType: MealType
)
