package com.caumarket.app.core.model
import com.google.firebase.Timestamp
data class ChatRoom(val chatId: String, val productId: String, val sellerId: String,
 val buyerId: String, val activeUserIds: List<String>, val lastMessage: String?,
 val lastMessageAt: Timestamp?, val unreadCount: Map<String, Int>,
 val lastReadAt: Map<String, Timestamp>, val createdAt: Timestamp,
 val lastSequence: Long, val lastReadSequence: Map<String, Long>)
