package com.cau.campusmarket.data.repository.demo

import com.cau.campusmarket.domain.model.CampusPlace
import com.cau.campusmarket.domain.model.ChatRoom
import com.cau.campusmarket.domain.model.MeetupSchedule
import com.cau.campusmarket.domain.model.MeetupStatus
import com.cau.campusmarket.domain.model.Message
import com.cau.campusmarket.domain.model.MessageType
import com.cau.campusmarket.domain.model.Product
import com.cau.campusmarket.domain.model.ProductCategory
import com.cau.campusmarket.domain.model.ProductCondition
import com.cau.campusmarket.domain.model.TradeStatus
import com.cau.campusmarket.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow

class DemoDataSource {
    private val now = System.currentTimeMillis()
    private val minute = 60_000L
    private val hour = 60 * minute
    private val day = 24 * hour

    val credentials = mutableMapOf(
        "student@cau.ac.kr" to "password123",
        "freshman@cau.ac.kr" to "password123",
        "physics@cau.ac.kr" to "password123"
    )

    val users = mutableMapOf(
        "student@cau.ac.kr" to User(
            userId = "user_me",
            email = "student@cau.ac.kr",
            nickname = "새내기22",
            createdAt = now - 30 * day
        ),
        "freshman@cau.ac.kr" to User(
            userId = "user_minji",
            email = "freshman@cau.ac.kr",
            nickname = "물리왕",
            createdAt = now - 25 * day
        ),
        "physics@cau.ac.kr" to User(
            userId = "user_junho",
            email = "physics@cau.ac.kr",
            nickname = "컴공23",
            createdAt = now - 20 * day
        )
    )

    val currentUserFlow = MutableStateFlow<User?>(null)

    val productsFlow = MutableStateFlow(
        listOf(
            Product(
                productId = "prod_1",
                sellerId = "user_minji",
                sellerNickname = "물리왕",
                title = "일반물리학 10판 (필기 거의 없음)",
                description = "한 학기 사용했고 필기는 앞부분 몇 장만 있어요. 표지 모서리 약간 접힘 외 하자 없습니다.",
                price = 15000,
                category = ProductCategory.TEXTBOOK,
                condition = ProductCondition.GOOD,
                imageUrls = listOf(
                    "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=800",
                    "https://images.unsplash.com/photo-1512820790803-83ca734da794?w=800"
                ),
                place = CampusPlace.LIBRARY,
                placeDetail = "정문 쪽 벤치",
                disposeBy = now + 20 * day,
                status = TradeStatus.SELLING,
                createdAt = now - 2 * hour,
                updatedAt = now - 2 * hour
            ),
            Product(
                productId = "prod_2",
                sellerId = "user_junho",
                sellerNickname = "컴공23",
                title = "기숙사용 미니 냉장고",
                description = "블루미르홀 기숙사에서 1년 사용했습니다. 소음 적고 냉장 성능 좋습니다.",
                price = 40000,
                category = ProductCategory.LIVING,
                condition = ProductCondition.GOOD,
                imageUrls = listOf(
                    "https://images.unsplash.com/photo-1584568694244-14fbdf83bd30?w=800"
                ),
                place = CampusPlace.DORMITORY,
                placeDetail = "308관 1층 로비",
                disposeBy = now + 15 * day,
                status = TradeStatus.RESERVED,
                createdAt = now - 5 * hour,
                updatedAt = now - 3 * hour
            ),
            Product(
                productId = "prod_3",
                sellerId = "user_minji",
                sellerNickname = "물리왕",
                title = "아이패드 9세대 64GB",
                description = "강의 필기용으로 사용했습니다. 보호필름 부착되어 있고 충전기 포함입니다.",
                price = 250000,
                category = ProductCategory.ELECTRONICS,
                condition = ProductCondition.GOOD,
                imageUrls = listOf(
                    "https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?w=800"
                ),
                place = CampusPlace.MAIN_GATE,
                placeDetail = null,
                disposeBy = null,
                status = TradeStatus.SELLING,
                createdAt = now - 10 * hour,
                updatedAt = now - 10 * hour
            ),
            Product(
                productId = "prod_4",
                sellerId = "user_me",
                sellerNickname = "새내기22",
                title = "LED 책상 스탠드",
                description = "이사 정리하면서 무료 나눔합니다. 밝기 3단계 조절 정상 작동합니다.",
                price = 0,
                category = ProductCategory.LIVING,
                condition = ProductCondition.GOOD,
                imageUrls = listOf(
                    "https://images.unsplash.com/photo-1507473885765-e6ed057f782c?w=800"
                ),
                place = CampusPlace.DORMITORY,
                placeDetail = "기숙사 앞",
                disposeBy = now + 12 * day,
                status = TradeStatus.SELLING,
                createdAt = now - 1 * day,
                updatedAt = now - 1 * day
            ),
            Product(
                productId = "prod_5",
                sellerId = "user_me",
                sellerNickname = "새내기22",
                title = "공학수학 솔루션",
                description = "깨끗하게 사용한 전공 교재입니다.",
                price = 8000,
                category = ProductCategory.TEXTBOOK,
                condition = ProductCondition.USED,
                imageUrls = listOf(
                    "https://images.unsplash.com/photo-1532012197267-da84d127e765?w=800"
                ),
                place = CampusPlace.STUDENT_HALL,
                placeDetail = "1층 로비",
                disposeBy = null,
                status = TradeStatus.RESERVED,
                createdAt = now - 2 * day,
                updatedAt = now - 1 * day
            ),
            Product(
                productId = "prod_6",
                sellerId = "user_me",
                sellerNickname = "새내기22",
                title = "경영학원론 전공책",
                description = "거래 완료된 전공책 예시입니다. 검색 시 거래 완료 포함으로 확인할 수 있습니다.",
                price = 9000,
                category = ProductCategory.TEXTBOOK,
                condition = ProductCondition.NEW,
                imageUrls = listOf(
                    "https://images.unsplash.com/photo-1495446815901-a7297e633e8d?w=800"
                ),
                place = CampusPlace.BACK_GATE,
                placeDetail = null,
                disposeBy = null,
                status = TradeStatus.SOLD,
                createdAt = now - 3 * day,
                updatedAt = now - 2 * day
            )
        )
    )

