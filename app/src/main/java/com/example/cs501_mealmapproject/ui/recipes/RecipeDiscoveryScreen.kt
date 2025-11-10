package com.example.cs501_mealmapproject.ui.recipes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDiscoveryScreen(
    modifier: Modifier = Modifier,
    viewModel: RecipeDiscoveryViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    RecipeDiscoveryContent(
        modifier = modifier,
        recipes = uiState.recipes
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecipeDiscoveryContent(
    modifier: Modifier = Modifier,
    recipes: List<RecipeSummary>
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
            text = "Filters and pantry-aware suggestions pull from TheMealDB to keep ideas fresh.",
            style = MaterialTheme.typography.bodyMedium
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(recipes) { recipe ->
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
                            text = recipe.title,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = recipe.prepTime,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "${recipe.calories} kcal",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
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
                        Text(
                            text = "Tap to open details, add to planner, or export ingredients to the shopping list.",
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
private fun RecipeDiscoveryScreenPreview() {
    CS501MealMapProjectTheme {
        RecipeDiscoveryContent(recipes = RecipeDiscoveryUiState().recipes)
    }
}
