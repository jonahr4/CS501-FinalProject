package com.example.cs501_mealmapproject.ui.foodlog

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Bolt
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cs501_mealmapproject.data.nutrition.FoodItem
import com.example.cs501_mealmapproject.ui.scanner.BarcodeScannerView
import java.time.LocalDate

@Composable
fun FoodLogScreen(
    modifier: Modifier = Modifier,
    currentUserId: String = "",
    foodLogViewModel: FoodLogViewModel = viewModel()
) {
    val uiState by foodLogViewModel.uiState.collectAsState()
    
    // Set current user and refresh planned meals when screen loads
    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotEmpty()) {
            foodLogViewModel.setCurrentUser(currentUserId)
        }
        foodLogViewModel.refreshPlannedMeals()
    }
    
    var showScanner by remember { mutableStateOf(false) }
    var showManualEntryDialog by remember { mutableStateOf(false) }
    var showQuickAddDialog by remember { mutableStateOf(false) }
    var showFoodSearch by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<FoodLogEntry?>(null) }
    var selectedMealType by remember { mutableStateOf(MealType.SNACK) }
    var selectedFoodItem by remember { mutableStateOf<FoodItem?>(null) }
    var showRecentFoods by remember { mutableStateOf(false) }
    var showMealBuilder by remember { mutableStateOf(false) }

    // Meal Builder Dialog
    if (showMealBuilder || uiState.mealBuilder != null) {
        MealBuilderDialog(
            mealBuilder = uiState.mealBuilder,
            searchResults = uiState.searchResults,
            isSearching = uiState.isSearching,
            searchError = uiState.searchError,
            onDismiss = {
                foodLogViewModel.cancelMealBuilder()
                showMealBuilder = false
            },
            onSearch = { query -> foodLogViewModel.searchFoods(query) },
            onAddIngredient = { food, servings -> foodLogViewModel.addIngredientToBuilder(food, servings) },
            onRemoveIngredient = { index -> foodLogViewModel.removeIngredientFromBuilder(index) },
            onUpdateServings = { index, servings -> foodLogViewModel.updateIngredientServings(index, servings) },
            onUpdateMealName = { name -> foodLogViewModel.updateMealBuilderName(name) },
            onUpdateMealType = { type -> foodLogViewModel.updateMealBuilderType(type) },
            onSave = {
                foodLogViewModel.saveMealBuilder()
                showMealBuilder = false
            }
        )
    }

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
    
    // Edit entry dialog
    showEditDialog?.let { entry ->
        EditFoodLogDialog(
            entry = entry,
            onDismiss = { showEditDialog = null },
            onUpdateServings = { newServings ->
                foodLogViewModel.updateLogServings(entry.id, newServings, entry)
                showEditDialog = null
            },
            onUpdateMealType = { newMealType ->
                foodLogViewModel.updateLogMealType(entry.id, newMealType)
                showEditDialog = null
            },
            onToggleFavorite = {
                foodLogViewModel.toggleFavorite(entry.id, !entry.isFavorite)
                showEditDialog = null
            }
        )
    }

    // Manual entry dialog
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
    
    // Quick add calories dialog
    if (showQuickAddDialog) {
        QuickAddCaloriesDialog(
            onDismiss = { showQuickAddDialog = false },
            onConfirm = { calories, name, mealType ->
                foodLogViewModel.quickAddCalories(calories, mealType, name)
                showQuickAddDialog = false
            }
        )
    }
    
    // Recent foods picker
    if (showRecentFoods) {
        RecentFoodsDialog(
            recentFoods = uiState.recentUniqueFoods,
            favorites = uiState.favoriteFoods,
            onDismiss = { showRecentFoods = false },
            onSelectFood = { entry, mealType ->
                foodLogViewModel.quickRelogFood(entry, mealType)
                showRecentFoods = false
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
        EnhancedFoodLogContent(
            modifier = modifier,
            uiState = uiState,
            onScanBarcode = { mealType ->
                selectedMealType = mealType
                showScanner = true
            },
            onSearchFood = { showFoodSearch = true },
            onAddManual = { showManualEntryDialog = true },
            onQuickAdd = { showQuickAddDialog = true },
            onShowRecentFoods = { showRecentFoods = true },
            onDeleteLog = { id -> foodLogViewModel.deleteLog(id) },
            onEditLog = { entry -> showEditDialog = entry },
            onToggleFavorite = { entry ->
                foodLogViewModel.toggleFavorite(entry.id, !entry.isFavorite)
            },
            onLogPlannedMeal = { meal ->
                foodLogViewModel.logPlannedMeal(meal)
            },
            onCopyYesterday = { mealType ->
                foodLogViewModel.copyMealsFromDate(LocalDate.now().minusDays(1), mealType)
            },
            onBuildMeal = {
                foodLogViewModel.startMealBuilder(mealType = MealType.SNACK)
                showMealBuilder = true
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
    onDelete: () -> Unit,
    onEdit: () -> Unit = {},
    onToggleFavorite: () -> Unit = {}
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onEdit)
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
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = getMealTypeColor(entry.mealType)
                        )
                    ) {
                        Text(
                            text = entry.mealType.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
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
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = entry.source,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    entry.loggedTime?.let { time ->
                        Text(
                            text = "• $time",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Column {
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (entry.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (entry.isFavorite) "Remove from favorites" else "Add to favorites",
                        tint = if (entry.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
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
}

@Composable
private fun getMealTypeColor(mealType: MealType) = when (mealType) {
    MealType.BREAKFAST -> MaterialTheme.colorScheme.tertiaryContainer
    MealType.LUNCH -> MaterialTheme.colorScheme.secondaryContainer
    MealType.DINNER -> MaterialTheme.colorScheme.primaryContainer
    MealType.SNACK -> MaterialTheme.colorScheme.surfaceVariant
}

// ============== New Enhanced Composables ==============

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EnhancedFoodLogContent(
    modifier: Modifier = Modifier,
    uiState: FoodLogUiState,
    onScanBarcode: (MealType) -> Unit,
    onSearchFood: () -> Unit,
    onAddManual: () -> Unit,
    onQuickAdd: () -> Unit,
    onShowRecentFoods: () -> Unit,
    onDeleteLog: (Long) -> Unit,
    onEditLog: (FoodLogEntry) -> Unit,
    onToggleFavorite: (FoodLogEntry) -> Unit,
    onLogPlannedMeal: (PlannedMealForLog) -> Unit,
    onCopyYesterday: (MealType?) -> Unit,
    onBuildMeal: () -> Unit
) {
    var expandedMealSections by remember { mutableStateOf(setOf(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER, MealType.SNACK)) }
    
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Daily summary card
        item {
            DailySummaryCard(summary = uiState.todaysSummary)
        }
        
        // Quick actions
        item {
            QuickActionsCard(
                onSearchFood = onSearchFood,
                onScanBarcode = { onScanBarcode(MealType.SNACK) },
                onAddManual = onAddManual,
                onQuickAdd = onQuickAdd,
                onShowRecent = onShowRecentFoods,
                onCopyYesterday = { onCopyYesterday(null) },
                onBuildMeal = onBuildMeal
            )
        }
        
        // Planned meals from meal plan
        if (uiState.todaysPlannedMeals.isNotEmpty()) {
            item {
                PlannedMealsCard(
                    plannedMeals = uiState.todaysPlannedMeals,
                    onLogMeal = onLogPlannedMeal
                )
            }
        }
        
        // Meal sections
        MealType.entries.forEach { mealType ->
            val mealsForType = when (mealType) {
                MealType.BREAKFAST -> uiState.breakfastLogs
                MealType.LUNCH -> uiState.lunchLogs
                MealType.DINNER -> uiState.dinnerLogs
                MealType.SNACK -> uiState.snackLogs
            }
            
            item {
                MealSectionHeader(
                    mealType = mealType,
                    mealCount = mealsForType.size,
                    totalCalories = mealsForType.sumOf { it.calories },
                    isExpanded = mealType in expandedMealSections,
                    onToggleExpand = {
                        expandedMealSections = if (mealType in expandedMealSections) {
                            expandedMealSections - mealType
                        } else {
                            expandedMealSections + mealType
                        }
                    },
                    onAddFood = onSearchFood
                )
            }
            
            if (mealType in expandedMealSections) {
                if (mealsForType.isEmpty()) {
                    item {
                        Text(
                            text = "No ${mealType.displayName.lowercase()} logged yet",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                        )
                    }
                } else {
                    items(mealsForType, key = { it.id }) { entry ->
                        FoodLogEntryCard(
                            entry = entry,
                            onDelete = { onDeleteLog(entry.id) },
                            onEdit = { onEditLog(entry) },
                            onToggleFavorite = { onToggleFavorite(entry) }
                        )
                    }
                }
            }
        }
        
        // Bottom spacing
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun DailySummaryCard(summary: NutritionSummary) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Today's Nutrition",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NutritionStatItem("Calories", "${summary.totalCalories}", "kcal")
                NutritionStatItem("Protein", "${summary.totalProtein.toInt()}", "g")
                NutritionStatItem("Carbs", "${summary.totalCarbs.toInt()}", "g")
                NutritionStatItem("Fat", "${summary.totalFat.toInt()}", "g")
            }
            
            if (summary.mealCount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${summary.mealCount} item${if (summary.mealCount != 1) "s" else ""} logged",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun NutritionStatItem(label: String, value: String, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "$label ($unit)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun QuickActionsCard(
    onSearchFood: () -> Unit,
    onScanBarcode: () -> Unit,
    onAddManual: () -> Unit,
    onQuickAdd: () -> Unit,
    onShowRecent: () -> Unit,
    onCopyYesterday: () -> Unit,
    onBuildMeal: () -> Unit
) {
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
                text = "Log Food",
                style = MaterialTheme.typography.titleMedium
            )
            
            // Primary action
            OutlinedButton(
                onClick = onSearchFood,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Search Food Database")
            }
            
            // Secondary actions row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onScanBarcode,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("📷 Scan")
                }
                OutlinedButton(
                    onClick = onAddManual,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("✏️ Manual")
                }
            }
            
            // Build from ingredients button
            OutlinedButton(
                onClick = onBuildMeal,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🍳 Build from Ingredients")
            }
            
            // Quick access row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = onShowRecent,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Recent", style = MaterialTheme.typography.labelMedium)
                }
                TextButton(
                    onClick = onQuickAdd,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Outlined.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Quick Add", style = MaterialTheme.typography.labelMedium)
                }
                TextButton(
                    onClick = onCopyYesterday,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun PlannedMealsCard(
    plannedMeals: List<PlannedMealForLog>,
    onLogMeal: (PlannedMealForLog) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "📅 From Your Meal Plan",
                style = MaterialTheme.typography.titleMedium
            )
            
            Text(
                text = "Tap to log a planned meal",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            
            plannedMeals.forEach { meal ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onLogMeal(meal) },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = meal.recipeName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = meal.mealType.displayName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = if (meal.hasNutritionData) "~${meal.estimatedCalories} cal" else "~0 cal",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
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

@Composable
private fun MealSectionHeader(
    mealType: MealType,
    mealCount: Int,
    totalCalories: Int,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onAddFood: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = getMealTypeColor(mealType).copy(alpha = 0.7f)
        ),
        modifier = Modifier.clickable(onClick = onToggleExpand)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = getMealEmoji(mealType),
                    style = MaterialTheme.typography.titleMedium
                )
                Column {
                    Text(
                        text = mealType.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (mealCount > 0) {
                        Text(
                            text = "$mealCount items • $totalCalories cal",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            
            Row {
                IconButton(onClick = onAddFood) {
                    Icon(Icons.Default.Add, contentDescription = "Add food")
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand"
                )
            }
        }
    }
}

private fun getMealEmoji(mealType: MealType) = when (mealType) {
    MealType.BREAKFAST -> "🌅"
    MealType.LUNCH -> "☀️"
    MealType.DINNER -> "🌙"
    MealType.SNACK -> "🍎"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickAddCaloriesDialog(
    onDismiss: () -> Unit,
    onConfirm: (calories: Int, name: String, mealType: MealType) -> Unit
) {
    var calories by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("Quick Add") }
    var selectedMealType by remember { mutableStateOf(MealType.SNACK) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quick Add Calories") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = calories,
                    onValueChange = { calories = it.filter { c -> c.isDigit() } },
                    label = { Text("Calories") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Text("Log to:", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MealType.entries.forEach { type ->
                        FilterChip(
                            selected = type == selectedMealType,
                            onClick = { selectedMealType = type },
                            label = { Text(type.displayName) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { 
                    calories.toIntOrNull()?.let { cal ->
                        onConfirm(cal, name.ifBlank { "Quick Add" }, selectedMealType)
                    }
                },
                enabled = calories.isNotBlank() && calories.toIntOrNull() != null
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditFoodLogDialog(
    entry: FoodLogEntry,
    onDismiss: () -> Unit,
    onUpdateServings: (Float) -> Unit,
    onUpdateMealType: (MealType) -> Unit,
    onToggleFavorite: () -> Unit
) {
    var servings by remember { mutableFloatStateOf(entry.servings) }
    var selectedMealType by remember { mutableStateOf(entry.mealType) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Entry") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = entry.meal,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                HorizontalDivider()
                
                // Servings
                Text("Servings", style = MaterialTheme.typography.labelLarge)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = { if (servings > 0.5f) servings -= 0.5f }) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease")
                    }
                    Text(
                        text = "%.1f".format(servings),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.width(60.dp),
                        textAlign = TextAlign.Center
                    )
                    IconButton(onClick = { servings += 0.5f }) {
                        Icon(Icons.Default.Add, contentDescription = "Increase")
                    }
                }
                
                // Calculated calories for new serving
                val ratio = servings / entry.servings
                Text(
                    text = "Calories: ${(entry.calories * ratio).toInt()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                HorizontalDivider()
                
                // Meal type
                Text("Meal Type", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MealType.entries.forEach { type ->
                        FilterChip(
                            selected = type == selectedMealType,
                            onClick = { selectedMealType = type },
                            label = { Text(type.displayName) }
                        )
                    }
                }
                
                HorizontalDivider()
                
                // Favorite toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onToggleFavorite),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (entry.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = if (entry.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (entry.isFavorite) "Remove from Favorites" else "Add to Favorites"
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { 
                if (servings != entry.servings) onUpdateServings(servings)
                if (selectedMealType != entry.mealType) onUpdateMealType(selectedMealType)
                onDismiss()
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecentFoodsDialog(
    recentFoods: List<FoodLogEntry>,
    favorites: List<FoodLogEntry>,
    onDismiss: () -> Unit,
    onSelectFood: (FoodLogEntry, MealType) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedMealType by remember { mutableStateOf(MealType.SNACK) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quick Re-log") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Tab selection
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        label = { Text("Recent") }
                    )
                    FilterChip(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        label = { Text("Favorites") }
                    )
                }
                
                // Meal type selection
                Text("Log to:", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    MealType.entries.forEach { type ->
                        FilterChip(
                            selected = type == selectedMealType,
                            onClick = { selectedMealType = type },
                            label = { Text(type.displayName) }
                        )
                    }
                }
                
                HorizontalDivider()
                
                // Food list
                val displayList = if (selectedTab == 0) recentFoods else favorites
                
                if (displayList.isEmpty()) {
                    Text(
                        text = if (selectedTab == 0) "No recent foods" else "No favorites yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.height(300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(displayList.take(20)) { food ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectFood(food, selectedMealType) },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = food.meal,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${food.calories} cal",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Add",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
