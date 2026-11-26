package com.cau.campusmarket.data.repository.firebase

import android.net.Uri
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
import com.cau.campusmarket.domain.model.ProductCondition
import com.cau.campusmarket.domain.model.ProductInput
import com.cau.campusmarket.domain.model.TradeStatus
import com.cau.campusmarket.domain.model.User
import com.cau.campusmarket.domain.validation.Validators
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID

class FirebaseAuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : AuthRepository {

    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser

    override suspend fun signUp(email: String, password: String, nickname: String): Result<Unit> = runCatching {
        val normalized = email.trim().lowercase()
        Validators.validateCauEmail(normalized)?.let { throw IllegalArgumentException(it) }
        Validators.validateNickname(nickname)?.let { throw IllegalArgumentException(it) }
        Validators.validatePassword(password)?.let { throw IllegalArgumentException(it) }

        val res = auth.createUserWithEmailAndPassword(normalized, password).await()
        val uid = res.user?.uid ?: throw IllegalStateException("UID 생성 실패")
        firestore.collection("users").document(uid).set(
            mapOf(
                "userId" to uid,
                "email" to normalized,
                "nickname" to nickname.trim(),
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
        auth.signOut()
        _currentUser.value = null
    }

    override suspend fun login(email: String, password: String): Result<User> = runCatching {
        val normalized = email.trim().lowercase()
        val res = auth.signInWithEmailAndPassword(normalized, password).await()
        val uid = res.user?.uid ?: throw IllegalStateException("로그인 실패")
        val doc = firestore.collection("users").document(uid).get().await()
        val user = User(
            userId = uid,
            email = doc.getString("email") ?: normalized,
            nickname = doc.getString("nickname") ?: normalized.substringBefore("@"),
            createdAt = doc.getTimestamp("createdAt")?.toDate()?.time ?: System.currentTimeMillis()
        )
        _currentUser.value = user
        user
    }

    override suspend fun logout(): Result<Unit> = runCatching {
        auth.signOut()
        _currentUser.value = null
    }
}

class FirebaseProductRepository(
    private val authRepository: AuthRepository,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) : ProductRepository {

    private val productsRef get() = firestore.collection("products")

    override suspend fun getProducts(
        query: String?,
        category: ProductCategory?,
        includeSold: Boolean
    ): Result<List<Product>> = runCatching {
        val snap = productsRef.orderBy("createdAt", Query.Direction.DESCENDING).get().await()
        val trimmed = query?.trim()?.lowercase().orEmpty()
        snap.documents.mapNotNull { it.toProduct() }.filter { p ->
            val statusOk = includeSold || p.status != TradeStatus.SOLD
            val catOk = category == null || p.category == category
            val queryOk = trimmed.isEmpty() ||
                p.title.lowercase().contains(trimmed) ||
                (p.description?.lowercase()?.contains(trimmed) == true)
            statusOk && catOk && queryOk
        }
    }

    override suspend fun getMyProducts(status: TradeStatus?): Result<List<Product>> = runCatching {
        val uid = authRepository.currentUser.value?.userId ?: throw IllegalStateException("로그인이 필요합니다.")
        val snap = productsRef
            .whereEqualTo("sellerId", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get().await()
        snap.documents.mapNotNull { it.toProduct() }
            .filter { status == null || it.status == status }
    }

    override suspend fun getProduct(productId: String): Result<Product> = runCatching {
        val doc = productsRef.document(productId).get().await()
        doc.toProduct() ?: throw NoSuchElementException("물품을 찾을 수 없습니다.")
    }

    override suspend fun createProduct(input: ProductInput, imageUris: List<String>): Result<String> = runCatching {
        val seller = authRepository.currentUser.value ?: throw IllegalStateException("로그인이 필요합니다.")
        Validators.validateProductInput(input, imageUris)?.let { throw IllegalArgumentException(it) }

        val docRef = productsRef.document()
        val urls = uploadProductImages(docRef.id, imageUris)
        docRef.set(
            mapOf(
                "productId" to docRef.id,
                "sellerId" to seller.userId,
                "sellerNickname" to seller.nickname,
                "title" to input.title.trim(),
                "description" to input.description?.trim()?.takeIf { it.isNotEmpty() },
                "price" to input.price,
                "category" to input.category.name,
                "condition" to input.condition.name,
                "imageUrls" to urls,
                "place" to input.place.name,
                "placeDetail" to input.placeDetail?.trim()?.takeIf { it.isNotEmpty() },
                "disposeBy" to input.disposeBy?.let { Timestamp(Date(it)) },
                "status" to TradeStatus.SELLING.name,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        docRef.id
    }

    override suspend fun updateProduct(
        productId: String,
        input: ProductInput,
        imageUris: List<String>
    ): Result<Unit> = runCatching {
        Validators.validateProductInput(input, imageUris)?.let { throw IllegalArgumentException(it) }
        val urls = uploadProductImages(productId, imageUris)
        productsRef.document(productId).update(
            mapOf(
                "title" to input.title.trim(),
                "description" to input.description?.trim()?.takeIf { it.isNotEmpty() },
                "price" to input.price,
                "category" to input.category.name,
                "condition" to input.condition.name,
                "imageUrls" to urls,
                "place" to input.place.name,
                "placeDetail" to input.placeDetail?.trim()?.takeIf { it.isNotEmpty() },
                "disposeBy" to input.disposeBy?.let { Timestamp(Date(it)) },
                "status" to input.status.name,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    override suspend fun deleteProduct(productId: String): Result<Unit> = runCatching {
        productsRef.document(productId).delete().await()
    }

    override suspend fun changeProductStatus(productId: String, status: TradeStatus): Result<Unit> = runCatching {
        productsRef.document(productId).update(
            mapOf(
                "status" to status.name,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    private suspend fun uploadProductImages(productId: String, uris: List<String>): List<String> {
        return uris.mapIndexed { index, uriStr ->
            if (uriStr.startsWith("http://") || uriStr.startsWith("https://")) uriStr
            else {
                // README Storage 경로 규칙: products/{productId}/{index}.jpg
                val ref = storage.reference.child("products/$productId/$index.jpg")
                ref.putFile(Uri.parse(uriStr)).await()
                ref.downloadUrl.await().toString()
            }
        }
    }

    private fun DocumentSnapshot.toProduct(): Product? {
        if (!exists()) return null
        return Product(
            productId = getString("productId") ?: id,
            sellerId = getString("sellerId").orEmpty(),
            sellerNickname = getString("sellerNickname").orEmpty(),
            title = getString("title").orEmpty(),
            description = getString("description"),
            price = (getLong("price") ?: 0L).toInt(),
            category = ProductCategory.fromName(getString("category").orEmpty()),
            condition = ProductCondition.fromName(getString("condition").orEmpty()),
            imageUrls = (get("imageUrls") as? List<*>)?.mapNotNull { it as? String }.orEmpty(),
            place = CampusPlace.fromName(getString("place").orEmpty()),
            placeDetail = getString("placeDetail"),
            disposeBy = getTimestamp("disposeBy")?.toDate()?.time,
            status = TradeStatus.fromName(getString("status").orEmpty()),
            createdAt = getTimestamp("createdAt")?.toDate()?.time ?: 0L,
            updatedAt = getTimestamp("updatedAt")?.toDate()?.time ?: 0L
        )
    }
}

class FirebaseChatRepository(
    private val authRepository: AuthRepository,
    private val productRepository: ProductRepository,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) : ChatRepository {

    private val roomsRef get() = firestore.collection("chatRooms")

    override fun observeChatRooms(): Flow<List<ChatRoom>> = callbackFlow {
        val uid = authRepository.currentUser.value?.userId
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val reg = roomsRef
            .whereArrayContains("activeUserIds", uid)
            .orderBy("lastMessageAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                trySend(snap?.documents.orEmpty().mapNotNull { it.toChatRoom() })
            }
        awaitClose { reg.remove() }
    }

    override suspend fun getOrCreateChatRoom(productId: String): Result<String> = runCatching {
        val buyer = authRepository.currentUser.value ?: throw IllegalStateException("로그인이 필요합니다.")
        val product = productRepository.getProduct(productId).getOrThrow()
        if (product.status == TradeStatus.SOLD) {
            throw IllegalStateException("거래 완료된 물품에는 신규 문의를 시작할 수 없어요.")
        }

        val existing = roomsRef
            .whereEqualTo("productId", productId)
            .whereEqualTo("buyerId", buyer.userId)
            .whereArrayContains("activeUserIds", buyer.userId)
            .get().await()
            .documents.firstOrNull()
        if (existing != null) return@runCatching existing.id

        val docRef = roomsRef.document()
        docRef.set(
            mapOf(
                "chatId" to docRef.id,
                "productId" to product.productId,
                "sellerId" to product.sellerId,
                "buyerId" to buyer.userId,
                "activeUserIds" to listOf(buyer.userId, product.sellerId),
                "lastMessage" to null,
                "lastMessageAt" to FieldValue.serverTimestamp(),
                "unreadCount" to mapOf(buyer.userId to 0, product.sellerId to 0),
                "lastReadAt" to mapOf(buyer.userId to FieldValue.serverTimestamp()),
                "createdAt" to FieldValue.serverTimestamp(),
                "sellerNickname" to product.sellerNickname,
                "buyerNickname" to buyer.nickname,
                "productTitle" to product.title,
                "productPrice" to product.price,
                "productThumbnailUrl" to product.thumbnailUrl.orEmpty(),
                "productStatus" to product.status.name
            )
        ).await()
        docRef.id
    }

    override fun observeMessages(chatId: String): Flow<List<Message>> = callbackFlow {
        val reg = roomsRef.document(chatId).collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, _ ->
                trySend(snap?.documents.orEmpty().mapNotNull { it.toMessage() })
            }
        awaitClose { reg.remove() }
    }

    override suspend fun sendMessage(chatId: String, text: String): Result<Unit> = runCatching {
        val uid = authRepository.currentUser.value?.userId ?: throw IllegalStateException("로그인이 필요합니다.")
        val trimmed = text.trim()
        if (trimmed.isEmpty()) throw IllegalArgumentException("메시지를 입력해 주세요.")

        val roomDoc = roomsRef.document(chatId).get().await()
        val room = roomDoc.toChatRoom() ?: throw NoSuchElementException("채팅방을 찾을 수 없습니다.")
        val opponentId = room.opponentId(uid)

        val msgRef = roomsRef.document(chatId).collection("messages").document()
        msgRef.set(
            mapOf(
                "messageId" to msgRef.id,
                "senderId" to uid,
                "type" to MessageType.TEXT.name,
                "text" to trimmed,
                "imageUrls" to emptyList<String>(),
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()

        val updates = mutableMapOf<String, Any>(
            "lastMessage" to trimmed,
            "lastMessageAt" to FieldValue.serverTimestamp(),
            "lastReadAt.$uid" to FieldValue.serverTimestamp()
        )
        if (opponentId in room.activeUserIds) {
            updates["unreadCount.$opponentId"] = FieldValue.increment(1)
        }
        roomsRef.document(chatId).update(updates).await()
    }

    override suspend fun sendImages(chatId: String, imageUris: List<String>): Result<Unit> = runCatching {
        val uid = authRepository.currentUser.value?.userId ?: throw IllegalStateException("로그인이 필요합니다.")
        if (imageUris.isEmpty() || imageUris.size > Validators.MAX_CHAT_PHOTOS) {
            throw IllegalArgumentException("사진은 1~10장까지 전송할 수 있어요.")
        }

        val room = roomsRef.document(chatId).get().await().toChatRoom()
            ?: throw NoSuchElementException("채팅방을 찾을 수 없습니다.")
        val opponentId = room.opponentId(uid)

        val msgRef = roomsRef.document(chatId).collection("messages").document()
        // README Storage 경로 규칙: chats/{chatId}/{messageId}_{index}.jpg
        val urls = imageUris.mapIndexed { index, uriStr ->
            if (uriStr.startsWith("http://") || uriStr.startsWith("https://")) uriStr
            else {
                val ref = storage.reference.child("chats/$chatId/${msgRef.id}_$index.jpg")
                ref.putFile(Uri.parse(uriStr)).await()
                ref.downloadUrl.await().toString()
            }
        }

        msgRef.set(
            mapOf(
                "messageId" to msgRef.id,
                "senderId" to uid,
                "type" to MessageType.IMAGE.name,
                "text" to null,
                "imageUrls" to urls,
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()

        val updates = mutableMapOf<String, Any>(
            "lastMessage" to "사진",
            "lastMessageAt" to FieldValue.serverTimestamp(),
            "lastReadAt.$uid" to FieldValue.serverTimestamp()
        )
        if (opponentId in room.activeUserIds) {
            updates["unreadCount.$opponentId"] = FieldValue.increment(1)
        }
        roomsRef.document(chatId).update(updates).await()
    }

    override suspend fun markAsRead(chatId: String): Result<Unit> = runCatching {
        val uid = authRepository.currentUser.value?.userId ?: return@runCatching
        roomsRef.document(chatId).update(
            mapOf(
                "unreadCount.$uid" to 0,
                "lastReadAt.$uid" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    override suspend fun leaveChatRoom(chatId: String): Result<Unit> = runCatching {
        val uid = authRepository.currentUser.value?.userId ?: throw IllegalStateException("로그인이 필요합니다.")
        val sysRef = roomsRef.document(chatId).collection("messages").document()
        sysRef.set(
            mapOf(
                "messageId" to sysRef.id,
                "senderId" to "system",
                "type" to MessageType.SYSTEM.name,
                "text" to "상대방이 채팅방을 나갔습니다.",
                "imageUrls" to emptyList<String>(),
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
        roomsRef.document(chatId).update(
            mapOf(
                "activeUserIds" to FieldValue.arrayRemove(uid),
                "unreadCount.$uid" to 0
            )
        ).await()
    }

    private fun DocumentSnapshot.toChatRoom(): ChatRoom? {
        if (!exists()) return null
        val unread = (get("unreadCount") as? Map<*, *>)?.mapNotNull { (k, v) ->
            val key = k as? String ?: return@mapNotNull null
            key to ((v as? Number)?.toInt() ?: 0)
        }?.toMap().orEmpty()
        val readAt = (get("lastReadAt") as? Map<*, *>)?.mapNotNull { (k, v) ->
            val key = k as? String ?: return@mapNotNull null
            val ts = (v as? Timestamp)?.toDate()?.time ?: (v as? Number)?.toLong() ?: 0L
            key to ts
        }?.toMap().orEmpty()
        return ChatRoom(
            chatId = getString("chatId") ?: id,
            productId = getString("productId").orEmpty(),
            sellerId = getString("sellerId").orEmpty(),
            buyerId = getString("buyerId").orEmpty(),
            activeUserIds = (get("activeUserIds") as? List<*>)?.mapNotNull { it as? String }.orEmpty(),
            lastMessage = getString("lastMessage"),
            lastMessageAt = getTimestamp("lastMessageAt")?.toDate()?.time,
            unreadCount = unread,
            lastReadAt = readAt,
            createdAt = getTimestamp("createdAt")?.toDate()?.time ?: 0L,
            sellerNickname = getString("sellerNickname").orEmpty(),
            buyerNickname = getString("buyerNickname").orEmpty(),
            productTitle = getString("productTitle").orEmpty(),
            productPrice = (getLong("productPrice") ?: 0L).toInt(),
            productThumbnailUrl = getString("productThumbnailUrl").orEmpty(),
            productStatus = TradeStatus.fromName(getString("productStatus").orEmpty()),
            productDeleted = getBoolean("productDeleted") ?: false
        )
    }

    private fun DocumentSnapshot.toMessage(): Message? {
        if (!exists()) return null
        return Message(
            messageId = getString("messageId") ?: id,
            senderId = getString("senderId").orEmpty(),
            type = MessageType.fromName(getString("type").orEmpty()),
            text = getString("text"),
            imageUrls = (get("imageUrls") as? List<*>)?.mapNotNull { it as? String }.orEmpty(),
            createdAt = getTimestamp("createdAt")?.toDate()?.time ?: 0L
        )
    }
}

class FirebaseMeetupRepository(
    private val authRepository: AuthRepository,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : MeetupRepository {

    private fun docRef(chatId: String) =
        firestore.collection("chatRooms").document(chatId).collection("meetup").document("current")

    override fun observeMeetup(chatId: String): Flow<MeetupSchedule?> = callbackFlow {
        val reg = docRef(chatId).addSnapshotListener { doc, _ ->
            if (doc == null || !doc.exists()) {
                trySend(null)
            } else {
                trySend(
                    MeetupSchedule(
                        scheduleId = doc.getString("scheduleId").orEmpty(),
                        chatId = doc.getString("chatId").orEmpty(),
                        productId = doc.getString("productId").orEmpty(),
                        proposerId = doc.getString("proposerId").orEmpty(),
                        proposerNickname = doc.getString("proposerNickname").orEmpty(),
                        place = CampusPlace.fromName(doc.getString("place").orEmpty()),
                        placeDetail = doc.getString("placeDetail").orEmpty(),
                        dateText = doc.getString("dateText").orEmpty(),
                        timeText = doc.getString("timeText").orEmpty(),
                        memo = doc.getString("memo").orEmpty(),
                        status = MeetupStatus.fromName(doc.getString("status").orEmpty()),
                        updatedAt = doc.getTimestamp("updatedAt")?.toDate()?.time ?: 0L
                    )
                )
            }
        }
        awaitClose { reg.remove() }
    }

    override suspend fun proposeMeetup(
        chatId: String,
        productId: String,
        place: CampusPlace,
        placeDetail: String,
        dateText: String,
        timeText: String,
        memo: String
    ): Result<MeetupSchedule> = runCatching {
        val proposer = authRepository.currentUser.value ?: throw IllegalStateException("로그인이 필요합니다.")
        val schedule = MeetupSchedule(
            scheduleId = UUID.randomUUID().toString(),
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
        docRef(chatId).set(
            mapOf(
                "scheduleId" to schedule.scheduleId,
                "chatId" to schedule.chatId,
                "productId" to schedule.productId,
                "proposerId" to schedule.proposerId,
                "proposerNickname" to schedule.proposerNickname,
                "place" to schedule.place.name,
                "placeDetail" to schedule.placeDetail,
                "dateText" to schedule.dateText,
                "timeText" to schedule.timeText,
                "memo" to schedule.memo,
                "status" to schedule.status.name,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        schedule
    }

    override suspend fun acceptMeetup(chatId: String): Result<Unit> = runCatching {
        docRef(chatId).update(
            mapOf("status" to MeetupStatus.CONFIRMED.name, "updatedAt" to FieldValue.serverTimestamp())
        ).await()
    }

    override suspend fun cancelMeetup(chatId: String): Result<Unit> = runCatching {
        docRef(chatId).update(
            mapOf("status" to MeetupStatus.CANCELLED.name, "updatedAt" to FieldValue.serverTimestamp())
        ).await()
    }
}
