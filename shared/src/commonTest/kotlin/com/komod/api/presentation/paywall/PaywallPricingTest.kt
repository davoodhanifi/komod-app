package com.komod.api.presentation.paywall

import com.komod.api.domain.model.SubscriptionPlan
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PaywallPricingTest {

    @Test
    fun `yearly discount is calculated from live monthly and yearly prices`() {
        // $9.99/month * 12 = $119.88/year; a $95.90 yearly price is a 20% saving.
        val percent = yearlyDiscountPercent(monthlyPriceMicros = 9_990_000, yearlyPriceMicros = 95_904_000)
        assertEquals(20, percent)
    }

    @Test
    fun `yearly discount rounds to the nearest percent`() {
        // $9.99/month * 12 = $119.88/year; a $99.99 yearly price is a ~16.6% saving, which
        // rounds to 17 — never hard-coded, always derived from the two live prices.
        val percent = yearlyDiscountPercent(monthlyPriceMicros = 9_990_000, yearlyPriceMicros = 99_990_000)
        assertEquals(17, percent)
    }

    @Test
    fun `discount is hidden when the monthly price is unavailable`() {
        assertNull(yearlyDiscountPercent(monthlyPriceMicros = 0, yearlyPriceMicros = 95_904_000))
    }

    @Test
    fun `discount is hidden when the yearly price is unavailable`() {
        assertNull(yearlyDiscountPercent(monthlyPriceMicros = 9_990_000, yearlyPriceMicros = 0))
    }

    @Test
    fun `discount is hidden rather than shown as zero or negative`() {
        // Yearly costing exactly 12x monthly, or more, is not a saving worth advertising.
        assertNull(yearlyDiscountPercent(monthlyPriceMicros = 9_990_000, yearlyPriceMicros = 119_880_000))
        assertNull(yearlyDiscountPercent(monthlyPriceMicros = 9_990_000, yearlyPriceMicros = 130_000_000))
    }

    @Test
    fun `recommended badge applies only to Komod 2 Doors`() {
        assertEquals(SubscriptionPlan.TwoDoors, RecommendedPlan)
        assertEquals(false, isRecommendedPlan(SubscriptionPlan.OneDoor))
        assertEquals(true, isRecommendedPlan(SubscriptionPlan.TwoDoors))
        assertEquals(false, isRecommendedPlan(SubscriptionPlan.ThreeDoors))
        assertEquals(false, isRecommendedPlan(SubscriptionPlan.WalkIn))
        assertEquals(false, isRecommendedPlan(SubscriptionPlan.Rack))
        assertEquals(false, isRecommendedPlan(SubscriptionPlan.Unknown))
    }

    // isRecommendedPlan() takes only the plan itself — no selectedPlan/currentPlan parameter
    // exists for it to consult, so "recommended" can never be computed from (or imply) either.
    @Test
    fun `recommended badge is independent of selection or current-plan state`() {
        assertEquals(true, isRecommendedPlan(SubscriptionPlan.TwoDoors))
    }
}
