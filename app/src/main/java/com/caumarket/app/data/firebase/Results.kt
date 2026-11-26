package com.caumarket.app.data.firebase

import com.caumarket.app.core.error.*
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException

internal fun marketError(error: Exception, operationId: String? = null): MarketException {
    if (error is MarketException) return MarketException(error.code,error.field,error.operationId?:operationId,error.retryable)
    val code = when(error) {
        is java.io.IOException -> MarketErrorCode.NETWORK
        is FirebaseNetworkException -> MarketErrorCode.NETWORK
        is FirebaseAuthInvalidCredentialsException, is FirebaseAuthInvalidUserException -> MarketErrorCode.INVALID_CREDENTIALS
        is FirebaseAuthUserCollisionException -> MarketErrorCode.EMAIL_ALREADY_USED
        is FirebaseFirestoreException -> when(error.code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> MarketErrorCode.PERMISSION_DENIED
            FirebaseFirestoreException.Code.UNAVAILABLE, FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> MarketErrorCode.NETWORK
            FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED -> MarketErrorCode.QUOTA_EXCEEDED
            else -> MarketErrorCode.UNKNOWN
        }
        else -> MarketErrorCode.UNKNOWN
    }
    return MarketException(code, null, operationId,
        code == MarketErrorCode.NETWORK || code == MarketErrorCode.QUOTA_EXCEEDED)
}
internal suspend fun <T> result(operationId: String? = null, block: suspend () -> T): Result<T> =
    try { Result.success(block()) }
    catch (cancelled: CancellationException) { throw cancelled }
    catch (error: Exception) { Result.failure(marketError(error, operationId)) }
