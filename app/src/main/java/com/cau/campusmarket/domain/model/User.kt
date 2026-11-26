package com.cau.campusmarket.domain.model

/**
 * GitHub README 6. 데이터 구조 — users/{userId}
 */
data class User(
    val userId: String = "",
    val email: String = "",
    val nickname: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
