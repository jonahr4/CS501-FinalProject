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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
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
            text = "Discover recipes without the scroll fatigue",
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RecipeThumbnail(imageUrl = recipe.imageUrl, contentDescription = recipe.title)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                }
            }
            Text(
                text = recipe.description,
                style = MaterialTheme.typography.bodyMedium
            )
            if (recipe.tags.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    recipe.tags.forEach { tag ->
                        AssistChip(
                            onClick = {},
                            label = { Text(tag) },
                            colors = AssistChipDefaults.assistChipColors()
                        )
                    }
                }
            }
            Text(
                text = "Tap to open details, add to planner, or export ingredients to the shopping list.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun RecipeThumbnail(
    imageUrl: String?,
    contentDescription: String
) {
    val modifier = Modifier
        .size(48.dp)
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
                if (recipe.tags.isNotEmpty()) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        recipe.tags.forEach { tag ->
                            AssistChip(onClick = {}, label = { Text(tag) })
                        }
                    }
                }
                Text(
                    text = recipe.instructions,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

