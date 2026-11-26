package com.cau.campusmarket.data.repository

import com.cau.campusmarket.domain.model.CampusPlace
import com.cau.campusmarket.domain.model.ChatRoom
import com.cau.campusmarket.domain.model.MeetupSchedule
import com.cau.campusmarket.domain.model.Message
import com.cau.campusmarket.domain.model.Product
import com.cau.campusmarket.domain.model.ProductCategory
import com.cau.campusmarket.domain.model.ProductInput
import com.cau.campusmarket.domain.model.TradeStatus
import com.cau.campusmarket.domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * GitHub README 7. 공통 함수 규격 (Repository)
 * - 모든 함수는 suspend 또는 Flow이며, 실패는 Result로 전달합니다.
 */
interface AuthRepository {
    val currentUser: StateFlow<User?>

    suspend fun signUp(email: String, password: String, nickname: String): Result<Unit>
    suspend fun login(email: String, password: String): Result<User>
    suspend fun logout(): Result<Unit>
}

interface ProductRepository {
    suspend fun getProducts(
        query: String? = null,
        category: ProductCategory? = null,
        includeSold: Boolean = false
    ): Result<List<Product>>

    suspend fun getMyProducts(
        status: TradeStatus? = null
    ): Result<List<Product>>

    suspend fun getProduct(productId: String): Result<Product>

    suspend fun createProduct(
        input: ProductInput,
        imageUris: List<String>
    ): Result<String>

    suspend fun updateProduct(
        productId: String,
        input: ProductInput,
        imageUris: List<String>
    ): Result<Unit>

    suspend fun deleteProduct(productId: String): Result<Unit>

    suspend fun changeProductStatus(
        productId: String,
        status: TradeStatus
    ): Result<Unit>
}

interface ChatRepository {
    fun observeChatRooms(): Flow<List<ChatRoom>>
    suspend fun getOrCreateChatRoom(productId: String): Result<String>
    fun observeMessages(chatId: String): Flow<List<Message>>
    suspend fun sendMessage(chatId: String, text: String): Result<Unit>
    suspend fun sendImages(chatId: String, imageUris: List<String>): Result<Unit>
    suspend fun markAsRead(chatId: String): Result<Unit>
    suspend fun leaveChatRoom(chatId: String): Result<Unit>
}

interface MeetupRepository {
    fun observeMeetup(chatId: String): Flow<MeetupSchedule?>
    suspend fun proposeMeetup(
        chatId: String,
        productId: String,
        place: CampusPlace,
        placeDetail: String,
        dateText: String,
        timeText: String,
        memo: String
    ): Result<MeetupSchedule>
    suspend fun acceptMeetup(chatId: String): Result<Unit>
    suspend fun cancelMeetup(chatId: String): Result<Unit>
}
