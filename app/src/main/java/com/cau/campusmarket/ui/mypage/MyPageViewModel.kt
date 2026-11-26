package com.cau.campusmarket.ui.mypage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cau.campusmarket.data.repository.AuthRepository
import com.cau.campusmarket.data.repository.ProductRepository
import com.cau.campusmarket.domain.model.Product
import com.cau.campusmarket.domain.model.TradeStatus
import com.cau.campusmarket.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MyPageUiState(
    val user: User? = null,
    val selectedStatus: TradeStatus? = null,
    val allMyProducts: List<Product> = emptyList(),
    val filteredProducts: List<Product> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
) {
    val totalCount: Int get() = allMyProducts.size
    val sellingCount: Int get() = allMyProducts.count { it.status == TradeStatus.SELLING }
    val reservedCount: Int get() = allMyProducts.count { it.status == TradeStatus.RESERVED }
    val soldCount: Int get() = allMyProducts.count { it.status == TradeStatus.SOLD }
}

class MyPageViewModel(
    private val authRepository: AuthRepository,
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        MyPageUiState(user = authRepository.currentUser.value)
    )
    val uiState: StateFlow<MyPageUiState> = _uiState.asStateFlow()

    init {
        loadMyProducts()
    }

    /**
     * GitHub README 공통 함수 규격 호출:
     * productRepository.getMyProducts(status)
     */
    fun loadMyProducts() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    user = authRepository.currentUser.value,
                    isLoading = true,
                    errorMessage = null
                )
            }
            val selected = _uiState.value.selectedStatus
            val allRes = productRepository.getMyProducts(status = null)
            val filteredRes = if (selected == null) allRes else productRepository.getMyProducts(status = selected)

            allRes.fold(
                onSuccess = { all ->
                    val filtered = filteredRes.getOrElse { all }
                    _uiState.update {
                        it.copy(
                            allMyProducts = all,
                            filteredProducts = filtered,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = e.message ?: "불러오지 못했어요")
                    }
                }
            )
        }
    }

    fun selectStatusTab(status: TradeStatus?) {
        _uiState.update { it.copy(selectedStatus = status) }
        loadMyProducts()
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onLoggedOut()
        }
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
