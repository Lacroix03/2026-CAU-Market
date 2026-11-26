package com.caumarket.app.data.firebase

import com.caumarket.app.core.error.*
import com.caumarket.app.core.model.User
import com.caumarket.app.core.validation.Validation
import com.caumarket.app.data.repository.AuthRepository
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepository(private val client: FirebaseClient) : AuthRepository {
    private suspend fun user(): User? {
        val account = client.auth.currentUser ?: return null
        val snapshot = client.firestore.collection("users").document(account.uid).get(Source.SERVER).await()
        if (!snapshot.exists()) throw MarketException(MarketErrorCode.INCOMPLETE_PROFILE)
        return User(snapshot.getString("userId") ?: error("Invalid profile"),
            snapshot.getString("email") ?: error("Invalid profile"),
            snapshot.getString("nickname") ?: error("Invalid profile"),
            snapshot.getTimestamp("createdAt") ?: error("Invalid profile"))
    }
    override suspend fun signUp(email: String, password: String, nickname: String): Result<Unit> = result {
        val normalizedEmail = Validation.email(email)
        val normalizedNickname = Validation.nickname(nickname)
        Validation.password(password)
        client.auth.createUserWithEmailAndPassword(normalizedEmail,password).await()
        try {
            client.call("completeSignUp",mapOf("nickname" to normalizedNickname))
            client.auth.signOut()
        } catch (cancelled: CancellationException) { throw cancelled
        } catch (error: Exception) {
            val exists = try { user() != null } catch (cancelled: CancellationException) { throw cancelled
            } catch (_: Exception) { false }
            if (exists) client.auth.signOut()
            else {
                try { client.auth.currentUser?.delete()?.await() }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { throw MarketException(MarketErrorCode.INCOMPLETE_PROFILE) }
                throw error
            }
        }
    }
    override suspend fun login(email: String, password: String): Result<User> = result {
        if (password.isEmpty()) throw MarketException(MarketErrorCode.VALIDATION,"password")
        client.auth.signInWithEmailAndPassword(Validation.email(email),password).await()
        user() ?: throw MarketException(MarketErrorCode.AUTH_REQUIRED)
    }
    override suspend fun logout(): Result<Unit> = result {
        val uid=client.auth.currentUser?.uid
        client.auth.signOut()
        if(uid!=null) client.pending.clear(uid)
    }
    override suspend fun getCurrentUser(): Result<User?> = result { user() }
    override suspend fun completeSignUp(nickname: String): Result<Unit> = result {
        if (client.auth.currentUser == null) throw MarketException(MarketErrorCode.AUTH_REQUIRED)
        client.call("completeSignUp",mapOf("nickname" to Validation.nickname(nickname)))
        client.auth.signOut()
    }
}
