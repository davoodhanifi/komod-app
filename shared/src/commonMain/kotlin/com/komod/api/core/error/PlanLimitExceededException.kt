package com.komod.api.core.error

// The backend returns the same "PlanLimitExceeded" error code for several unrelated
// plan/capacity limits (wardrobe full, upload/pending capacity full, daily outfit
// generation limit). [category] records which limit this particular call hit — set by
// the API-service call site that detected the code, based on which endpoint it called —
// so the UI can show the right copy without string-matching the backend's English
// message.
enum class PlanLimitCategory {
    WardrobeCapacity,
    PendingCapacity,
    DailyGenerationLimit,
}

class PlanLimitExceededException(val category: PlanLimitCategory) : RuntimeException()
