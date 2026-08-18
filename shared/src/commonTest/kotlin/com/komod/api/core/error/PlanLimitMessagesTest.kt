package com.komod.api.core.error

import kotlin.test.Test
import kotlin.test.assertEquals

// Guards the exact copy shown to the user for each PlanLimitExceeded situation — a
// regression here would either leak technical language (the raw "PlanLimitExceeded" code,
// "entitlement", "provisional capacity") or silently drift from the agreed-on wording.
class PlanLimitMessagesTest {

    @Test
    fun `wardrobe approval capacity uses the wardrobe-full copy`() {
        val content = PlanLimitMessages.forCategory(PlanLimitCategory.WardrobeCapacity)

        assertEquals("Your Komod is full", content.title)
        assertEquals("Upgrade your plan to add more wardrobe items.", content.message)
    }

    @Test
    fun `pending upload or analysis capacity uses the review-or-remove copy`() {
        val content = PlanLimitMessages.forCategory(PlanLimitCategory.PendingCapacity)

        assertEquals("Your Komod is full", content.title)
        assertEquals("Finish reviewing or remove existing items before adding more photos.", content.message)
    }

    @Test
    fun `daily outfit generation limit uses the daily-limit copy`() {
        val content = PlanLimitMessages.forCategory(PlanLimitCategory.DailyGenerationLimit)

        assertEquals("Daily outfit limit reached", content.title)
        assertEquals("Upgrade your Komod plan to generate more outfits today.", content.message)
    }

    @Test
    fun `no message for any category mentions the raw backend code or internal terms`() {
        val forbiddenTerms = listOf("PlanLimitExceeded", "entitlement", "provisional capacity", "subscription service")

        for (category in PlanLimitCategory.entries) {
            val content = PlanLimitMessages.forCategory(category)
            for (term in forbiddenTerms) {
                assertEquals(
                    false,
                    content.title.contains(term, ignoreCase = true) || content.message.contains(term, ignoreCase = true),
                    "Category $category leaked technical term \"$term\"",
                )
            }
        }
    }
}
