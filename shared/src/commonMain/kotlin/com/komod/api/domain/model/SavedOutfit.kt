package com.komod.api.domain.model

data class SavedOutfit(
    val id: String,
    val name: String,
    val reason: String,
    val wardrobeItemIds: List<String>,
    val isFavorite: Boolean,
    val createdAt: String,
    // Resolved from the local wardrobe cache only (see WardrobeItemCache) — no network
    // fetches are made to hydrate these, so items whose wardrobe entry isn't cached yet
    // are simply absent here rather than triggering a fetch.
    val items: List<OutfitItem>,
)

fun SavedOutfit.toOutfit(): Outfit = Outfit(
    id = id,
    name = name,
    reason = reason,
    matchScore = stableMatchScore(name + wardrobeItemIds.joinToString()),
    wardrobeItemIds = wardrobeItemIds,
    items = items,
)

private fun stableMatchScore(seed: String): Int {
    val hash = seed.hashCode()
    val bucket = (if (hash < 0) -hash else hash) % 10
    return 90 + bucket
}
