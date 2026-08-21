package com.komod.api.domain.model

import com.revenuecat.purchases.kmp.models.Package

// One purchasable Komod tier, paired with its monthly and yearly RevenueCat packages — both
// are always present (BillingRepository omits any plan missing either duration rather than
// returning a partial one). [plan] excludes Rack/Unknown, which aren't purchasable tiers.
data class PaywallPlan(
    val plan: SubscriptionPlan,
    val monthlyPackage: Package,
    val yearlyPackage: Package,
)
