package com.cau.campusmarket.domain.model

/**
 * GitHub README 6. 데이터 구조 — 열거형 규격 (대문자 영문 저장 후 화면에서 한글 변환)
 */
enum class TradeStatus(val displayName: String) {
    SELLING("판매 중"),
    RESERVED("예약 중"),
    SOLD("거래 완료");

    companion object {
        fun fromName(name: String): TradeStatus =
            entries.firstOrNull { it.name == name || it.displayName == name } ?: SELLING
    }
}

enum class ProductCategory(val displayName: String) {
    TEXTBOOK("교재"),
    LIVING("생활용품"),
    ELECTRONICS("전자기기"),
    CLOTHING("의류"),
    ETC("기타");

    companion object {
        fun fromName(name: String): ProductCategory =
            entries.firstOrNull { it.name == name || it.displayName == name } ?: ETC
    }
}

enum class ProductCondition(val displayName: String) {
    NEW("새 상품"),
    GOOD("사용감 적음"),
    USED("사용감 많음"),
    DEFECTIVE("하자 있음");

    companion object {
        fun fromName(name: String): ProductCondition =
            entries.firstOrNull { it.name == name || it.displayName == name } ?: GOOD
    }
}

enum class CampusPlace(val displayName: String) {
    MAIN_GATE("정문"),
    BACK_GATE("후문"),
    LIBRARY("중앙도서관"),
    STUDENT_HALL("학생회관"),
    DORMITORY("기숙사"),
    ETC("기타");

    companion object {
        fun fromName(name: String): CampusPlace =
            entries.firstOrNull { it.name == name || it.displayName == name } ?: ETC
    }
}

/**
 * GitHub README 6. 데이터 구조 — products/{productId}
 */
data class Product(
    val productId: String = "",
    val sellerId: String = "",
    val sellerNickname: String = "",
    val title: String = "",
    val description: String? = null,
    val price: Int = 0, // 0 = 무료 나눔 ('나눔'), 0 ~ 10,000,000원
    val category: ProductCategory = ProductCategory.ETC,
    val condition: ProductCondition = ProductCondition.GOOD,
    val imageUrls: List<String> = emptyList(), // 1~15개, 0번 대표 사진
    val place: CampusPlace = CampusPlace.MAIN_GATE,
    val placeDetail: String? = null, // 선택, 최대 30자
    val disposeBy: Long? = null, // 선택, 처분 희망 기한 epoch millis
    val status: TradeStatus = TradeStatus.SELLING,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isFreeShare: Boolean get() = price == 0
    val thumbnailUrl: String? get() = imageUrls.firstOrNull()

    fun formattedPlace(): String =
        if (placeDetail.isNullOrBlank()) place.displayName
        else "${place.displayName} · $placeDetail"
}

/**
 * 물품 등록·수정 시 Repository(createProduct / updateProduct)로 전달하는 입력 객체
 */
data class ProductInput(
    val title: String,
    val description: String?,
    val price: Int,
    val category: ProductCategory,
    val condition: ProductCondition,
    val place: CampusPlace,
    val placeDetail: String?,
    val disposeBy: Long? = null,
    val status: TradeStatus = TradeStatus.SELLING
)
