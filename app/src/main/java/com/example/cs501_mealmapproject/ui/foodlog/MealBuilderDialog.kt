package com.example.cs501_mealmapproject.ui.foodlog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.example.cs501_mealmapproject.data.nutrition.FoodItem

@Composable
fun MealBuilderDialog(
    mealBuilder: MealBuilder?,
    searchResults: List<FoodItem>,
    isSearching: Boolean,
    searchError: String?,
    onDismiss: () -> Unit,
    onSearch: (String) -> Unit,
    onAddIngredient: (FoodItem, Float) -> Unit,
    onRemoveIngredient: (Int) -> Unit,
    onUpdateServings: (Int, Float) -> Unit,
    onUpdateMealName: (String) -> Unit,
    onUpdateMealType: (MealType) -> Unit,
    onSave: () -> Unit
) {
    val builder = mealBuilder ?: return

    var searchQuery by remember { mutableStateOf("") }
    var selectedFood by remember { mutableStateOf<FoodItem?>(null) }
    val focusManager = LocalFocusManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Create Custom Meal")
                Text(
                    text = "Add multiple ingredients to build your meal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Meal Name
                item {
                    OutlinedTextField(
                        value = builder.mealName,
                        onValueChange = onUpdateMealName,
                        label = { Text("Meal Name") },
                        placeholder = { Text("e.g., Egg Wrap, Protein Bowl") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                // Meal Type Selector
                item {
                    Text("Meal Type", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MealType.entries.forEach { type ->
                            FilterChip(
                                selected = builder.mealType == type,
                                onClick = { onUpdateMealType(type) },
                                label = { Text(type.displayName) }
                            )
                        }
                    }
                }

                // Current Ingredients
                if (builder.ingredients.isNotEmpty()) {
                    item {
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Ingredients (${builder.ingredients.size})",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    itemsIndexed(builder.ingredients) { index, ingredient ->
                        IngredientCard(
                            ingredient = ingredient,
                            onRemove = { onRemoveIngredient(index) },
                            onUpdateServings = { newServings ->
                                onUpdateServings(index, newServings)
                            }
                        )
                    }

                    // Nutrition Summary
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    "Total Nutrition",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    NutrientText("Calories", "${builder.totalCalories}")
                                    NutrientText("Protein", "${builder.totalProtein.toInt()}g")
                                    NutrientText("Carbs", "${builder.totalCarbs.toInt()}g")
                                    NutrientText("Fat", "${builder.totalFat.toInt()}g")
                                }
                            }
                        }
                    }
                }

                // Search for ingredients
                item {
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Add Ingredient", style = MaterialTheme.typography.titleMedium)
                }

                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            onSearch(it)
                        },
                        label = { Text("Search foods") },
                        placeholder = { Text("e.g., egg, tortilla, chicken") },
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
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = { focusManager.clearFocus() }
                        )
                    )
                }

                // Search Error
                if (searchError != null) {
                    item {
                        Text(
                            text = searchError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                // Search Results
                if (isSearching) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }
                } else if (searchResults.isNotEmpty() && searchQuery.isNotEmpty()) {
                    items(searchResults.size) { index ->
                        val food = searchResults[index]
                        FoodSearchResultCard(
                            food = food,
                            onClick = { selectedFood = food }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSave,
                enabled = builder.isValid
            ) {
                Text("Save Meal")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    // Dialog to add servings for selected food
    selectedFood?.let { food ->
        AddIngredientDialog(
            food = food,
            onDismiss = { selectedFood = null },
            onAdd = { servings ->
                onAddIngredient(food, servings)
                selectedFood = null
                searchQuery = ""
                onSearch("")
            }
        )
    }
}

@Composable
private fun IngredientCard(
    ingredient: MealIngredient,
    onRemove: () -> Unit,
    onUpdateServings: (Float) -> Unit
) {
    var servings by remember(ingredient.servings) { mutableFloatStateOf(ingredient.servings) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ingredient.foodItem.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${ingredient.foodItem.servingSize ?: "serving"} • ${ingredient.totalCalories} cal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = {
                        if (servings > 0.5f) {
                            servings -= 0.5f
                            onUpdateServings(servings)
                        }
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
                }

                Text(
                    text = "${servings}x",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(40.dp)
                )

                IconButton(
                    onClick = {
                        servings += 0.5f
                        onUpdateServings(servings)
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(16.dp))
                }

                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Remove",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun FoodSearchResultCard(
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = food.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                food.brand?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                Text(
                    text = food.servingSize ?: "per serving",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${food.calories}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "cal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AddIngredientDialog(
    food: FoodItem,
    onDismiss: () -> Unit,
    onAdd: (Float) -> Unit
) {
    var servings by remember { mutableFloatStateOf(1f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add ${food.name}") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("How many servings?")
                Text(
                    text = "Per serving (${food.servingSize ?: "1 serving"}): ${food.calories} cal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = { if (servings > 0.5f) servings -= 0.5f }
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease")
                    }

                    Text(
                        text = "$servings",
                        style = MaterialTheme.typography.headlineSmall
                    )

                    IconButton(
                        onClick = { servings += 0.5f }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase")
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Total:", fontWeight = FontWeight.Bold)
                        Text("${(food.calories * servings).toInt()} calories")
                        Text("Protein: ${(food.protein * servings).toInt()}g")
                        Text("Carbs: ${(food.carbs * servings).toInt()}g")
                        Text("Fat: ${(food.fat * servings).toInt()}g")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onAdd(servings) }) {
                Text("Add")
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
private fun NutrientText(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
