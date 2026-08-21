package com.komod.api.data.billing

import com.komod.api.domain.model.SubscriptionPlan

// The RevenueCat package and entitlement identifiers configured for one purchasable tier —
// see the RevenueCat dashboard offering "default" for the source of truth these must match.
internal data class RevenueCatPlanIds(
    val monthlyPackageId: String,
    val yearlyPackageId: String,
    val entitlementId: String,
)

// Maps each purchasable [SubscriptionPlan] to its RevenueCat package/entitlement identifiers.
// Rack (free tier, nothing to purchase) and Unknown are intentionally absent. Map order drives
// the order plans are shown in on the Paywall.
internal object RevenueCatCatalog {
    val plans: Map<SubscriptionPlan, RevenueCatPlanIds> = linkedMapOf(
        SubscriptionPlan.OneDoor to RevenueCatPlanIds(
            monthlyPackageId = "komod_1door_monthly",
            yearlyPackageId = "komod_1door_yearly",
            entitlementId = "komod_1door",
        ),
        SubscriptionPlan.TwoDoors to RevenueCatPlanIds(
            monthlyPackageId = "komod_2doors_monthly",
            yearlyPackageId = "komod_2doors_yearly",
            entitlementId = "komod_2doors",
        ),
        SubscriptionPlan.ThreeDoors to RevenueCatPlanIds(
            monthlyPackageId = "komod_3doors_monthly",
            yearlyPackageId = "komod_3doors_yearly",
            entitlementId = "komod_3doors",
        ),
        SubscriptionPlan.WalkIn to RevenueCatPlanIds(
            monthlyPackageId = "komod_walkin_monthly",
            yearlyPackageId = "komod_walkin_yearly",
            entitlementId = "komod_walkin",
        ),
    )
}
