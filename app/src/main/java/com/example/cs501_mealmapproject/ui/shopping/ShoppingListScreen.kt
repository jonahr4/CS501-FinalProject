package com.example.cs501_mealmapproject.ui.shopping

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import android.util.Log
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cs501_mealmapproject.ui.theme.CS501MealMapProjectTheme

@Composable
fun ShoppingListScreen(
    modifier: Modifier = Modifier,
    viewModel: ShoppingListViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.generateShoppingListFromMealPlan()
    }
    
    ShoppingListContent(
        modifier = modifier,
        sections = uiState.sections,
        onToggle = { sectionIndex, itemIndex, checked ->
            viewModel.toggleItem(sectionIndex, itemIndex, checked)
        }
    )
}

// Composable function for Shopping List Screen. Currently using placeholder info
// TODO: Implement Shopping functionality
@Composable
private fun ShoppingListContent(
    modifier: Modifier = Modifier,
    sections: List<ShoppingSection>,
    onToggle: (sectionIndex: Int, itemIndex: Int, checked: Boolean) -> Unit = { _, _, _ -> }
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Shop smarter with auto-generated lists",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "Track what you have shopped for ",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "Lists sync with the meal planner",
            style = MaterialTheme.typography.bodyMedium
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(sections) { sectionIndex, section ->
                val expanded = remember { mutableStateOf(false) }
                
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
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = section.title,
                                style = MaterialTheme.typography.titleMedium
                            )
                            IconButton(
                                onClick = { expanded.value = !expanded.value }
                            ) {
                                Icon(
                                    imageVector = if (expanded.value) 
                                        Icons.Default.KeyboardArrowUp 
                                    else 
                                        Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (expanded.value) "Collapse" else "Expand"
                                )
                            }
                        }
                        
                        if (expanded.value) {
                            section.items.forEachIndexed { itemIndex, item ->
                                key("section-${sectionIndex}-item-${item.name}") {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = item.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                                        )
                                        Checkbox(
                                            checked = item.checked,
                                            onCheckedChange = { checked ->
                                                onToggle(sectionIndex, itemIndex, checked)
                                            }
                                        )
                                    }
                                }
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
private fun ShoppingListScreenPreview() {
    CS501MealMapProjectTheme {
        ShoppingListContent(sections = ShoppingListUiState().sections)
    }
}
