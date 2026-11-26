package com.caumarket.app.data.repository
import com.caumarket.app.core.model.User

interface AuthRepository {
    suspend fun signUp(email: String, password: String, nickname: String): Result<Unit>
    suspend fun login(email: String, password: String): Result<User>
    suspend fun logout(): Result<Unit>
    suspend fun getCurrentUser(): Result<User?>
    suspend fun completeSignUp(nickname: String): Result<Unit>
}
