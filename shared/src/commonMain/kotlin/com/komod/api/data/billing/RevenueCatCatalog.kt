package com.komod.api.data.billing

import com.komod.api.domain.model.SubscriptionPlan

// The RevenueCat package identifiers configured for one purchasable tier — see the RevenueCat
// dashboard offering "default" for the source of truth these must match.
//
// No entitlement identifier here by design: the backend (POST /subscription/sync) is the sole
// source of truth for "what Komod plan is the user on", so mobile has no need to map a
// RevenueCat entitlement to a plan locally — see KomodSubscriptionState.
internal data class RevenueCatPlanIds(
    val monthlyPackageId: String,
    val yearlyPackageId: String,
)

// Maps each purchasable [SubscriptionPlan] to its RevenueCat package identifiers. Rack (free
// tier, nothing to purchase) and Unknown are intentionally absent. Map order drives the order
// plans are shown in on the Paywall.
internal object RevenueCatCatalog {
    val plans: Map<SubscriptionPlan, RevenueCatPlanIds> = linkedMapOf(
        SubscriptionPlan.OneDoor to RevenueCatPlanIds(
            monthlyPackageId = "komod_1door_monthly",
            yearlyPackageId = "komod_1door_yearly",
        ),
        SubscriptionPlan.TwoDoors to RevenueCatPlanIds(
            monthlyPackageId = "komod_2doors_monthly",
            yearlyPackageId = "komod_2doors_yearly",
        ),
        SubscriptionPlan.ThreeDoors to RevenueCatPlanIds(
            monthlyPackageId = "komod_3doors_monthly",
            yearlyPackageId = "komod_3doors_yearly",
        ),
        SubscriptionPlan.WalkIn to RevenueCatPlanIds(
            monthlyPackageId = "komod_walkin_monthly",
            yearlyPackageId = "komod_walkin_yearly",
        ),
    )
}
