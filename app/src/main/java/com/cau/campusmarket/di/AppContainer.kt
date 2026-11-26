package com.cau.campusmarket.di

import com.cau.campusmarket.data.repository.AuthRepository
import com.cau.campusmarket.data.repository.ChatRepository
import com.cau.campusmarket.data.repository.MeetupRepository
import com.cau.campusmarket.data.repository.ProductRepository
import com.cau.campusmarket.data.repository.demo.DemoAuthRepository
import com.cau.campusmarket.data.repository.demo.DemoChatRepository
import com.cau.campusmarket.data.repository.demo.DemoDataSource
import com.cau.campusmarket.data.repository.demo.DemoMeetupRepository
import com.cau.campusmarket.data.repository.demo.DemoProductRepository
import com.cau.campusmarket.data.repository.firebase.FirebaseAuthRepository
import com.cau.campusmarket.data.repository.firebase.FirebaseChatRepository
import com.cau.campusmarket.data.repository.firebase.FirebaseMeetupRepository
import com.cau.campusmarket.data.repository.firebase.FirebaseProductRepository

object AppConfig {
    /**
     * false: 로컬 데모 모드 (에뮬레이터 즉시 실행용)
     * true : Firebase 실연동 모드 (google-services.json 추가 후 전환)
     */
    const val USE_FIREBASE = false
}

class AppContainer {
    private val demoDataSource by lazy { DemoDataSource() }

    val authRepository: AuthRepository by lazy {
        if (AppConfig.USE_FIREBASE) FirebaseAuthRepository()
        else DemoAuthRepository(demoDataSource)
    }

    val productRepository: ProductRepository by lazy {
        if (AppConfig.USE_FIREBASE) FirebaseProductRepository(authRepository)
        else DemoProductRepository(demoDataSource)
    }

    val chatRepository: ChatRepository by lazy {
        if (AppConfig.USE_FIREBASE) FirebaseChatRepository(authRepository, productRepository)
        else DemoChatRepository(demoDataSource)
    }

    val meetupRepository: MeetupRepository by lazy {
        if (AppConfig.USE_FIREBASE) FirebaseMeetupRepository(authRepository)
        else DemoMeetupRepository(demoDataSource)
    }
}
