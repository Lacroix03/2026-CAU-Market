package com.caumarket.app.core.model

import android.net.Uri
import com.google.firebase.Timestamp

data class ProductInput(val title: String, val description: String?, val price: Int,
    val category: String, val condition: String, val place: String,
    val placeDetail: String?, val disposeBy: Timestamp?)
data class ProductUpdate(val input: ProductInput, val expectedUpdatedAt: Timestamp)
sealed interface ProductPhoto {
    data class Existing(val url: String) : ProductPhoto
    data class New(val uri: Uri) : ProductPhoto
}
data class ChatRoomInfo(val opponentId: String, val opponentNickname: String, val product: Product?)
