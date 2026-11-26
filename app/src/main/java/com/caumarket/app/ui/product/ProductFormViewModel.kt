package com.caumarket.app.ui.product

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.caumarket.app.core.error.MarketException
import com.caumarket.app.core.model.ProductInput
import com.caumarket.app.core.model.ProductPhoto
import com.caumarket.app.core.model.ProductUpdate
import com.caumarket.app.data.repository.ProductRepository
import com.caumarket.app.ui.common.MarketErrorMapper
import com.caumarket.app.ui.common.formatDisposeInput
import com.caumarket.app.ui.common.parseSeoulDateToTimestamp
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProductFormUiState(
    val isEditMode: Boolean = false,
    val photos: List<ProductPhoto> = emptyList(),
    val status: String = "SELLING",
    val originalStatus: String = "SELLING",
    val expectedUpdatedAt: Timestamp? = null,
    val title: String = "",
    val category: String = "TEXTBOOK",
    val priceText: String = "",
    val isFreeShare: Boolean = false,
    val condition: String = "GOOD",
    val description: String = "",
    val place: String = "LIBRARY",
    val placeDetail: String = "",
    val disposeByText: String = "",
    val pendingRequestId: String? = null,
    val isLoadingInitial: Boolean = false,
    val isSubmitting: Boolean = false,
    val isDirty: Boolean = false,
    val errorMessage: String? = null
)

