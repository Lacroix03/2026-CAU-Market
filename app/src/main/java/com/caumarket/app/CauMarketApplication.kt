package com.caumarket.app

import android.app.Application
import com.caumarket.app.di.AppContainer

/**
 * Application 범위에서 백엔드 AppContainer를 1회 생성합니다.
 * - Android Emulator 기본 호스트: 10.0.2.2
 * - 실제 휴대폰 LAN 시연 시: AppContainer(applicationContext, "개발 PC IPv4 주소")
 */
class CauMarketApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(applicationContext)
    }
}
