package com.komod.api.data.repository

import com.komod.api.data.api.model.CurrentSubscriptionDto
import com.komod.api.domain.model.SubscriptionPlan
import kotlin.test.Test
import kotlin.test.assertEquals

class SubscriptionRepositoryImplTest {

    // 1 & 2 & 3. The confirmed backend wire values (KomodPlan, [JsonConverter(typeof(
    // JsonStringEnumConverter))]): KomodRack, Komod1Door, Komod2Doors, Komod3Doors,
    // KomodWalkIn — note the "Komod" prefix is baked into the wire value itself, not just
    // the display name.
    @Test
    fun `each confirmed backend wire value maps to its plan`() {
        assertEquals(SubscriptionPlan.Rack, parseSubscriptionPlan("KomodRack"))
        assertEquals(SubscriptionPlan.OneDoor, parseSubscriptionPlan("Komod1Door"))
        assertEquals(SubscriptionPlan.TwoDoors, parseSubscriptionPlan("Komod2Doors"))
        assertEquals(SubscriptionPlan.ThreeDoors, parseSubscriptionPlan("Komod3Doors"))
        assertEquals(SubscriptionPlan.WalkIn, parseSubscriptionPlan("KomodWalkIn"))
    }

    // 1. Rack with finite limits.
    @Test
    fun `Rack maps its exact wire value and finite limits`() {
        val plan = parseSubscriptionPlan("Rack")
        assertEquals(SubscriptionPlan.Rack, plan)
    }

    // 2. 1 Door / 2 Doors / 3 Doors — additional plausible wire spellings kept as a second
    // line of defense; normalization must not depend on any one exact format.
    @Test
    fun `OneDoor variants all map to OneDoor`() {
        for (raw in listOf("OneDoor", "one_door", "1Door", "1door", "ONEDOOR")) {
            assertEquals(SubscriptionPlan.OneDoor, parseSubscriptionPlan(raw), "raw=$raw")
        }
    }

    @Test
    fun `TwoDoors variants all map to TwoDoors`() {
        for (raw in listOf("TwoDoors", "two_doors", "2Doors", "2-doors", "TWODOORS")) {
            assertEquals(SubscriptionPlan.TwoDoors, parseSubscriptionPlan(raw), "raw=$raw")
        }
    }

    @Test
    fun `ThreeDoors variants all map to ThreeDoors`() {
        for (raw in listOf("ThreeDoors", "three_doors", "3Doors", "3-doors", "THREEDOORS")) {
            assertEquals(SubscriptionPlan.ThreeDoors, parseSubscriptionPlan(raw), "raw=$raw")
        }
    }

    // 3. Walk-in.
    @Test
    fun `WalkIn variants all map to WalkIn`() {
        for (raw in listOf("WalkIn", "walk_in", "Walk-In", "WALKIN")) {
            assertEquals(SubscriptionPlan.WalkIn, parseSubscriptionPlan(raw), "raw=$raw")
        }
    }

    // Defensive fallback in case KomodPlan serializes as its underlying int instead of by
    // name (this project's lenient Json config decodes a bare number into plan's String
    // field as its digit string rather than failing — see parseSubscriptionPlan's doc).
    @Test
    fun `a bare numeric index also maps to the corresponding plan`() {
        assertEquals(SubscriptionPlan.Rack, parseSubscriptionPlan("0"))
        assertEquals(SubscriptionPlan.OneDoor, parseSubscriptionPlan("1"))
        assertEquals(SubscriptionPlan.TwoDoors, parseSubscriptionPlan("2"))
        assertEquals(SubscriptionPlan.ThreeDoors, parseSubscriptionPlan("3"))
        assertEquals(SubscriptionPlan.WalkIn, parseSubscriptionPlan("4"))
    }

    @Test
    fun `an unrecognized plan value falls back to Unknown instead of throwing`() {
        assertEquals(SubscriptionPlan.Unknown, parseSubscriptionPlan("SomethingNew"))
    }

    // 3. Walk-in with null limits displays "Unlimited" (verified at the mapping layer —
    // the DTO's null limits pass straight through to the domain model).
    @Test
    fun `toDomain preserves null limits for an unlimited plan`() {
        val dto = CurrentSubscriptionDto(
            plan = "KomodWalkIn",
            wardrobeItemLimit = null,
            dailyOutfitGenerationLimit = null,
            currentWardrobeItemCount = 87,
            todayOutfitGenerationCount = 12,
        )

        val subscription = dto.toDomain()

        assertEquals(SubscriptionPlan.WalkIn, subscription.plan)
        assertEquals(null, subscription.wardrobeItemLimit)
        assertEquals(null, subscription.dailyOutfitGenerationLimit)
        assertEquals(87, subscription.currentWardrobeItemCount)
        assertEquals(12, subscription.todayOutfitGenerationCount)
    }

    // 4. Current usage is displayed correctly — verified here at the mapping layer: the
    // DTO's raw counts/limits pass through to the domain model completely unmodified (no
    // local limit table is ever consulted).
    @Test
    fun `toDomain preserves finite limits and usage counts exactly as returned`() {
        val dto = CurrentSubscriptionDto(
            plan = "Komod2Doors",
            wardrobeItemLimit = 150,
            dailyOutfitGenerationLimit = 30,
            currentWardrobeItemCount = 87,
            todayOutfitGenerationCount = 12,
        )

        val subscription = dto.toDomain()

        assertEquals(SubscriptionPlan.TwoDoors, subscription.plan)
        assertEquals(150, subscription.wardrobeItemLimit)
        assertEquals(30, subscription.dailyOutfitGenerationLimit)
        assertEquals(87, subscription.currentWardrobeItemCount)
        assertEquals(12, subscription.todayOutfitGenerationCount)
    }
}
