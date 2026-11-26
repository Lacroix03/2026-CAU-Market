package com.cau.campusmarket.domain.model

enum class MeetupStatus(val displayName: String) {
    PROPOSED("수락 대기"),
    CONFIRMED("확정"),
    CANCELLED("취소됨");

    companion object {
        fun fromName(name: String): MeetupStatus =
            entries.firstOrNull { it.name == name } ?: PROPOSED
    }
}

/**
 * 2차 확장 기능 — 채팅방 내 거래 일정 협의 (기본안: 교내 장소 목록 + 날짜·시간 직접 선택)
 */
data class MeetupSchedule(
    val scheduleId: String = "",
    val chatId: String = "",
    val productId: String = "",
    val proposerId: String = "",
    val proposerNickname: String = "",
    val place: CampusPlace = CampusPlace.LIBRARY,
    val placeDetail: String = "",
    val dateText: String = "",
    val timeText: String = "",
    val memo: String = "",
    val status: MeetupStatus = MeetupStatus.PROPOSED,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun formattedLocation(): String =
        if (placeDetail.isBlank()) place.displayName
        else "${place.displayName} · $placeDetail"
}
