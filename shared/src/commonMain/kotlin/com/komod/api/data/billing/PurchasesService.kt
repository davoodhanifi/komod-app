package com.komod.api.data.billing

import com.revenuecat.purchases.kmp.LogLevel
import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.configure
import com.revenuecat.purchases.kmp.ktx.SuccessfulPurchase
import com.revenuecat.purchases.kmp.ktx.awaitCustomerInfo
import com.revenuecat.purchases.kmp.ktx.awaitOfferings
import com.revenuecat.purchases.kmp.ktx.awaitPurchase
import com.revenuecat.purchases.kmp.ktx.awaitRestore
import com.revenuecat.purchases.kmp.models.CustomerInfo
import com.revenuecat.purchases.kmp.models.Offerings
import com.revenuecat.purchases.kmp.models.Package

/**
 * Thin wrapper around the RevenueCat Kotlin Multiplatform SDK — the only file in the app that
 * talks to [Purchases] directly. Everything else goes through [com.komod.api.data.repository.BillingRepository],
 * so swapping/upgrading the SDK, or wiring up Google Play later, only touches this layer.
 */
class PurchasesService {

    // Only true once [configure] has run. That currently only happens on iOS (see
    // MainViewController.kt's initKoin()) — Google Play isn't wired up yet, so on Android this
    // stays false and BillingRepository treats billing as unavailable rather than calling an
    // unconfigured SDK.
    val isConfigured: Boolean
        get() = Purchases.isConfigured

    suspend fun getOfferings(): Offerings = Purchases.sharedInstance.awaitOfferings()

    suspend fun purchase(rcPackage: Package): SuccessfulPurchase =
        Purchases.sharedInstance.awaitPurchase(packageToPurchase = rcPackage)

    suspend fun restorePurchases(): CustomerInfo = Purchases.sharedInstance.awaitRestore()

    suspend fun getCustomerInfo(): CustomerInfo = Purchases.sharedInstance.awaitCustomerInfo()

    companion object {
        // Safe to call more than once (e.g. hot reload) — configuring twice is a no-op.
        fun configure(apiKey: String) {
            if (Purchases.isConfigured || apiKey.isBlank()) return
            Purchases.logLevel = LogLevel.DEBUG
            Purchases.configure(apiKey = apiKey)
        }
    }
}
