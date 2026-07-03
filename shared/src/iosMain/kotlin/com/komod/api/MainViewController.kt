package com.komod.api

import androidx.compose.ui.window.ComposeUIViewController
import com.komod.api.data.repository.AuthRepository
import com.komod.api.di.appModule
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatform

fun initKoin() {
    startKoin {
        modules(appModule())
    }
}

fun MainViewController() = ComposeUIViewController { App() }

fun handleDeepLink(url: String) {
    MainScope().launch {
        KoinPlatform.getKoin().get<AuthRepository>().handleOAuthCallback(url)
    }
}