class ProductFormViewModel(
    private val editingProductId: String?,
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ProductFormUiState(
            isEditMode = editingProductId != null,
            isLoadingInitial = editingProductId != null
        )
    )
    val uiState: StateFlow<ProductFormUiState> = _uiState.asStateFlow()

    init {
        if (editingProductId != null) loadExisting(editingProductId)
    }

    private fun loadExisting(productId: String) {
        viewModelScope.launch {
            productRepository.getProduct(productId).fold(
                onSuccess = { p ->
                    _uiState.update {
                        it.copy(
                            photos = p.imageUrls.map { url -> ProductPhoto.Existing(url) },
                            status = p.status,
                            originalStatus = p.status,
                            expectedUpdatedAt = p.updatedAt,
                            title = p.title,
                            category = p.category,
                            priceText = if (p.price == 0) "" else p.price.toString(),
                            isFreeShare = p.price == 0,
                            condition = p.condition,
                            description = p.description.orEmpty(),
                            place = p.place,
                            placeDetail = p.placeDetail.orEmpty(),
                            disposeByText = formatDisposeInput(p.disposeBy),
                            isLoadingInitial = false,
                            isDirty = false
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(isLoadingInitial = false, errorMessage = MarketErrorMapper.toMessage(e))
                    }
                }
            )
        }
    }

    fun addNewPhotos(uris: List<Uri>) {
        _uiState.update { s ->
            val added = uris.map { ProductPhoto.New(it) }
            s.copy(
                photos = (s.photos + added).take(15),
                isDirty = true,
                pendingRequestId = null,
                errorMessage = null
            )
        }
    }

    fun removePhoto(index: Int) {
        _uiState.update { s ->
            if (index !in s.photos.indices) return@update s
            s.copy(
                photos = s.photos.toMutableList().apply { removeAt(index) },
                isDirty = true,
                pendingRequestId = null
            )
        }
    }

    fun onStatusChange(status: String) = _uiState.update { it.copy(status = status, isDirty = true) }

    fun onTitleChange(v: String) {
        if (v.length <= 50) _uiState.update { it.copy(title = v, isDirty = true, pendingRequestId = null, errorMessage = null) }
    }

    fun onCategoryChange(code: String) = _uiState.update { it.copy(category = code, isDirty = true, pendingRequestId = null) }

    fun onPriceChange(v: String) {
        val digits = v.filter { it.isDigit() }.take(8)
        _uiState.update { it.copy(priceText = digits, isDirty = true, pendingRequestId = null, errorMessage = null) }
    }

    fun onFreeShareToggle(checked: Boolean) {
        _uiState.update {
            it.copy(
                isFreeShare = checked,
                priceText = if (checked) "0" else "",
                isDirty = true,
                pendingRequestId = null,
                errorMessage = null
            )
        }
    }

    fun onConditionChange(code: String) = _uiState.update { it.copy(condition = code, isDirty = true, pendingRequestId = null) }

    fun onDescriptionChange(v: String) {
        if (v.length <= 1000) _uiState.update { it.copy(description = v, isDirty = true, pendingRequestId = null, errorMessage = null) }
    }

    fun onPlaceChange(code: String) = _uiState.update { it.copy(place = code, isDirty = true, pendingRequestId = null) }

    fun onPlaceDetailChange(v: String) {
        if (v.length <= 30) _uiState.update { it.copy(placeDetail = v, isDirty = true, pendingRequestId = null) }
    }

    fun onDisposeByChange(v: String) = _uiState.update { it.copy(disposeByText = v, isDirty = true, pendingRequestId = null) }

    fun submit(onSaved: (String) -> Unit) {
        val s = _uiState.value
        if (s.isSubmitting) return

        if (s.photos.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "사진을 최소 1장 이상 등록해 주세요.") }
            return
        }
        val price = if (s.isFreeShare) 0 else s.priceText.toIntOrNull()
        if (price == null) {
            _uiState.update { it.copy(errorMessage = "가격을 숫자로 입력하거나 무료 나눔을 선택해 주세요.") }
            return
        }
        val disposeTimestamp = try {
            parseSeoulDateToTimestamp(s.disposeByText)
        } catch (_: Exception) {
            _uiState.update { it.copy(errorMessage = "처분 희망 기한은 YYYY-MM-DD 형식으로 입력해 주세요.") }
            return
        }

        val input = ProductInput(
            title = s.title,
            description = s.description.takeIf { it.isNotBlank() },
            price = price,
            category = s.category,
            condition = s.condition,
            place = s.place,
            placeDetail = s.placeDetail.takeIf { it.isNotBlank() },
            disposeBy = disposeTimestamp
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            if (editingProductId == null) {
                val uris = s.photos.mapNotNull { (it as? ProductPhoto.New)?.uri }
                productRepository.createProduct(input, uris, requestId = s.pendingRequestId).fold(
                    onSuccess = { newId ->
                        _uiState.update { it.copy(isSubmitting = false, isDirty = false, pendingRequestId = null) }
                        onSaved(newId)
                    },
                    onFailure = { e ->
                        val opId = (e as? MarketException)?.operationId
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                pendingRequestId = opId ?: it.pendingRequestId,
                                errorMessage = MarketErrorMapper.toMessage(e)
                            )
                        }
                    }
                )
            } else {
                val expectedUpdatedAt = s.expectedUpdatedAt ?: Timestamp.now()
                val updateResult = productRepository.updateProduct(
                    productId = editingProductId,
                    update = ProductUpdate(input = input, expectedUpdatedAt = expectedUpdatedAt),
                    photos = s.photos,
                    requestId = s.pendingRequestId
                )
                if (updateResult.isFailure) {
                    val e = updateResult.exceptionOrNull()!!
                    val opId = (e as? MarketException)?.operationId
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            pendingRequestId = opId ?: it.pendingRequestId,
                            errorMessage = MarketErrorMapper.toMessage(e)
                        )
                    }
                    return@launch
                }

                // 백엔드 규격: 거래 상태는 changeProductStatus로 별도 변경
                if (s.status != s.originalStatus) {
                    val statusResult = productRepository.changeProductStatus(editingProductId, s.status)
                    if (statusResult.isFailure) {
                        val e = statusResult.exceptionOrNull()!!
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                errorMessage = "물품 정보는 저장되었으나 상태 변경에 실패했어요: ${MarketErrorMapper.toMessage(e)}"
                            )
                        }
                        return@launch
                    }
                }
                _uiState.update { it.copy(isSubmitting = false, isDirty = false, pendingRequestId = null) }
                onSaved(editingProductId)
            }
        }
    }

    companion object {
        fun factory(
            editingProductId: String?,
            productRepository: ProductRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ProductFormViewModel(editingProductId, productRepository) as T
        }
    }
}
