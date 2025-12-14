package com.example.cs501_mealmapproject.ui.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val MAX_RESULTS = 5

class RecipeDiscoveryViewModel(
    private val repository: RecipeDiscoveryRepository = RecipeDiscoveryRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecipeDiscoveryUiState())
    val uiState: StateFlow<RecipeDiscoveryUiState> = _uiState.asStateFlow()

    fun onQueryChange(value: String) {
        _uiState.update { it.copy(query = value) }
    }

    fun performSearch() {
        val sanitizedQuery = _uiState.value.query.trim()
        if (sanitizedQuery.isEmpty()) return
        fetchRecipes(sanitizedQuery)
    }

    // Searches for recipes using API
    private fun fetchRecipes(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, query = query) }
            try {
                val recipes = repository.searchRecipes(query).take(MAX_RESULTS)
                _uiState.update {
                    it.copy(
                        recipes = recipes,
                        isLoading = false,
                        hasSearched = true,
                        errorMessage = null
                    )
                }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        hasSearched = true,
                        errorMessage = error.message ?: "Unable to load recipes"
                    )
                }
            }
        }
    }
}

data class RecipeDiscoveryUiState(
    val recipes: List<RecipeSummary> = emptyList(),
    val query: String = "",
    val isLoading: Boolean = false,
    val hasSearched: Boolean = false,
    val errorMessage: String? = null
)

data class RecipeSummary(
    val title: String,
    val subtitle: String,
    val description: String,
    val tags: List<String>,
    val imageUrl: String?,
    val instructions: String,
    val ingredients: List<String>,
    val sourceUrl: String?
)
