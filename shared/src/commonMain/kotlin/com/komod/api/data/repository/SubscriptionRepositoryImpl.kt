package com.komod.api.data.repository

import com.komod.api.core.error.AppLogger
import com.komod.api.data.api.SubscriptionApiService
import com.komod.api.data.api.model.CurrentSubscriptionDto
import com.komod.api.domain.model.CurrentSubscription
import com.komod.api.domain.model.SubscriptionPlan

class SubscriptionRepositoryImpl(
    private val subscriptionApiService: SubscriptionApiService,
) : SubscriptionRepository {
    override suspend fun getCurrentSubscription(): CurrentSubscription {
        val dto = subscriptionApiService.getCurrentSubscription()
        return dto.toDomain()
    }
}

// Extracted from SubscriptionRepositoryImpl so the DTO-mapping logic is unit-testable
// directly, without a live HttpClient — same reasoning as AuthRepositoryImpl.buildUser().
internal fun CurrentSubscriptionDto.toDomain(): CurrentSubscription = CurrentSubscription(
    plan = parseSubscriptionPlan(plan),
    wardrobeItemLimit = wardrobeItemLimit,
    dailyOutfitGenerationLimit = dailyOutfitGenerationLimit,
    currentWardrobeItemCount = currentWardrobeItemCount,
    todayOutfitGenerationCount = todayOutfitGenerationCount,
)

// Confirmed against the actual backend enum (JsonStringEnumConverter over KomodPlan):
//   KomodRack, Komod1Door, Komod2Doors, Komod3Doors, KomodWalkIn
// i.e. the "Komod" prefix is baked into the wire value itself, not just the display name
// — matching case-insensitively and after stripping separators so formatting differences
// (spacing/casing/hyphens) can't cause a mismatch. The older no-prefix/digit-only aliases
// ("rack", "1door", "0".."4") are kept as a second line of defense in case a differently
// shaped value ever shows up; an unrecognized value falls back to Unknown instead of
// throwing, so a future/renamed plan never breaks the whole Profile screen.
internal fun parseSubscriptionPlan(raw: String): SubscriptionPlan {
    val normalized = raw.trim().lowercase().filter { it.isLetterOrDigit() }
    val plan = when (normalized) {
        "komodrack", "rack", "0" -> SubscriptionPlan.Rack
        "komod1door", "onedoor", "1door", "door1", "1" -> SubscriptionPlan.OneDoor
        "komod2doors", "twodoors", "twodoor", "2doors", "2door", "door2", "2" -> SubscriptionPlan.TwoDoors
        "komod3doors", "threedoors", "threedoor", "3doors", "3door", "door3", "3" -> SubscriptionPlan.ThreeDoors
        "komodwalkin", "walkin", "4" -> SubscriptionPlan.WalkIn
        else -> SubscriptionPlan.Unknown
    }

    if (plan == SubscriptionPlan.Unknown) {
        // A logging failure must never affect parsing's result — same reasoning as
        // PlanLimitDetection.isPlanLimitExceeded.
        runCatching {
            AppLogger.e("SubscriptionRepository", "Unrecognized plan value from backend. Raw: \"$raw\"")
        }
    }

    return plan
}
