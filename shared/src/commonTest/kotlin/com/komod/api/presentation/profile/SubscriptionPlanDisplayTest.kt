package com.komod.api.presentation.profile

import com.komod.api.domain.model.SubscriptionPlan
import kotlin.test.Test
import kotlin.test.assertEquals

// 2. 1 Door / 2 Doors / 3 Doors / Rack / Walk-in rendering the correct display names.
class SubscriptionPlanDisplayTest {

    @Test
    fun `each known plan maps to its exact spec display name`() {
        assertEquals("Komod Rack", SubscriptionPlan.Rack.displayName())
        assertEquals("Komod 1 Door", SubscriptionPlan.OneDoor.displayName())
        assertEquals("Komod 2 Doors", SubscriptionPlan.TwoDoors.displayName())
        assertEquals("Komod 3 Doors", SubscriptionPlan.ThreeDoors.displayName())
        assertEquals("Komod Walk-in", SubscriptionPlan.WalkIn.displayName())
    }

    @Test
    fun `an unrecognized plan still gets a sensible, non-crashing display name`() {
        assertEquals("Your Komod Plan", SubscriptionPlan.Unknown.displayName())
    }
}
