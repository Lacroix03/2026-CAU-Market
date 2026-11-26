package com.caumarket.app.core.error

enum class MarketErrorCode {
    VALIDATION, AUTH_REQUIRED, INVALID_CREDENTIALS, EMAIL_ALREADY_USED, INCOMPLETE_PROFILE,
    PERMISSION_DENIED, NOT_FOUND, CONFLICT, NETWORK, QUOTA_EXCEEDED, UNKNOWN
}
class MarketException(
    val code: MarketErrorCode,
    val field: String? = null,
    val operationId: String? = null,
    val retryable: Boolean = false
) : Exception(code.name)
