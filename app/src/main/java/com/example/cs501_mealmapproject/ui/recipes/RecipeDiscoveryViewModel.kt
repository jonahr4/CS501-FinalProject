package com.example.cs501_mealmapproject.ui.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val DEFAULT_QUERY = "chicken"
private const val MAX_RESULTS = 5

class RecipeDiscoveryViewModel(
    private val repository: RecipeDiscoveryRepository = RecipeDiscoveryRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecipeDiscoveryUiState())
    val uiState: StateFlow<RecipeDiscoveryUiState> = _uiState.asStateFlow()

    init {
        performSearch()
    }

    fun onQueryChange(value: String) {
        _uiState.update { it.copy(query = value) }
    }

    fun performSearch() {
        val sanitizedQuery = _uiState.value.query.trim().ifEmpty { DEFAULT_QUERY }
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
                        errorMessage = null
                    )
                }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Unable to load recipes"
                    )
                }
            }
        }
    }
}

data class RecipeDiscoveryUiState(
    val recipes: List<RecipeSummary> = emptyList(),
    val query: String = DEFAULT_QUERY,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

data class RecipeSummary(
    val title: String,
    val subtitle: String,
    val description: String,
    val tags: List<String>,
    val imageUrl: String?,
    val instructions: String
)
