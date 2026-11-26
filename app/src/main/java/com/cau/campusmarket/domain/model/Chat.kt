package com.cau.campusmarket.domain.model

enum class MessageType {
    TEXT,
    IMAGE,
    SYSTEM;

    companion object {
        fun fromName(name: String): MessageType =
            entries.firstOrNull { it.name == name } ?: TEXT
    }
}

enum class MessageSendState {
    SENT,
    SENDING,
    FAILED
}

/**
 * GitHub README 6. 데이터 구조 — chatRooms/{chatId}
 */
data class ChatRoom(
    val chatId: String = "",
    val productId: String = "",
    val sellerId: String = "",
    val buyerId: String = "",
    val activeUserIds: List<String> = emptyList(), // 나가지 않은 참여자
    val lastMessage: String? = null, // 사진이면 "사진"
    val lastMessageAt: Long? = null,
    val unreadCount: Map<String, Int> = emptyMap(),
    val lastReadAt: Map<String, Long> = emptyMap(),
    val createdAt: Long = System.currentTimeMillis(),
    // UI 표시용 부가 메타데이터 (물품 삭제 시 productDeleted = true -> '삭제된 물품' 표시)
    val sellerNickname: String = "",
    val buyerNickname: String = "",
    val productTitle: String = "",
    val productPrice: Int = 0,
    val productThumbnailUrl: String = "",
    val productStatus: TradeStatus = TradeStatus.SELLING,
    val productDeleted: Boolean = false
) {
    fun opponentId(currentUserId: String): String =
        if (currentUserId == buyerId) sellerId else buyerId

    fun opponentNickname(currentUserId: String): String =
        if (currentUserId == buyerId) sellerNickname else buyerNickname

    fun isOpponentLeft(currentUserId: String): Boolean =
        opponentId(currentUserId) !in activeUserIds

    fun unreadCountFor(currentUserId: String): Int =
        unreadCount[currentUserId] ?: 0

    fun formattedUnreadBadge(currentUserId: String): String? {
        val count = unreadCountFor(currentUserId)
        return when {
            count <= 0 -> null
            count >= 100 -> "99+"
            else -> count.toString()
        }
    }

    fun opponentLastReadAt(currentUserId: String): Long =
        lastReadAt[opponentId(currentUserId)] ?: 0L
}

/**
 * GitHub README 6. 데이터 구조 — chatRooms/{chatId}/messages/{messageId}
 */
data class Message(
    val messageId: String = "",
    val senderId: String = "", // SYSTEM이면 "system"
    val type: MessageType = MessageType.TEXT,
    val text: String? = null,
    val imageUrls: List<String> = emptyList(), // IMAGE일 때 1~10개
    val createdAt: Long = System.currentTimeMillis(),
    val sendState: MessageSendState = MessageSendState.SENT
)
