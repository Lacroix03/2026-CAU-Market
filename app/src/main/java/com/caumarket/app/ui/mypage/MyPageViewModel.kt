package com.caumarket.app.ui.mypage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.caumarket.app.core.model.Product
import com.caumarket.app.core.model.User
import com.caumarket.app.data.repository.AuthRepository
import com.caumarket.app.data.repository.ProductRepository
import com.caumarket.app.ui.common.MarketErrorMapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MyPageUiState(
    val user: User? = null,
    val selectedStatus: String? = null,
    val allMyProducts: List<Product> = emptyList(),
    val filteredProducts: List<Product> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
) {
    val totalCount: Int get() = allMyProducts.size
    val sellingCount: Int get() = allMyProducts.count { it.status == "SELLING" }
    val reservedCount: Int get() = allMyProducts.count { it.status == "RESERVED" }
    val soldCount: Int get() = allMyProducts.count { it.status == "SOLD" }
}

class MyPageViewModel(
    private val authRepository: AuthRepository,
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyPageUiState())
    val uiState: StateFlow<MyPageUiState> = _uiState.asStateFlow()

    init {
        loadMyProducts()
    }

    fun loadMyProducts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val user = authRepository.getCurrentUser().getOrNull()
            val selected = _uiState.value.selectedStatus
            val allRes = productRepository.getMyProducts(status = null)
            val filteredRes = if (selected == null) allRes else productRepository.getMyProducts(status = selected)

            allRes.fold(
                onSuccess = { all ->
                    val filtered = filteredRes.getOrElse { all }
                    _uiState.update {
                        it.copy(
                            user = user,
                            allMyProducts = all,
                            filteredProducts = filtered,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            user = user,
                            isLoading = false,
                            errorMessage = MarketErrorMapper.toMessage(e, "불러오지 못했어요")
                        )
                    }
                }
            )
        }
    }

    fun selectStatusTab(status: String?) {
        _uiState.update { it.copy(selectedStatus = status) }
        loadMyProducts()
    }

    companion object {
        fun factory(
            authRepository: AuthRepository,
            productRepository: ProductRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                MyPageViewModel(authRepository, productRepository) as T
        }
    }
}