    val chatRoomsFlow = MutableStateFlow(
        listOf(
            ChatRoom(
                chatId = "chat_1",
                productId = "prod_1",
                sellerId = "user_minji",
                buyerId = "user_me",
                activeUserIds = listOf("user_me", "user_minji"),
                lastMessage = "네 판매 중이에요 :)",
                lastMessageAt = now - 15 * minute,
                unreadCount = mapOf("user_me" to 1, "user_minji" to 0),
                lastReadAt = mapOf("user_minji" to (now - 15 * minute), "user_me" to (now - 40 * minute)),
                createdAt = now - 2 * hour,
                sellerNickname = "물리왕",
                buyerNickname = "새내기22",
                productTitle = "일반물리학 10판 (필기 거의 없음)",
                productPrice = 15000,
                productThumbnailUrl = "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=800",
                productStatus = TradeStatus.SELLING
            ),
            ChatRoom(
                chatId = "chat_2",
                productId = "prod_2",
                sellerId = "user_junho",
                buyerId = "user_me",
                activeUserIds = listOf("user_me", "user_junho"),
                lastMessage = "아직 예약 중인가요?",
                lastMessageAt = now - day,
                unreadCount = mapOf("user_me" to 0, "user_junho" to 1),
                lastReadAt = mapOf("user_me" to (now - day)),
                createdAt = now - 2 * day,
                sellerNickname = "컴공23",
                buyerNickname = "새내기22",
                productTitle = "기숙사용 미니 냉장고",
                productPrice = 40000,
                productThumbnailUrl = "https://images.unsplash.com/photo-1584568694244-14fbdf83bd30?w=800",
                productStatus = TradeStatus.RESERVED
            )
        )
    )

    val messagesByChatFlow = MutableStateFlow(
        mapOf(
            "chat_1" to listOf(
                Message(
                    messageId = "msg_1",
                    senderId = "user_me",
                    type = MessageType.TEXT,
                    text = "안녕하세요! 물리학 책 아직 판매 중인가요?",
                    createdAt = now - 45 * minute
                ),
                Message(
                    messageId = "msg_2",
                    senderId = "user_minji",
                    type = MessageType.TEXT,
                    text = "네 판매 중이에요 :)",
                    createdAt = now - 15 * minute
                )
            ),
            "chat_2" to listOf(
                Message(
                    messageId = "msg_3",
                    senderId = "user_me",
                    type = MessageType.TEXT,
                    text = "아직 예약 중인가요?",
                    createdAt = now - day
                )
            )
        )
    )

    val meetupsByChatFlow = MutableStateFlow(
        mapOf(
            "chat_1" to MeetupSchedule(
                scheduleId = "meetup_1",
                chatId = "chat_1",
                productId = "prod_1",
                proposerId = "user_minji",
                proposerNickname = "물리왕",
                place = CampusPlace.LIBRARY,
                placeDetail = "정문 쪽 벤치",
                dateText = "12월 10일 (목)",
                timeText = "오후 3:00",
                memo = "도착하시면 채팅 주세요!",
                status = MeetupStatus.PROPOSED,
                updatedAt = now - 10 * minute
            )
        )
    )
}
