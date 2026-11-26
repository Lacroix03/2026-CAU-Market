package com.cau.campusmarket.data.repository.demo

import com.cau.campusmarket.data.repository.AuthRepository
import com.cau.campusmarket.data.repository.ChatRepository
import com.cau.campusmarket.data.repository.MeetupRepository
import com.cau.campusmarket.data.repository.ProductRepository
import com.cau.campusmarket.domain.model.CampusPlace
import com.cau.campusmarket.domain.model.ChatRoom
import com.cau.campusmarket.domain.model.MeetupSchedule
import com.cau.campusmarket.domain.model.MeetupStatus
import com.cau.campusmarket.domain.model.Message
import com.cau.campusmarket.domain.model.MessageType
import com.cau.campusmarket.domain.model.Product
import com.cau.campusmarket.domain.model.ProductCategory
import com.cau.campusmarket.domain.model.ProductInput
import com.cau.campusmarket.domain.model.TradeStatus
import com.cau.campusmarket.domain.model.User
import com.cau.campusmarket.domain.validation.Validators
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.util.UUID

class DemoAuthRepository(
    private val ds: DemoDataSource
) : AuthRepository {

    override val currentUser: StateFlow<User?> = ds.currentUserFlow

    override suspend fun signUp(email: String, password: String, nickname: String): Result<Unit> {
        delay(300)
        val normalized = email.trim().lowercase()
        Validators.validateCauEmail(normalized)?.let { return Result.failure(IllegalArgumentException(it)) }
        Validators.validateNickname(nickname)?.let { return Result.failure(IllegalArgumentException(it)) }
        Validators.validatePassword(password)?.let { return Result.failure(IllegalArgumentException(it)) }
        if (ds.users.containsKey(normalized)) {
            return Result.failure(IllegalStateException("이미 가입된 학교 이메일입니다."))
        }
        val user = User(
            userId = "user_${UUID.randomUUID().toString().take(8)}",
            email = normalized,
            nickname = nickname.trim(),
            createdAt = System.currentTimeMillis()
        )
        ds.users[normalized] = user
        ds.credentials[normalized] = password
        return Result.success(Unit)
    }

    override suspend fun login(email: String, password: String): Result<User> {
        delay(300)
        val normalized = email.trim().lowercase()
        val savedPw = ds.credentials[normalized]
        val user = ds.users[normalized]
        if (savedPw == null || user == null || savedPw != password) {
            return Result.failure(IllegalArgumentException("이메일 또는 비밀번호가 일치하지 않습니다."))
        }
        ds.currentUserFlow.value = user
        return Result.success(user)
    }

    override suspend fun logout(): Result<Unit> {
        ds.currentUserFlow.value = null
        return Result.success(Unit)
    }
}

