package com.komod.api.data.repository

// Thrown when POST /outfits/generate returns 404 — the backend uses this for both an
// empty/inactive wardrobe and a selected item that's no longer in the active wardrobe.
// Neither case carries a stable machine-readable code (unlike PlanLimitExceeded), so both
// map to this single type rather than parsing the translatable "detail" text to tell them apart.
class OutfitGenerationNotFoundException : RuntimeException()
