package com.komod.api

import android.app.Application
import com.komod.api.di.appModule
import com.komod.api.platform.AndroidAppContextHolder
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class KomodApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidAppContextHolder.context = this
        startKoin {
            androidContext(this@KomodApplication)
            modules(appModule())
        }
    }
}
