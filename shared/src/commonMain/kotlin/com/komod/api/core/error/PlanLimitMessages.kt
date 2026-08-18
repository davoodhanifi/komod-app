package com.komod.api.core.error

/**
 * Pre-approved, non-technical copy for each [PlanLimitCategory] — never the backend's raw
 * "PlanLimitExceeded" code or its "detail" text, matching [ErrorMapper]/[UserFacingMessages]'s
 * rule that user-facing copy always comes from a fixed, hand-written string.
 */
object PlanLimitMessages {
    data class Content(val title: String, val message: String)

    fun forCategory(category: PlanLimitCategory): Content = when (category) {
        PlanLimitCategory.WardrobeCapacity -> Content(
            title = "Your Komod is full",
            message = "Upgrade your plan to add more wardrobe items.",
        )
        PlanLimitCategory.PendingCapacity -> Content(
            title = "Your Komod is full",
            message = "Finish reviewing or remove existing items before adding more photos.",
        )
        PlanLimitCategory.DailyGenerationLimit -> Content(
            title = "Daily outfit limit reached",
            message = "Upgrade your Komod plan to generate more outfits today.",
        )
    }
}
