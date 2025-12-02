package com.example.cs501_mealmapproject.ui.shopping

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
    
    LaunchedEffect(Unit) {
        viewModel.generateShoppingListFromMealPlan()
    }
    
    ShoppingListContent(
        modifier = modifier,
        sections = uiState.sections,
        onToggle = { sectionIndex, itemIndex, checked ->
            viewModel.toggleItem(sectionIndex, itemIndex, checked)
        },
        onClearChecked = { viewModel.clearCheckedItems() },
        onAddManualItem = { name -> viewModel.addManualItem(name) }
    )
}

@Composable
private fun ShoppingListContent(
    modifier: Modifier = Modifier,
    sections: List<ShoppingSection>,
    onToggle: (sectionIndex: Int, itemIndex: Int, checked: Boolean) -> Unit = { _, _, _ -> },
    onClearChecked: () -> Unit = {},
    onAddManualItem: (String) -> Unit = {}
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var newItemName by remember { mutableStateOf("") }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Shopping List",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Lists sync with meal planner",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (sections.any { section -> section.items.any { it.checked } }) {
                    TextButton(onClick = onClearChecked) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text("Clear Done")
                    }
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (sections.isEmpty()) {
                    item {
                        Text(
                            text = "Your shopping list is empty. Add meals to your plan or add items manually.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 32.dp)
                        )
                    }
                }
                
                itemsIndexed(sections) { sectionIndex, section ->
                    val expanded = remember { mutableStateOf(true) } // Default to expanded
                    
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
                                        if (item.isHeader) {
                                            // Render as a Header Label (No Checkbox)
                                            Text(
                                                text = item.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 8.dp, bottom = 4.dp)
                                            )
                                        } else {
                                            // Render as a Checkable Item
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = item.name,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        textDecoration = if (item.checked) 
                                                            androidx.compose.ui.text.style.TextDecoration.LineThrough 
                                                        else 
                                                            androidx.compose.ui.text.style.TextDecoration.None,
                                                        color = if (item.checked) 
                                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) 
                                                        else 
                                                            MaterialTheme.colorScheme.onSurface
                                                    ),
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
                // Spacer for FAB
                item { 
                     Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
        
        // Floating Action Button
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Item")
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Item") },
            text = {
                OutlinedTextField(
                    value = newItemName,
                    onValueChange = { newItemName = it },
                    label = { Text("Item name (e.g. Milk)") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newItemName.isNotBlank()) {
                            onAddManualItem(newItemName)
                            newItemName = ""
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ShoppingListScreenPreview() {
    CS501MealMapProjectTheme {
        ShoppingListContent(
            sections = listOf(
                ShoppingSection("Produce", listOf(
                    ShoppingItem("Apples", false),
                    ShoppingItem("Potatoes: (buy one bag)", false, isHeader = true),
                    ShoppingItem("  • 500g for Mash", false)
                ))
            )
        )
    }
}
