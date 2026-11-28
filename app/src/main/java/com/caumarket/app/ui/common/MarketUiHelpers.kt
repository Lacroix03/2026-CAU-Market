package com.caumarket.app.ui.common

import com.caumarket.app.core.error.MarketErrorCode
import com.caumarket.app.core.error.MarketException
import com.google.firebase.Timestamp
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class CodeOption(val code: String, val label: String)

object MarketOptions {
    val statuses = listOf(
        CodeOption("SELLING", "판매 중"),
        CodeOption("RESERVED", "예약 중"),
        CodeOption("SOLD", "거래 완료")
    )

    val categories = listOf(
        CodeOption("TEXTBOOK", "교재"),
        CodeOption("LIVING", "생활용품"),
        CodeOption("ELECTRONICS", "전자기기"),
        CodeOption("CLOTHING", "의류"),
        CodeOption("ETC", "기타")
    )

    val conditions = listOf(
        CodeOption("NEW", "새 상품"),
        CodeOption("GOOD", "사용감 적음"),
        CodeOption("USED", "사용감 많음"),
        CodeOption("DEFECTIVE", "하자 있음")
    )

    val places = listOf(
        CodeOption("MAIN_GATE", "정문"),
        CodeOption("BACK_GATE", "후문"),
        CodeOption("LIBRARY", "중앙도서관"),
        CodeOption("STUDENT_HALL", "학생회관"),
        CodeOption("DORMITORY", "기숙사"),
        CodeOption("ETC", "기타")
    )

    fun statusLabel(code: String): String = statuses.firstOrNull { it.code == code }?.label ?: code
    fun categoryLabel(code: String): String = categories.firstOrNull { it.code == code }?.label ?: code
    fun conditionLabel(code: String): String = conditions.firstOrNull { it.code == code }?.label ?: code
    fun placeLabel(code: String): String = places.firstOrNull { it.code == code }?.label ?: code

    fun formatPlace(place: String, placeDetail: String?): String {
        val base = placeLabel(place)
        return if (placeDetail.isNullOrBlank()) base else "$base · $placeDetail"
    }
}

object MarketErrorMapper {
    fun toMessage(error: Throwable, fallback: String = "요청을 처리하지 못했어요. 다시 시도해 주세요."): String {
        val me = error as? MarketException ?: return error.message?.takeIf { it.isNotBlank() } ?: fallback
        return when (me.code) {
            MarketErrorCode.VALIDATION -> when (me.field) {
                "email" -> "cau.ac.kr 학교 이메일 형식으로 입력해 주세요."
                "nickname" -> "닉네임은 2~10자로 입력해 주세요."
                "password" -> "비밀번호는 8자 이상 입력해 주세요."
                "passwordConfirmation" -> "비밀번호가 일치하지 않습니다."
                "title" -> "물품명은 1~50자로 입력해 주세요."
                "description" -> "상세 설명은 최대 1,000자까지 입력할 수 있어요."
                "price" -> "가격은 0원 ~ 10,000,000원 사이로 입력해 주세요."
                "images", "imageUris", "photos" -> "사진은 1~15장(채팅은 최대 10장, 장당 2MB 이하)으로 선택해 주세요."
                "placeDetail" -> "상세 위치는 최대 30자까지 입력할 수 있어요."
                "disposeBy" -> "처분 희망 기한은 내일 이후 날짜(YYYY-MM-DD)로 입력해 주세요."
                else -> "입력값을 다시 확인해 주세요."
            }
            MarketErrorCode.AUTH_REQUIRED -> "로그인이 필요합니다."
            MarketErrorCode.INVALID_CREDENTIALS -> "이메일 또는 비밀번호가 일치하지 않습니다."
            MarketErrorCode.EMAIL_ALREADY_USED -> "이미 가입된 학교 이메일입니다."
            MarketErrorCode.INCOMPLETE_PROFILE -> "프로필 등록이 완료되지 않았습니다. 닉네임을 입력해 가입을 마무리해 주세요."
            MarketErrorCode.PERMISSION_DENIED -> "접근 권한이 없습니다."
            MarketErrorCode.NOT_FOUND -> "대상을 찾을 수 없습니다."
            MarketErrorCode.CONFLICT -> "이미 변경되었거나 처리할 수 없는 상태입니다. 새로고침 후 다시 시도해 주세요."
            MarketErrorCode.NETWORK -> "서버에 연결할 수 없어요. 로컬 에뮬레이터 서버가 켜져 있는지 확인해 주세요."
            MarketErrorCode.QUOTA_EXCEEDED -> "요청 한도를 초과했습니다. 잠시 후 다시 시도해 주세요."
            MarketErrorCode.UNKNOWN -> fallback
        }
    }
}

fun formatPrice(price: Int): String =
    if (price == 0) "나눔"
    else "${NumberFormat.getNumberInstance(Locale.KOREA).format(price)}원"

fun formatSmartDate(timestamp: Timestamp?): String {
    if (timestamp == null) return ""
    val millis = timestamp.toDate().time
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply { timeInMillis = millis }

    val sameYear = now.get(Calendar.YEAR) == target.get(Calendar.YEAR)
    val sameDay = sameYear && now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val isYesterday = yesterday.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
        yesterday.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)

    return when {
        sameDay -> SimpleDateFormat("a h:mm", Locale.KOREA).format(Date(millis))
        isYesterday -> "어제"
        sameYear -> SimpleDateFormat("M월 d일", Locale.KOREA).format(Date(millis))
        else -> SimpleDateFormat("yyyy.MM.dd", Locale.KOREA).format(Date(millis))
    }
}

fun formatTimeOnly(timestamp: Timestamp): String =
    SimpleDateFormat("a h:mm", Locale.KOREA).format(timestamp.toDate())

fun formatDateDivider(timestamp: Timestamp): String =
    SimpleDateFormat("yyyy년 M월 d일 EEEE", Locale.KOREA).format(timestamp.toDate())

fun formatDisposeDate(timestamp: Timestamp?): String? =
    timestamp?.let { SimpleDateFormat("MM/dd까지", Locale.KOREA).format(it.toDate()) }

fun formatDisposeInput(timestamp: Timestamp?): String =
    timestamp?.let { SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(it.toDate()) }.orEmpty()

/**
 * 백엔드 규격: 처분 희망 기한은 Asia/Seoul 기준 선택 날짜 00:00:00 KST Timestamp
 */
fun parseSeoulDateToTimestamp(yyyyMmDd: String): Timestamp? {
    val trimmed = yyyyMmDd.trim()
    if (trimmed.isEmpty()) return null
    val localDate = LocalDate.parse(trimmed)
    val instant = localDate.atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant()
    return Timestamp(Date.from(instant))
}

fun formatUnreadBadge(count: Int): String? = when {
    count <= 0 -> null
    count >= 100 -> "99+"
    else -> count.toString()
}
