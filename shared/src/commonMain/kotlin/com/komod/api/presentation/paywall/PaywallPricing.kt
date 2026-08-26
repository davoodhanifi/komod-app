package com.komod.api.presentation.paywall

import com.komod.api.domain.model.PaywallPlan
import com.komod.api.domain.model.SubscriptionPlan
import kotlin.math.roundToInt

// The plan highlighted with the Paywall's "Recommended" badge — a presentation-only nudge with
// no bearing on selection/purchase logic. Change this single value to move the badge.
internal val RecommendedPlan: SubscriptionPlan = SubscriptionPlan.TwoDoors

// A pure function of [plan] alone — deliberately takes no selectedPlan/currentPlan, so
// "recommended" can never be conflated with "selected" or "current" in the UI.
internal fun isRecommendedPlan(plan: SubscriptionPlan): Boolean = plan == RecommendedPlan

// discount = 1 - (yearlyPrice / (monthlyPrice * 12)), rounded to the nearest percent. Returns
// null (hide the badge, never show a broken or nonsensical one) when either price is missing,
// or when the math doesn't actually work out to a positive saving.
internal fun yearlyDiscountPercent(monthlyPriceMicros: Long, yearlyPriceMicros: Long): Int? {
    if (monthlyPriceMicros <= 0 || yearlyPriceMicros <= 0) return null
    val discount = 1.0 - (yearlyPriceMicros.toDouble() / (monthlyPriceMicros.toDouble() * 12))
    return (discount * 100).roundToInt().takeIf { it > 0 }
}

// Convenience overload for the plan currently selected on the Paywall — always live RevenueCat
// StoreProduct prices, never hard-coded.
internal fun yearlyDiscountPercent(plan: PaywallPlan): Int? = yearlyDiscountPercent(
    monthlyPriceMicros = plan.monthlyPackage.storeProduct.price.amountMicros,
    yearlyPriceMicros = plan.yearlyPackage.storeProduct.price.amountMicros,
)
