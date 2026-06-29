package com.komod.api

import android.app.Application
import com.komod.api.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class KomodApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@KomodApplication)
            modules(
                appModule(
                    AppConfig(
                        supabaseUrl = BuildConfig.SUPABASE_URL,
                        supabasePublishableKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY,
                    )
                )
            )
        }
    }
}
