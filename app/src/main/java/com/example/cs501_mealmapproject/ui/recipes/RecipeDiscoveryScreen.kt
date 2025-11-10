package com.example.cs501_mealmapproject.ui.recipes

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.cs501_mealmapproject.ui.theme.CS501MealMapProjectTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDiscoveryScreen(
    modifier: Modifier = Modifier,
    viewModel: RecipeDiscoveryViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedRecipe by remember { mutableStateOf<RecipeSummary?>(null) }

    when {
        uiState.isLoading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        uiState.errorMessage != null -> {
            Box(
                modifier = modifier.fillMaxSize(),
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
                modifier = modifier,
                recipes = uiState.recipes,
                query = uiState.query,
                onQueryChange = viewModel::onQueryChange,
                onSearch = viewModel::performSearch,
                onRecipeClick = { recipe -> selectedRecipe = recipe }
            )
        }
    }

    selectedRecipe?.let { recipe ->
        RecipeDetailDialog(
            recipe = recipe,
            onDismiss = { selectedRecipe = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecipeDiscoveryContent(
    modifier: Modifier = Modifier,
    recipes: List<RecipeSummary>,
    query: String,
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
        Text(
            text = "Discover recipes",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "Search TheMealDB catalog and tap a card to see full directions.",
            style = MaterialTheme.typography.bodyMedium
        )
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            label = { Text("Search by recipe name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = onSearch) {
            Text("Search")
        }
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (recipes.isEmpty()) {
                item {
                    Text(
                        text = "No meals found. Try another search.",
                        style = MaterialTheme.typography.bodyMedium
                    )
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
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RecipeThumbnail(
                imageUrl = recipe.imageUrl,
                contentDescription = recipe.title,
                sizeDp = 72.dp
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = recipe.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = recipe.subtitle,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Tap to open details, add to planner, or export ingredients to the shopping list.",
                    style = MaterialTheme.typography.bodySmall
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
        .clip(CircleShape)

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
            TextButton(onClick = onDismiss) {
                Text("Close")
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
