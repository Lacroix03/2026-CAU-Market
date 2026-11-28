package com.caumarket.app.ui.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.caumarket.app.core.model.Product
import com.caumarket.app.data.repository.ProductRepository
import com.caumarket.app.ui.common.MarketErrorMapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProductListUiState(
    val query: String = "",
    val selectedCategory: String? = null,
    val excludeSoldInSearch: Boolean = false,
    val products: List<Product> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
) {
    val isSearching: Boolean get() = query.trim().isNotEmpty()
}

class ProductListViewModel(
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductListUiState())
    val uiState: StateFlow<ProductListUiState> = _uiState.asStateFlow()

    init {
        loadProducts()
    }

    fun onQueryChange(newQuery: String) {
        _uiState.update {
            it.copy(
                query = newQuery,
                excludeSoldInSearch = if (newQuery.isBlank()) false else it.excludeSoldInSearch
            )
        }
        loadProducts()
    }

    fun onCategorySelected(categoryCode: String?) {
        _uiState.update { it.copy(selectedCategory = categoryCode) }
        loadProducts()
    }

    fun toggleExcludeSoldInSearch() {
        _uiState.update { it.copy(excludeSoldInSearch = !it.excludeSoldInSearch) }
        loadProducts()
    }

    fun loadProducts() {
        val state = _uiState.value
        val searching = state.query.trim().isNotEmpty()
        val includeSold = if (searching) !state.excludeSoldInSearch else false

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            productRepository.getProducts(
                query = state.query.trim().takeIf { it.isNotEmpty() },
                category = state.selectedCategory,
                includeSold = includeSold
            ).fold(
                onSuccess = { list ->
                    _uiState.update { it.copy(products = list, isLoading = false, errorMessage = null) }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = MarketErrorMapper.toMessage(e, "불러오지 못했어요"))
                    }
                }
            )
        }
    }

    companion object {
        fun factory(productRepository: ProductRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ProductListViewModel(productRepository) as T
            }
    }
}
