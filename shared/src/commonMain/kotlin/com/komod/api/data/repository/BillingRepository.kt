package com.komod.api.data.repository

import com.komod.api.data.billing.PurchasesService
import com.komod.api.data.billing.RevenueCatCatalog
import com.komod.api.data.billing.RevenueCatIdentitySync
import com.komod.api.domain.model.PaywallPlan
import com.revenuecat.purchases.kmp.models.Package

// Thrown instead of calling into an unconfigured Purchases SDK — currently that's any non-iOS
// build, since Google Play isn't wired up yet (see PurchasesService).
class BillingUnavailableException : RuntimeException("Subscriptions aren't available on this device yet.")

interface BillingRepository {
    val isBillingAvailable: Boolean

    // The four purchasable tiers with live, localized RevenueCat pricing. Never hard-coded —
    // sourced from the current RevenueCat offering every time this is called. Waits for
    // RevenueCat's identity to be confirmed ready first, same as purchase()/restorePurchases()
    // below — fetching offerings while a logIn()/logOut() call is still in flight (e.g. right
    // after a Google sign-in, which backgrounds the app for the OAuth redirect, unlike Apple's
    // in-app native flow) is a real source of a transient StoreKit product-fetch failure.
    suspend fun getPaywallPlans(): List<PaywallPlan>

    // Purchases [rcPackage]. Waits for RevenueCat's identity to be confirmed ready for the
    // current Supabase user first (see RevenueCatIdentitySync) — never proceeds against a
    // possibly-still-anonymous or stale identity. Deliberately returns nothing about "what plan
    // the user is on now" — RevenueCat is only the source of truth for the App Store
    // transaction itself; the resulting Komod plan comes from the backend sync that must follow
    // (SubscriptionSyncRepository), never inferred from this call succeeding.
    suspend fun purchase(rcPackage: Package)

    suspend fun restorePurchases()

    // Warms/refreshes RevenueCat's local CustomerInfo cache (e.g. before a backend sync, so
    // the sync reflects the latest transaction state RevenueCat itself has observed). The
    // result is intentionally discarded — nothing in this app derives the displayed plan from
    // it directly; see SubscriptionSyncRepository.
    suspend fun refreshCustomerInfo()

    // The customer's currently active App Store/Play product IDs, straight from RevenueCat's
    // CustomerInfo.activeSubscriptions — RevenueCat's own recommended way to check "is this
    // customer subscribed to product X". Used only to auto-select the matching plan when the
    // Paywall opens; KomodSubscriptionState remains the sole source of truth for what Komod
    // plan/entitlements the user actually has (the "Current" badge, feature gating, etc.).
    // Empty (never throws) when billing is unavailable.
    suspend fun getActiveProductIdentifiers(): Set<String>
}

class BillingRepositoryImpl(
    private val purchasesService: PurchasesService,
    private val revenueCatIdentitySync: RevenueCatIdentitySync,
) : BillingRepository {

    override val isBillingAvailable: Boolean
        get() = purchasesService.isConfigured

    override suspend fun getPaywallPlans(): List<PaywallPlan> {
        if (!isBillingAvailable) throw BillingUnavailableException()
        revenueCatIdentitySync.awaitReadyForCurrentUser()
        val availablePackages = purchasesService.getOfferings().current?.availablePackages.orEmpty()
        return RevenueCatCatalog.plans.mapNotNull { (plan, ids) ->
            val monthly = availablePackages.find { it.identifier == ids.monthlyPackageId }
            val yearly = availablePackages.find { it.identifier == ids.yearlyPackageId }
            // Both durations must be present in the current offering — a plan missing either
            // one is omitted rather than shown with a broken/missing price.
            if (monthly != null && yearly != null) {
                PaywallPlan(plan = plan, monthlyPackage = monthly, yearlyPackage = yearly)
            } else {
                null
            }
        }
    }

    override suspend fun purchase(rcPackage: Package) {
        if (!isBillingAvailable) throw BillingUnavailableException()
        revenueCatIdentitySync.awaitReadyForCurrentUser()
        purchasesService.purchase(rcPackage)
    }

    override suspend fun restorePurchases() {
        if (!isBillingAvailable) throw BillingUnavailableException()
        revenueCatIdentitySync.awaitReadyForCurrentUser()
        purchasesService.restorePurchases()
    }

    override suspend fun refreshCustomerInfo() {
        if (!isBillingAvailable) return
        purchasesService.getCustomerInfo()
    }

    override suspend fun getActiveProductIdentifiers(): Set<String> {
        if (!isBillingAvailable) return emptySet()
        return purchasesService.getCustomerInfo().activeSubscriptions
    }
}
