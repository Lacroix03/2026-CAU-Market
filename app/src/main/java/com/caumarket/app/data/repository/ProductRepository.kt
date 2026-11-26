package com.caumarket.app.data.repository
import android.net.Uri
import com.caumarket.app.core.model.*

interface ProductRepository {
    suspend fun getProducts(query: String? = null, category: String? = null,
        includeSold: Boolean = !query.isNullOrBlank()): Result<List<Product>>
    suspend fun getMyProducts(status: String? = null): Result<List<Product>>
    suspend fun getProduct(productId: String): Result<Product>
    suspend fun createProduct(input: ProductInput, imageUris: List<Uri>, requestId: String? = null): Result<String>
    suspend fun updateProduct(productId: String, update: ProductUpdate, photos: List<ProductPhoto>,
        requestId: String? = null): Result<Unit>
    suspend fun deleteProduct(productId: String): Result<Unit>
    suspend fun changeProductStatus(productId: String, status: String): Result<Unit>
}
