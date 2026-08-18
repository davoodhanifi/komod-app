package com.komod.api.data.api.model

import kotlinx.serialization.Serializable

// plan is decoded as a raw String (not a Kotlin enum) so an unrecognized value never fails
// deserialization outright — SubscriptionRepositoryImpl maps it into SubscriptionPlan
// leniently. wardrobeItemLimit/dailyOutfitGenerationLimit are null for an unlimited plan
// (e.g. Walk-in) — the backend is the only source of truth for these, never a local default.
@Serializable
data class CurrentSubscriptionDto(
    val plan: String,
    val wardrobeItemLimit: Int? = null,
    val dailyOutfitGenerationLimit: Int? = null,
    val currentWardrobeItemCount: Int,
    val todayOutfitGenerationCount: Int,
)
