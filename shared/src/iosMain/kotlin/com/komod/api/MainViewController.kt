package com.komod.api

import androidx.compose.ui.window.ComposeUIViewController
import com.komod.api.data.billing.RevenueCatIdentitySync
import com.komod.api.data.billing.RevenueCatPurchasesService
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
    // iOS-only for now — RevenueCat isn't configured on Android since Google Play isn't
    // wired up yet. BillingRepository treats an unconfigured SDK as "billing unavailable"
    // rather than crashing, so this is the only platform gate needed.
    RevenueCatPurchasesService.configure(apiKey = BuildKonfig.REVENUECAT_IOS_API_KEY)
    // Must come after configure() above: this starts observing AuthRepository.currentUser
    // and calls Purchases logIn()/logOut() (via BillingRepository) to keep RevenueCat's
    // appUserID aligned with the authenticated Supabase user ID — see RevenueCatIdentitySync's
    // doc comment for the full identity contract with the backend.
    KoinPlatform.getKoin().get<RevenueCatIdentitySync>()
}

fun MainViewController() = ComposeUIViewController { App() }

fun handleDeepLink(url: String) {
    MainScope().launch {
        KoinPlatform.getKoin().get<AuthRepository>().handleOAuthCallback(url)
    }
}