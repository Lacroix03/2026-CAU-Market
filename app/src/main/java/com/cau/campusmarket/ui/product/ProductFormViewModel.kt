package com.cau.campusmarket.ui.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cau.campusmarket.data.repository.ProductRepository
import com.cau.campusmarket.domain.model.CampusPlace
import com.cau.campusmarket.domain.model.ProductCategory
import com.cau.campusmarket.domain.model.ProductCondition
import com.cau.campusmarket.domain.model.ProductInput
import com.cau.campusmarket.domain.model.TradeStatus
import com.cau.campusmarket.domain.validation.Validators
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

data class ProductFormUiState(
    val isEditMode: Boolean = false,
    val imageUris: List<String> = emptyList(),
    val status: TradeStatus = TradeStatus.SELLING,
    val originalStatus: TradeStatus = TradeStatus.SELLING,
    val title: String = "",
    val category: ProductCategory = ProductCategory.TEXTBOOK,
    val priceText: String = "",
    val isFreeShare: Boolean = false,
    val condition: ProductCondition = ProductCondition.GOOD,
    val description: String = "",
    val place: CampusPlace = CampusPlace.LIBRARY,
    val placeDetail: String = "",
    val disposeByText: String = "", // YYYY-MM-DD (선택)
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
                    val disposeStr = p.disposeBy?.let {
                        SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(it)
                    }.orEmpty()
                    _uiState.update {
                        it.copy(
                            imageUris = p.imageUrls,
                            status = p.status,
                            originalStatus = p.status,
                            title = p.title,
                            category = p.category,
                            priceText = if (p.price == 0) "" else p.price.toString(),
                            isFreeShare = p.price == 0,
                            condition = p.condition,
                            description = p.description.orEmpty(),
                            place = p.place,
                            placeDetail = p.placeDetail.orEmpty(),
                            disposeByText = disposeStr,
                            isLoadingInitial = false,
                            isDirty = false
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(isLoadingInitial = false, errorMessage = e.message ?: "불러오지 못했어요")
                    }
                }
            )
        }
    }

    fun addImages(uris: List<String>) {
        _uiState.update { s ->
            s.copy(
                imageUris = (s.imageUris + uris).take(Validators.MAX_PRODUCT_PHOTOS),
                isDirty = true,
                errorMessage = null
            )
        }
    }

    fun addDemoPhoto() {
        val samples = listOf(
            "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=800",
            "https://images.unsplash.com/photo-1491553895911-0055eca6402d?w=800",
            "https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=800"
        )
        addImages(listOf(samples[_uiState.value.imageUris.size % samples.size]))
    }

    fun removeImage(index: Int) {
        _uiState.update { s ->
            if (index !in s.imageUris.indices) return@update s
            s.copy(
                imageUris = s.imageUris.toMutableList().apply { removeAt(index) },
                isDirty = true
            )
        }
    }

    fun onStatusChange(status: TradeStatus) = _uiState.update { it.copy(status = status, isDirty = true) }

    fun onTitleChange(v: String) {
        if (v.length <= Validators.MAX_TITLE_LENGTH) {
            _uiState.update { it.copy(title = v, isDirty = true, errorMessage = null) }
        }
    }

    fun onCategoryChange(c: ProductCategory) = _uiState.update { it.copy(category = c, isDirty = true) }

    fun onPriceChange(v: String) {
        val digits = v.filter { it.isDigit() }.take(8)
        _uiState.update { it.copy(priceText = digits, isDirty = true, errorMessage = null) }
    }

    fun onFreeShareToggle(checked: Boolean) {
        _uiState.update {
            it.copy(
                isFreeShare = checked,
                priceText = if (checked) "0" else "",
                isDirty = true,
                errorMessage = null
            )
        }
    }

    fun onConditionChange(c: ProductCondition) = _uiState.update { it.copy(condition = c, isDirty = true) }

    fun onDescriptionChange(v: String) {
        if (v.length <= Validators.MAX_DESCRIPTION_LENGTH) {
            _uiState.update { it.copy(description = v, isDirty = true, errorMessage = null) }
        }
    }

    fun onPlaceChange(p: CampusPlace) = _uiState.update { it.copy(place = p, isDirty = true) }

    fun onPlaceDetailChange(v: String) {
        if (v.length <= Validators.MAX_PLACE_DETAIL_LENGTH) {
            _uiState.update { it.copy(placeDetail = v, isDirty = true) }
        }
    }

    fun onDisposeByChange(v: String) = _uiState.update { it.copy(disposeByText = v, isDirty = true) }

    fun submit(onSaved: (String) -> Unit) {
        val s = _uiState.value
        if (s.isSubmitting) return

        val price = if (s.isFreeShare) 0 else s.priceText.toIntOrNull()
        if (price == null) {
            _uiState.update { it.copy(errorMessage = "가격을 숫자로 입력하거나 무료 나눔을 선택해 주세요.") }
            return
        }

        val parsedDisposeBy: Long? = s.disposeByText.trim().takeIf { it.isNotEmpty() }?.let { txt ->
            runCatching { SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).parse(txt)?.time }.getOrNull()
        }

        val input = ProductInput(
            title = s.title,
            description = s.description.takeIf { it.isNotBlank() },
            price = price,
            category = s.category,
            condition = s.condition,
            place = s.place,
            placeDetail = s.placeDetail.takeIf { it.isNotBlank() },
            disposeBy = parsedDisposeBy,
            status = s.status
        )

        Validators.validateProductInput(input, s.imageUris)?.let { err ->
            _uiState.update { it.copy(errorMessage = err) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            if (editingProductId == null) {
                productRepository.createProduct(input, s.imageUris).fold(
                    onSuccess = { newId ->
                        _uiState.update { it.copy(isSubmitting = false, isDirty = false) }
                        onSaved(newId)
                    },
                    onFailure = { e ->
                        _uiState.update {
                            it.copy(isSubmitting = false, errorMessage = e.message ?: "저장하지 못했어요. 다시 시도해 주세요.")
                        }
                    }
                )
            } else {
                productRepository.updateProduct(editingProductId, input, s.imageUris).fold(
                    onSuccess = {
                        _uiState.update { it.copy(isSubmitting = false, isDirty = false) }
                        onSaved(editingProductId)
                    },
                    onFailure = { e ->
                        _uiState.update {
                            it.copy(isSubmitting = false, errorMessage = e.message ?: "저장하지 못했어요. 다시 시도해 주세요.")
                        }
                    }
                )
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
