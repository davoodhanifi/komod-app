package com.komod.api.domain.model

// Unknown covers any backend plan value this build doesn't recognize yet — display code
// falls back to a generic label for it rather than crashing or mislabeling it as Rack.
enum class SubscriptionPlan {
    Rack,
    OneDoor,
    TwoDoors,
    ThreeDoors,
    WalkIn,
    Unknown,
}

// wardrobeItemLimit/dailyOutfitGenerationLimit are null for an unlimited plan (Walk-in) —
// always sourced from the backend response, never inferred from [plan] locally.
data class CurrentSubscription(
    val plan: SubscriptionPlan,
    val wardrobeItemLimit: Int?,
    val dailyOutfitGenerationLimit: Int?,
    val currentWardrobeItemCount: Int,
    val todayOutfitGenerationCount: Int,
)
