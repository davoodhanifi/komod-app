package com.komod.api.data.billing

import com.komod.api.domain.model.SubscriptionPlan
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Guards against the mobile app ever pointing at the wrong RevenueCat App (RevenueCat projects
// can contain multiple Apps with different product catalogs — see REVENUECAT_IOS_API_KEY's doc
// comment in local.properties). All product IDs here must stay prefixed "komod_" and match the
// app0d17d65072 App's catalog: the backend only maps komod_* product IDs to a KomodPlan, and
// generic IDs like "monthly"/"yearly" (the app2945c763c3 App's catalog) can't be told apart.
class RevenueCatCatalogTest {

    @Test
    fun `catalog covers every purchasable SubscriptionPlan`() {
        val purchasablePlans = SubscriptionPlan.entries - SubscriptionPlan.Rack - SubscriptionPlan.Unknown
        assertEquals(purchasablePlans.toSet(), RevenueCatCatalog.plans.keys)
    }

    @Test
    fun `every package id is a komod product and never a generic monthly or yearly id`() {
        RevenueCatCatalog.plans.values.forEach { planIds ->
            assertTrue(planIds.monthlyPackageId.startsWith("komod_"), planIds.monthlyPackageId)
            assertTrue(planIds.yearlyPackageId.startsWith("komod_"), planIds.yearlyPackageId)
        }
    }

    @Test
    fun `package ids match the expected app0d17d65072 catalog exactly`() {
        val expected = mapOf(
            SubscriptionPlan.OneDoor to RevenueCatPlanIds("komod_1door_monthly", "komod_1door_yearly"),
            SubscriptionPlan.TwoDoors to RevenueCatPlanIds("komod_2doors_monthly", "komod_2doors_yearly"),
            SubscriptionPlan.ThreeDoors to RevenueCatPlanIds("komod_3doors_monthly", "komod_3doors_yearly"),
            SubscriptionPlan.WalkIn to RevenueCatPlanIds("komod_walkin_monthly", "komod_walkin_yearly"),
        )
        assertEquals(expected, RevenueCatCatalog.plans)
    }
}