class DemoProductRepository(
    private val ds: DemoDataSource
) : ProductRepository {

    override suspend fun getProducts(
        query: String?,
        category: ProductCategory?,
        includeSold: Boolean
    ): Result<List<Product>> {
        delay(120)
        val trimmed = query?.trim()?.lowercase().orEmpty()
        val list = ds.productsFlow.value
            .asSequence()
            .filter { p -> includeSold || p.status != TradeStatus.SOLD }
            .filter { p -> category == null || p.category == category }
            .filter { p ->
                trimmed.isEmpty() ||
                    p.title.lowercase().contains(trimmed) ||
                    (p.description?.lowercase()?.contains(trimmed) == true)
            }
            .sortedByDescending { it.createdAt }
            .toList()
        return Result.success(list)
    }

    override suspend fun getMyProducts(status: TradeStatus?): Result<List<Product>> {
        val me = ds.currentUserFlow.value
            ?: return Result.failure(IllegalStateException("로그인이 필요합니다."))
        delay(100)
        val list = ds.productsFlow.value
            .filter { it.sellerId == me.userId && (status == null || it.status == status) }
            .sortedByDescending { it.createdAt }
        return Result.success(list)
    }

    override suspend fun getProduct(productId: String): Result<Product> {
        val found = ds.productsFlow.value.firstOrNull { it.productId == productId }
            ?: return Result.failure(NoSuchElementException("물품을 찾을 수 없습니다."))
        return Result.success(found)
    }

    override suspend fun createProduct(
        input: ProductInput,
        imageUris: List<String>
    ): Result<String> {
        val seller = ds.currentUserFlow.value
            ?: return Result.failure(IllegalStateException("로그인이 필요합니다."))
        Validators.validateProductInput(input, imageUris)?.let {
            return Result.failure(IllegalArgumentException(it))
        }
        delay(250)
        val now = System.currentTimeMillis()
        val id = "prod_${UUID.randomUUID().toString().take(8)}"
        val product = Product(
            productId = id,
            sellerId = seller.userId,
            sellerNickname = seller.nickname,
            title = input.title.trim(),
            description = input.description?.trim()?.takeIf { it.isNotEmpty() },
            price = input.price,
            category = input.category,
            condition = input.condition,
            imageUrls = imageUris,
            place = input.place,
            placeDetail = input.placeDetail?.trim()?.takeIf { it.isNotEmpty() },
            disposeBy = input.disposeBy,
            status = TradeStatus.SELLING,
            createdAt = now,
            updatedAt = now
        )
        ds.productsFlow.update { listOf(product) + it }
        return Result.success(id)
    }

    override suspend fun updateProduct(
        productId: String,
        input: ProductInput,
        imageUris: List<String>
    ): Result<Unit> {
        Validators.validateProductInput(input, imageUris)?.let {
            return Result.failure(IllegalArgumentException(it))
        }
        delay(250)
        if (ds.productsFlow.value.none { it.productId == productId }) {
            return Result.failure(NoSuchElementException("수정할 물품을 찾을 수 없습니다."))
        }
        ds.productsFlow.update { list ->
            list.map { p ->
                if (p.productId == productId) {
                    p.copy(
                        title = input.title.trim(),
                        description = input.description?.trim()?.takeIf { it.isNotEmpty() },
                        price = input.price,
                        category = input.category,
                        condition = input.condition,
                        imageUrls = imageUris,
                        place = input.place,
                        placeDetail = input.placeDetail?.trim()?.takeIf { it.isNotEmpty() },
                        disposeBy = input.disposeBy,
                        status = input.status,
                        updatedAt = System.currentTimeMillis()
                    )
                } else p
            }
        }
        ds.chatRoomsFlow.update { rooms ->
            rooms.map { r ->
                if (r.productId == productId) {
                    r.copy(
                        productTitle = input.title.trim(),
                        productPrice = input.price,
                        productThumbnailUrl = imageUris.firstOrNull().orEmpty(),
                        productStatus = input.status
                    )
                } else r
            }
        }
        return Result.success(Unit)
    }

    override suspend fun deleteProduct(productId: String): Result<Unit> {
        delay(200)
        ds.productsFlow.update { list -> list.filterNot { it.productId == productId } }
        // README 정책: 물품 삭제 시 연결된 채팅방의 물품 요약에 '삭제된 물품' 표시
        ds.chatRoomsFlow.update { rooms ->
            rooms.map { r ->
                if (r.productId == productId) {
                    r.copy(productDeleted = true, productTitle = "삭제된 물품")
                } else r
            }
        }
        return Result.success(Unit)
    }

    override suspend fun changeProductStatus(productId: String, status: TradeStatus): Result<Unit> {
        delay(150)
        ds.productsFlow.update { list ->
            list.map { p ->
                if (p.productId == productId) p.copy(status = status, updatedAt = System.currentTimeMillis())
                else p
            }
        }
        ds.chatRoomsFlow.update { rooms ->
            rooms.map { r ->
                if (r.productId == productId) r.copy(productStatus = status) else r
            }
        }
        return Result.success(Unit)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class DemoChatRepository(
    private val ds: DemoDataSource
) : ChatRepository {

    override fun observeChatRooms(): Flow<List<ChatRoom>> =
        ds.currentUserFlow.flatMapLatest { user ->
            if (user == null) flowOf(emptyList())
            else ds.chatRoomsFlow.map { rooms ->
                rooms.filter { user.userId in it.activeUserIds }
                    .sortedByDescending { it.lastMessageAt ?: it.createdAt }
            }
        }

    override suspend fun getOrCreateChatRoom(productId: String): Result<String> {
        val buyer = ds.currentUserFlow.value
            ?: return Result.failure(IllegalStateException("로그인이 필요합니다."))
        val product = ds.productsFlow.value.firstOrNull { it.productId == productId }
            ?: return Result.failure(NoSuchElementException("물품을 찾을 수 없습니다."))

        if (product.status == TradeStatus.SOLD) {
            return Result.failure(IllegalStateException("거래 완료된 물품에는 신규 문의를 시작할 수 없어요."))
        }
        if (product.sellerId == buyer.userId) {
            return Result.failure(IllegalArgumentException("본인 물품에는 채팅을 시작할 수 없어요."))
        }

        // README 정책: 같은 물품·구매자 조합의 활성(나가지 않은) 채팅방은 1개만 유지
        val existing = ds.chatRoomsFlow.value.firstOrNull { r ->
            r.productId == productId &&
                r.buyerId == buyer.userId &&
                buyer.userId in r.activeUserIds
        }
        if (existing != null) return Result.success(existing.chatId)

        val now = System.currentTimeMillis()
        val chatId = "chat_${UUID.randomUUID().toString().take(8)}"
        val room = ChatRoom(
            chatId = chatId,
            productId = product.productId,
            sellerId = product.sellerId,
            buyerId = buyer.userId,
            activeUserIds = listOf(buyer.userId, product.sellerId),
            lastMessage = null,
            lastMessageAt = now,
            unreadCount = mapOf(buyer.userId to 0, product.sellerId to 0),
            lastReadAt = mapOf(buyer.userId to now),
            createdAt = now,
            sellerNickname = product.sellerNickname,
            buyerNickname = buyer.nickname,
            productTitle = product.title,
            productPrice = product.price,
            productThumbnailUrl = product.thumbnailUrl.orEmpty(),
            productStatus = product.status
        )
        ds.chatRoomsFlow.update { listOf(room) + it }
        ds.messagesByChatFlow.update { it + (chatId to emptyList()) }
        return Result.success(chatId)
    }

    override fun observeMessages(chatId: String): Flow<List<Message>> =
        ds.messagesByChatFlow.map { map ->
            map[chatId].orEmpty().sortedBy { it.createdAt }
        }

    override suspend fun sendMessage(chatId: String, text: String): Result<Unit> {
        val sender = ds.currentUserFlow.value
            ?: return Result.failure(IllegalStateException("로그인이 필요합니다."))
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return Result.failure(IllegalArgumentException("메시지를 입력해 주세요."))

        val room = ds.chatRoomsFlow.value.firstOrNull { it.chatId == chatId }
            ?: return Result.failure(NoSuchElementException("채팅방을 찾을 수 없습니다."))
        if (room.isOpponentLeft(sender.userId)) {
            return Result.failure(IllegalStateException("상대방이 채팅방을 나가 메시지를 보낼 수 없어요."))
        }

        delay(100)
        val now = System.currentTimeMillis()
        val msg = Message(
            messageId = "msg_${UUID.randomUUID().toString().take(8)}",
            senderId = sender.userId,
            type = MessageType.TEXT,
            text = trimmed,
            createdAt = now
        )
        appendMessage(chatId, room, sender.userId, msg, preview = trimmed)
        return Result.success(Unit)
    }

    override suspend fun sendImages(chatId: String, imageUris: List<String>): Result<Unit> {
        val sender = ds.currentUserFlow.value
            ?: return Result.failure(IllegalStateException("로그인이 필요합니다."))
        if (imageUris.isEmpty()) return Result.failure(IllegalArgumentException("전송할 사진을 선택해 주세요."))
        if (imageUris.size > Validators.MAX_CHAT_PHOTOS) {
            return Result.failure(IllegalArgumentException("사진은 한 번에 최대 10장까지 전송할 수 있어요."))
        }

        val room = ds.chatRoomsFlow.value.firstOrNull { it.chatId == chatId }
            ?: return Result.failure(NoSuchElementException("채팅방을 찾을 수 없습니다."))
        if (room.isOpponentLeft(sender.userId)) {
            return Result.failure(IllegalStateException("상대방이 채팅방을 나가 사진을 보낼 수 없어요."))
        }

        delay(150)
        val now = System.currentTimeMillis()
        val msg = Message(
            messageId = "msg_${UUID.randomUUID().toString().take(8)}",
            senderId = sender.userId,
            type = MessageType.IMAGE,
            text = null,
            imageUrls = imageUris,
            createdAt = now
        )
        appendMessage(chatId, room, sender.userId, msg, preview = "사진")
        return Result.success(Unit)
    }

    private fun appendMessage(
        chatId: String,
        room: ChatRoom,
        senderId: String,
        msg: Message,
        preview: String
    ) {
        ds.messagesByChatFlow.update { map ->
            map + (chatId to (map[chatId].orEmpty() + msg))
        }
        val opponentId = room.opponentId(senderId)
        ds.chatRoomsFlow.update { rooms ->
            rooms.map { r ->
                if (r.chatId == chatId) {
                    val unread = r.unreadCount.toMutableMap()
                    if (opponentId in r.activeUserIds) {
                        unread[opponentId] = (unread[opponentId] ?: 0) + 1
                    }
                    val readAt = r.lastReadAt.toMutableMap()
                    readAt[senderId] = msg.createdAt
                    r.copy(
                        lastMessage = preview,
                        lastMessageAt = msg.createdAt,
                        unreadCount = unread,
                        lastReadAt = readAt
                    )
                } else r
            }
        }
    }

    override suspend fun markAsRead(chatId: String): Result<Unit> {
        val user = ds.currentUserFlow.value ?: return Result.success(Unit)
        val now = System.currentTimeMillis()
        ds.chatRoomsFlow.update { rooms ->
            rooms.map { r ->
                if (r.chatId == chatId) {
                    val unread = r.unreadCount.toMutableMap().apply { put(user.userId, 0) }
                    val readAt = r.lastReadAt.toMutableMap().apply { put(user.userId, now) }
                    r.copy(unreadCount = unread, lastReadAt = readAt)
                } else r
            }
        }
        return Result.success(Unit)
    }

    override suspend fun leaveChatRoom(chatId: String): Result<Unit> {
        val user = ds.currentUserFlow.value
            ?: return Result.failure(IllegalStateException("로그인이 필요합니다."))
        val now = System.currentTimeMillis()
        // README 정책: 상대방에게 '상대방이 채팅방을 나갔습니다.' 시스템 메시지 표시
        val sysMsg = Message(
            messageId = "sys_${UUID.randomUUID().toString().take(8)}",
            senderId = "system",
            type = MessageType.SYSTEM,
            text = "상대방이 채팅방을 나갔습니다.",
            createdAt = now
        )
        ds.messagesByChatFlow.update { map ->
            map + (chatId to (map[chatId].orEmpty() + sysMsg))
        }
        ds.chatRoomsFlow.update { rooms ->
            rooms.map { r ->
                if (r.chatId == chatId) {
                    val unread = r.unreadCount.toMutableMap().apply { put(user.userId, 0) }
                    r.copy(
                        activeUserIds = r.activeUserIds.filterNot { it == user.userId },
                        unreadCount = unread
                    )
                } else r
            }
        }
        return Result.success(Unit)
    }
}

class DemoMeetupRepository(
    private val ds: DemoDataSource
) : MeetupRepository {

    override fun observeMeetup(chatId: String): Flow<MeetupSchedule?> =
        ds.meetupsByChatFlow.map { it[chatId] }

    override suspend fun proposeMeetup(
        chatId: String,
        productId: String,
        place: CampusPlace,
        placeDetail: String,
        dateText: String,
        timeText: String,
        memo: String
    ): Result<MeetupSchedule> {
        val proposer = ds.currentUserFlow.value
            ?: return Result.failure(IllegalStateException("로그인이 필요합니다."))
        if (dateText.isBlank() || timeText.isBlank()) {
            return Result.failure(IllegalArgumentException("날짜와 시간을 입력해 주세요."))
        }
        delay(150)
        val schedule = MeetupSchedule(
            scheduleId = "meetup_${UUID.randomUUID().toString().take(8)}",
            chatId = chatId,
            productId = productId,
            proposerId = proposer.userId,
            proposerNickname = proposer.nickname,
            place = place,
            placeDetail = placeDetail.trim(),
            dateText = dateText.trim(),
            timeText = timeText.trim(),
            memo = memo.trim(),
            status = MeetupStatus.PROPOSED,
            updatedAt = System.currentTimeMillis()
        )
        ds.meetupsByChatFlow.update { it + (chatId to schedule) }
        return Result.success(schedule)
    }

    override suspend fun acceptMeetup(chatId: String): Result<Unit> {
        val current = ds.meetupsByChatFlow.value[chatId]
            ?: return Result.failure(NoSuchElementException("제안된 거래 일정이 없습니다."))
        ds.meetupsByChatFlow.update {
            it + (chatId to current.copy(status = MeetupStatus.CONFIRMED, updatedAt = System.currentTimeMillis()))
        }
        return Result.success(Unit)
    }

    override suspend fun cancelMeetup(chatId: String): Result<Unit> {
        val current = ds.meetupsByChatFlow.value[chatId]
            ?: return Result.failure(NoSuchElementException("취소할 거래 일정이 없습니다."))
        ds.meetupsByChatFlow.update {
            it + (chatId to current.copy(status = MeetupStatus.CANCELLED, updatedAt = System.currentTimeMillis()))
        }
        return Result.success(Unit)
    }
}
