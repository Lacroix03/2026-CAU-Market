package com.caumarket.app.data.repository
import android.net.Uri
import com.caumarket.app.core.model.*
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun observeChatRooms(): Flow<Result<List<ChatRoom>>>
    suspend fun getOrCreateChatRoom(productId: String): Result<String>
    fun observeMessages(chatId: String): Flow<Result<List<Message>>>
    suspend fun sendMessage(chatId: String, text: String, requestId: String? = null): Result<Unit>
    suspend fun sendImages(chatId: String, imageUris: List<Uri>, requestId: String? = null): Result<Unit>
    suspend fun markAsRead(chatId: String): Result<Unit>
    suspend fun markAsRead(chatId: String, throughMessageId: String): Result<Unit>
    suspend fun leaveChatRoom(chatId: String): Result<Unit>
    suspend fun getChatRoomInfo(chatId: String): Result<ChatRoomInfo>
    suspend fun getChatImage(chatId: String, messageId: String, index: Int): Result<ByteArray>
}
