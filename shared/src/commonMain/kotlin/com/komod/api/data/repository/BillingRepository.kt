package com.komod.api.data.repository

import com.komod.api.data.billing.PurchasesService
import com.komod.api.data.billing.RevenueCatCatalog
import com.komod.api.domain.model.PaywallPlan
import com.komod.api.domain.model.SubscriptionPlan
import com.revenuecat.purchases.kmp.models.CustomerInfo
import com.revenuecat.purchases.kmp.models.Package

// Thrown instead of calling into an unconfigured Purchases SDK — currently that's any non-iOS
// build, since Google Play isn't wired up yet (see PurchasesService).
class BillingUnavailableException : RuntimeException("Subscriptions aren't available on this device yet.")

interface BillingRepository {
    val isBillingAvailable: Boolean

    // The four purchasable tiers with live, localized RevenueCat pricing. Never hard-coded —
    // sourced from the current RevenueCat offering every time this is called.
    suspend fun getPaywallPlans(): List<PaywallPlan>

    // Purchases [rcPackage] and returns the plan now active per RevenueCat's CustomerInfo — the
    // source of truth for entitlement state, never assumed from the purchase call succeeding.
    suspend fun purchase(rcPackage: Package): SubscriptionPlan?

    suspend fun restorePurchases(): SubscriptionPlan?

    // Null if nothing is active (or billing isn't available on this platform) — never throws
    // for that case, since callers use this for best-effort background refreshes.
    suspend fun getActivePlan(): SubscriptionPlan?
}

class BillingRepositoryImpl(
    private val purchasesService: PurchasesService,
) : BillingRepository {

    override val isBillingAvailable: Boolean
        get() = purchasesService.isConfigured

    override suspend fun getPaywallPlans(): List<PaywallPlan> {
        if (!isBillingAvailable) throw BillingUnavailableException()
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

    override suspend fun purchase(rcPackage: Package): SubscriptionPlan? {
        if (!isBillingAvailable) throw BillingUnavailableException()
        return purchasesService.purchase(rcPackage).customerInfo.activePlan()
    }

    override suspend fun restorePurchases(): SubscriptionPlan? {
        if (!isBillingAvailable) throw BillingUnavailableException()
        return purchasesService.restorePurchases().activePlan()
    }

    override suspend fun getActivePlan(): SubscriptionPlan? {
        if (!isBillingAvailable) return null
        return purchasesService.getCustomerInfo().activePlan()
    }
}

// The single place RevenueCat's CustomerInfo is turned into our SubscriptionPlan enum. This is
// the source of truth for "what is the user entitled to right now" — always read from here,
// never assumed locally just because a purchase call returned successfully.
private fun CustomerInfo.activePlan(): SubscriptionPlan? =
    RevenueCatCatalog.plans.entries.firstOrNull { (_, ids) -> entitlements[ids.entitlementId]?.isActive == true }?.key
