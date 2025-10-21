package com.example.cs501_mealmapproject.ui.shopping

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.cs501_mealmapproject.ui.theme.CS501MealMapProjectTheme

private val sampleSections = listOf(
    ShoppingSection(
        title = "Produce",
        items = listOf(
            ShoppingItem("Spinach", true),
            ShoppingItem("Cherry tomatoes", false)
        )
    ),
    ShoppingSection(
        title = "Pantry",
        items = listOf(
            ShoppingItem("Whole grain wraps", false),
            ShoppingItem("Chickpeas", true)
        )
    )
)

@Composable
fun ShoppingListScreen(
    modifier: Modifier = Modifier,
    sections: List<ShoppingSection> = sampleSections
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
            text = "Lists sync with the meal planner and stay available offline using Room.",
            style = MaterialTheme.typography.bodyMedium
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(sections) { section ->
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
                            text = section.title,
                            style = MaterialTheme.typography.titleMedium
                        )
                        section.items.forEach { item ->
                            val checkedState = remember(item.name) { mutableStateOf(item.checked) }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Checkbox(
                                    checked = checkedState.value,
                                    onCheckedChange = { checkedState.value = it }
                                )
                            }
                        }
                        Text(
                            text = "Swipe right in-app to move purchased items into pantry staples.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

data class ShoppingSection(
    val title: String,
    val items: List<ShoppingItem>
)

data class ShoppingItem(
    val name: String,
    val checked: Boolean
)

@Preview(showBackground = true)
@Composable
private fun ShoppingListScreenPreview() {
    CS501MealMapProjectTheme {
        ShoppingListScreen()
    }
}
