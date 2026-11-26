package com.cau.campusmarket

import android.app.Application
import com.cau.campusmarket.di.AppContainer

class CauMarketApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer()
    }
}
