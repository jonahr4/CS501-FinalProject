package com.example.cs501_mealmapproject.ui.shopping

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ShoppingListViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ShoppingListUiState())
    val uiState: StateFlow<ShoppingListUiState> = _uiState.asStateFlow()
}

data class ShoppingListUiState(
    val sections: List<ShoppingSection> = demoSections
)

data class ShoppingSection(
    val title: String,
    val items: List<ShoppingItem>
)

data class ShoppingItem(
    val name: String,
    val checked: Boolean
)

private val demoSections = listOf(
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
