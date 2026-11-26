package com.caumarket.app.di

import android.content.Context
import com.caumarket.app.data.firebase.*
import com.caumarket.app.data.repository.*

/** Create once at application scope. No UI/navigation or production initialization occurs here. */
class AppContainer(context: Context, emulatorHost: String = "10.0.2.2") {
 private val client=FirebaseClient(context,emulatorHost)
 val authRepository: AuthRepository=FirebaseAuthRepository(client)
 val productRepository: ProductRepository=FirebaseProductRepository(client)
 val chatRepository: ChatRepository=FirebaseChatRepository(client)
}
