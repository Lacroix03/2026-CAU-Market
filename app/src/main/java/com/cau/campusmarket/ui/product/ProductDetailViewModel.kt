package com.cau.campusmarket.ui.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cau.campusmarket.data.repository.AuthRepository
import com.cau.campusmarket.data.repository.ChatRepository
import com.cau.campusmarket.data.repository.ProductRepository
import com.cau.campusmarket.domain.model.Product
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

    private val _uiState = MutableStateFlow(
        ProductDetailUiState(currentUserId = authRepository.currentUser.value?.userId.orEmpty())
    )
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    init {
        loadProduct()
    }

    fun loadProduct() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    currentUserId = authRepository.currentUser.value?.userId.orEmpty()
                )
            }
            productRepository.getProduct(productId).fold(
                onSuccess = { p -> _uiState.update { it.copy(product = p, isLoading = false) } },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = e.message ?: "불러오지 못했어요")
                    }
                }
            )
        }
    }

    /**
     * GitHub README 공통 함수 규격 호출:
     * chatRepository.getOrCreateChatRoom(productId)
     */
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
                        it.copy(isSubmittingAction = false, errorMessage = e.message ?: "채팅방을 열지 못했어요.")
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
                        it.copy(isSubmittingAction = false, errorMessage = e.message ?: "삭제하지 못했어요.")
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
