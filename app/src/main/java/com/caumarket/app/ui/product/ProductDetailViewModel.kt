package com.caumarket.app.ui.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.caumarket.app.core.model.Product
import com.caumarket.app.data.repository.AuthRepository
import com.caumarket.app.data.repository.ChatRepository
import com.caumarket.app.data.repository.ProductRepository
import com.caumarket.app.ui.common.MarketErrorMapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProductDetailUiState(
    val product: Product? = null,
    val currentUserId: String = "",
    val isLoading: Boolean = true,
    val isSubmittingAction: Boolean = false,
    val errorMessage: String? = null
) {
    val isOwner: Boolean get() = product != null && product.sellerId == currentUserId
}

class ProductDetailViewModel(
    private val productId: String,
    private val authRepository: AuthRepository,
    private val productRepository: ProductRepository,
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductDetailUiState())
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    init {
        loadProduct()
    }

    fun loadProduct() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val uid = authRepository.getCurrentUser().getOrNull()?.userId.orEmpty()
            productRepository.getProduct(productId).fold(
                onSuccess = { p ->
                    _uiState.update { it.copy(product = p, currentUserId = uid, isLoading = false) }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            currentUserId = uid,
                            isLoading = false,
                            errorMessage = MarketErrorMapper.toMessage(e, "불러오지 못했어요")
                        )
                    }
                }
            )
        }
    }

    fun startChat(onChatOpened: (String) -> Unit) {
        if (_uiState.value.isSubmittingAction) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingAction = true, errorMessage = null) }
            chatRepository.getOrCreateChatRoom(productId).fold(
                onSuccess = { chatId ->
                    _uiState.update { it.copy(isSubmittingAction = false) }
                    onChatOpened(chatId)
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(isSubmittingAction = false, errorMessage = MarketErrorMapper.toMessage(e))
                    }
                }
            )
        }
    }

    fun deleteProduct(onDeleted: () -> Unit) {
        if (_uiState.value.isSubmittingAction) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingAction = true, errorMessage = null) }
            productRepository.deleteProduct(productId).fold(
                onSuccess = {
                    _uiState.update { it.copy(isSubmittingAction = false) }
                    onDeleted()
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(isSubmittingAction = false, errorMessage = MarketErrorMapper.toMessage(e))
                    }
                }
            )
        }
    }

    companion object {
        fun factory(
            productId: String,
            authRepository: AuthRepository,
            productRepository: ProductRepository,
            chatRepository: ChatRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ProductDetailViewModel(productId, authRepository, productRepository, chatRepository) as T
        }
    }
}
