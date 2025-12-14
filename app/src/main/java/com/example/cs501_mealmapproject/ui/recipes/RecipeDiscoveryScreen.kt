package com.example.cs501_mealmapproject.ui.recipes

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.cs501_mealmapproject.ui.mealplan.DailyMealPlan
import com.example.cs501_mealmapproject.ui.mealplan.MealPlanDayFormatter
import com.example.cs501_mealmapproject.ui.mealplan.MealPlanViewModel
import com.example.cs501_mealmapproject.ui.theme.CS501MealMapProjectTheme
import java.time.LocalDate

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDiscoveryScreen(
    mealPlanViewModel: MealPlanViewModel,
    modifier: Modifier = Modifier,
    viewModel: RecipeDiscoveryViewModel = viewModel(),
    onRecipeAdded: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val planState by mealPlanViewModel.uiState.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    var selectedRecipe by remember { mutableStateOf<RecipeSummary?>(null) }
    var plannerTarget by remember { mutableStateOf<RecipeSummary?>(null) }
    var showPlannerSuccess by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.errorMessage != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = uiState.errorMessage ?: "Unable to load recipes",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Button(onClick = { viewModel.performSearch() }) {
                            Text("Try again")
                        }
                    }
                }
            }

            else -> {
                RecipeDiscoveryContent(
                    modifier = Modifier,
                    recipes = uiState.recipes,
                    query = uiState.query,
                    hasSearched = uiState.hasSearched,
                    onQueryChange = viewModel::onQueryChange,
                    onSearch = viewModel::performSearch,
                    onRecipeClick = { recipe -> selectedRecipe = recipe }
                )
            }
        }

        // Snackbar host positioned at bottom of screen
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    selectedRecipe?.let { recipe ->
        RecipeDetailDialog(
            recipe = recipe,
            onAddToPlanner = {
                // Check if we have a pre-selected slot
                val preDate = planState.preSelectedDate
                val preMealType = planState.preSelectedMealType
                if (preDate != null && preMealType != null) {
                    // Auto-assign to the pre-selected slot
                    mealPlanViewModel.assignMeal(
                        preDate,
                        preMealType,
                        recipe.title
                    )
                    mealPlanViewModel.clearPreSelectedSlot()
                    selectedRecipe = null
                    snackbarScope.launch {
                        snackbarHostState.showSnackbar("Added '${recipe.title}' to planner")
                        kotlinx.coroutines.delay(1500)
                        onRecipeAdded?.invoke()
                    }
                } else {
                    // Show the planner selection dialog
                    plannerTarget = recipe
                }
            },
            onDismiss = { selectedRecipe = null }
        )
    }

    plannerTarget?.let { recipe ->
        PlannerSelectionDialog(
            recipe = recipe,
            plan = planState.plan,
            onConfirm = { selections ->
                selections.forEach { (day, mealType) ->
                    mealPlanViewModel.assignMeal(day.date, mealType, recipe.title)
                }
                plannerTarget = null
                selectedRecipe = null
                showPlannerSuccess = true
                // show snackbar then navigate after a brief delay
                snackbarScope.launch {
                    snackbarHostState.showSnackbar("Added '${recipe.title}' to planner")
                    kotlinx.coroutines.delay(1500)
                    onRecipeAdded?.invoke()
                }
            },
            onDismiss = { plannerTarget = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecipeDiscoveryContent(
    modifier: Modifier = Modifier,
    recipes: List<RecipeSummary>,
    query: String,
    hasSearched: Boolean,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onRecipeClick: (RecipeSummary) -> Unit
) {

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with icon
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.MenuBook,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column {
                Text(
                    text = "Discover recipes",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "Search TheMealDB catalog",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Search bar with button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Search recipes...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = onSearch,
                enabled = query.isNotBlank()
            ) {
                Text("Search")
            }
        }
        
        // Results or empty state
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!hasSearched) {
                // Initial state - show tips
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restaurant,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "Search for a recipe",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Try searching for dishes like \"pasta\", \"salad\", or \"beef\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else if (recipes.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "No recipes found",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Try a different search term",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            items(recipes) { recipe ->
                RecipeCard(recipe = recipe, onClick = onRecipeClick)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecipeCard(
    recipe: RecipeSummary,
    onClick: (RecipeSummary) -> Unit
) {
    Card(
        onClick = { onClick(recipe) },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RecipeThumbnail(
                imageUrl = recipe.imageUrl,
                contentDescription = recipe.title,
                sizeDp = 80.dp
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = recipe.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = recipe.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Tap for details & to add to planner",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RecipeThumbnail(
    imageUrl: String?,
    contentDescription: String,
    sizeDp: Dp = 48.dp
) {
    val modifier = Modifier
        .size(sizeDp)
        .clip(RoundedCornerShape(12.dp))

    if (imageUrl.isNullOrBlank()) {
        val initial = contentDescription.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    } else {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun RecipeDetailDialog(
    recipe: RecipeSummary,
    onAddToPlanner: () -> Unit,
    onDismiss: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = recipe.title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                recipe.imageUrl?.let { url ->
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(url)
                            .crossfade(true)
                            .build(),
                        contentDescription = recipe.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(4f / 3f)
                            .clip(MaterialTheme.shapes.medium)
                    )
                }
                Text(text = recipe.subtitle, style = MaterialTheme.typography.bodySmall)
                if (recipe.tags.any { it != "Source: TheMealDB" }) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        recipe.tags.filter { it != "Source: TheMealDB" }.forEach { tag ->
                            AssistChip(onClick = {}, label = { Text(tag) })
                        }
                    }
                }
                if (recipe.ingredients.isNotEmpty()) {
                    Text(
                        text = "Ingredients",
                        style = MaterialTheme.typography.titleMedium
                    )
                    recipe.ingredients.forEach { ingredient ->
                        Text(
                            text = "• $ingredient",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Divider()
                Text(
                    text = "Recipe",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = recipe.instructions,
                    style = MaterialTheme.typography.bodyMedium
                )
                recipe.sourceUrl?.let { url ->
                    TextButton(onClick = { uriHandler.openUri(url) }) {
                        Text("Click here to view on TheMealDB")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onAddToPlanner() }) {
                Text("Add to planner")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun PlannerSelectionDialog(
    recipe: RecipeSummary,
    plan: List<DailyMealPlan>,
    onConfirm: (List<Pair<DailyMealPlan, String>>) -> Unit,
    onDismiss: () -> Unit
) {
    val expandedDays = remember(plan) {
        mutableStateMapOf<LocalDate, Boolean>().apply {
            plan.forEach { put(it.date, false) }
        }
    }
    val selections = remember(plan) {
        mutableStateMapOf<Pair<LocalDate, String>, Boolean>().apply {
            plan.forEach { day ->
                day.meals.forEach { meal ->
                    put(day.date to meal.mealType, false)
                }
            }
        }
    }
    var showValidationError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Add ${recipe.title} to planner") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                plan.forEach { day ->
                    val expanded = expandedDays[day.date] ?: false
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = day.date.format(MealPlanDayFormatter),
                                    style = MaterialTheme.typography.titleSmall
                                )
                                IconButton(onClick = { expandedDays[day.date] = !expanded }) {
                                    Icon(
                                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                        contentDescription = if (expanded) "Collapse" else "Expand"
                                    )
                                }
                            }
                            if (expanded) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    day.meals.forEach { meal ->
                                        val key = day.date to meal.mealType
                                        val checked = selections[key] ?: false
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Checkbox(
                                                checked = checked,
                                                onCheckedChange = { selections[key] = it }
                                            )
                                            Column {
                                                Text(meal.mealType, style = MaterialTheme.typography.bodyLarge)
                                                Text(
                                                    text = meal.recipeName,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (showValidationError) {
                    Text(
                        text = "Select at least one meal slot",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val selectedSlots = selections
                    .filterValues { it }
                    .mapNotNull { (key, _) ->
                        val (date, mealType) = key
                        val day = plan.firstOrNull { it.date == date } ?: return@mapNotNull null
                        day to mealType
                    }
                if (selectedSlots.isEmpty()) {
                    showValidationError = true
                } else {
                    
                    onConfirm(selectedSlots)
                    showValidationError = false
                }
            }) {
                Text("Done")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun RecipeDiscoveryScreenPreview() {
    CS501MealMapProjectTheme {
        RecipeDiscoveryContent(
            recipes = previewRecipes,
            query = "salmon",
            hasSearched = true,
            onQueryChange = {},
            onSearch = {},
            onRecipeClick = {}
        )
    }
}

private val previewRecipes = listOf(
    RecipeSummary(
        title = "Mediterranean Chickpea Salad",
        subtitle = "Mediterranean • Vegetarian",
        description = "Chickpeas, cucumber, and herbs tossed in lemon dressing.",
        tags = listOf("High protein", "Budget"),
        imageUrl = "https://www.themealdb.com/images/media/meals/llcbn01574260722.jpg",
        instructions = "Mix everything and serve chilled.",
        ingredients = listOf("1 cup Chickpeas", "1 tbsp Olive Oil"),
        sourceUrl = "https://www.themealdb.com/meal/12345"
    ),
    RecipeSummary(
        title = "Sheet-Pan Teriyaki Salmon",
        subtitle = "Japanese • Seafood",
        description = "Sweet and savory glaze baked with seasonal veggies.",
        tags = listOf("Omega-3", "Meal Prep"),
        imageUrl = "https://www.themealdb.com/images/media/meals/xyz.jpg",
        instructions = "Bake salmon with sauce and veggies.",
        ingredients = listOf("2 Salmon Fillets", "1/4 cup Teriyaki Sauce"),
        sourceUrl = "https://www.themealdb.com/meal/67890"
    )
)
