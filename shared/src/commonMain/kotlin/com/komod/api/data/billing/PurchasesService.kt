package com.komod.api.data.billing

import com.revenuecat.purchases.kmp.LogLevel
import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.configure
import com.revenuecat.purchases.kmp.ktx.SuccessfulLogin
import com.revenuecat.purchases.kmp.ktx.SuccessfulPurchase
import com.revenuecat.purchases.kmp.ktx.awaitCustomerInfo
import com.revenuecat.purchases.kmp.ktx.awaitLogIn
import com.revenuecat.purchases.kmp.ktx.awaitLogOut
import com.revenuecat.purchases.kmp.ktx.awaitOfferings
import com.revenuecat.purchases.kmp.ktx.awaitPurchase
import com.revenuecat.purchases.kmp.ktx.awaitRestore
import com.revenuecat.purchases.kmp.models.CustomerInfo
import com.revenuecat.purchases.kmp.models.Offerings
import com.revenuecat.purchases.kmp.models.Package

/**
 * Thin wrapper around the RevenueCat Kotlin Multiplatform SDK — an interface (rather than a
 * plain class) purely so [RevenueCatIdentitySync] and [com.komod.api.data.repository.BillingRepository]
 * can be unit-tested against a fake instead of the real [Purchases] singleton.
 * [RevenueCatPurchasesService] is the only class in the app that talks to [Purchases] directly.
 */
interface PurchasesService {
    // Only true once [RevenueCatPurchasesService.configure] has run. That currently only
    // happens on iOS (see MainViewController.kt's initKoin()) — Google Play isn't wired up yet,
    // so on Android this stays false and BillingRepository treats billing as unavailable rather
    // than calling an unconfigured SDK.
    val isConfigured: Boolean

    suspend fun getOfferings(): Offerings

    suspend fun purchase(rcPackage: Package): SuccessfulPurchase

    suspend fun restorePurchases(): CustomerInfo

    suspend fun getCustomerInfo(): CustomerInfo

    // Aliases/identifies RevenueCat's appUserID as [supabaseUserId]. RevenueCat's own SDK owns
    // the anonymous-to-identified migration (and the "already an existing customer" merge) —
    // this is a direct passthrough, no custom migration logic on our side.
    suspend fun logIn(supabaseUserId: String): SuccessfulLogin

    // RevenueCat's documented reset: clears the identified appUserID and generates a fresh
    // anonymous one, so a subsequent logIn() for a different user never inherits this state.
    suspend fun logOut(): CustomerInfo
}

class RevenueCatPurchasesService : PurchasesService {
    override val isConfigured: Boolean
        get() = Purchases.isConfigured

    override suspend fun getOfferings(): Offerings = Purchases.sharedInstance.awaitOfferings()

    override suspend fun purchase(rcPackage: Package): SuccessfulPurchase =
        Purchases.sharedInstance.awaitPurchase(packageToPurchase = rcPackage)

    override suspend fun restorePurchases(): CustomerInfo = Purchases.sharedInstance.awaitRestore()

    override suspend fun getCustomerInfo(): CustomerInfo = Purchases.sharedInstance.awaitCustomerInfo()

    override suspend fun logIn(supabaseUserId: String): SuccessfulLogin =
        Purchases.sharedInstance.awaitLogIn(newAppUserID = supabaseUserId)

    override suspend fun logOut(): CustomerInfo = Purchases.sharedInstance.awaitLogOut()

    companion object {
        // Safe to call more than once (e.g. hot reload) — configuring twice is a no-op.
        fun configure(apiKey: String) {
            if (Purchases.isConfigured || apiKey.isBlank()) return
            Purchases.logLevel = LogLevel.DEBUG
            Purchases.configure(apiKey = apiKey)
        }
    }
}
