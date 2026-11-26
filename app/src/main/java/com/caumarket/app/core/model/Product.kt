package com.caumarket.app.core.model

import com.google.firebase.Timestamp

/** README section 6 product document. Enum values are stored as uppercase strings. */
data class Product(
    val productId: String,
    val sellerId: String,
    val sellerNickname: String,
    val title: String,
    val description: String?,
    val price: Int,
    val category: String,
    val condition: String,
    val imageUrls: List<String>,
    val place: String,
    val placeDetail: String?,
    val disposeBy: Timestamp?,
    val status: String,
    val createdAt: Timestamp,
    val updatedAt: Timestamp,
)
