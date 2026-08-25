package com.komod.api.data.repository

import com.komod.api.core.error.AppLogger
import com.komod.api.data.api.SubscriptionApiService
import com.komod.api.data.api.model.SyncSubscriptionDto
import com.komod.api.data.billing.RevenueCatIdentitySync
import com.komod.api.domain.model.KomodSubscriptionState
import com.komod.api.domain.model.SubscriptionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Reconciles RevenueCat's purchase state with the backend via POST /subscription/sync and
// exposes the result — the backend's response, never anything inferred from RevenueCat
// locally, is the source of truth for the user's current Komod plan (see
// KomodSubscriptionState). Shared as a single StateFlow so every screen that cares about the
// synced plan (Paywall today) observes the same last-known value instead of each holding its
// own, possibly stale, copy.
interface SubscriptionSyncRepository {
    val subscriptionState: StateFlow<KomodSubscriptionState?>

    // Throws on failure — callers must not fabricate a plan locally when this fails (the
    // RevenueCat purchase/restore that triggered the sync is never undone by a sync failure;
    // see PaywallViewModel's purchase/restore flows). Also throws
    // RevenueCatIdentityNotReadyException without calling the backend at all if RevenueCat's
    // identity isn't confirmed ready for the current Supabase user yet — syncing against a
    // possibly-still-anonymous identity would let the backend read the wrong (or no)
    // RevenueCat customer.
    suspend fun sync(): KomodSubscriptionState
}

class SubscriptionSyncRepositoryImpl(
    private val subscriptionApiService: SubscriptionApiService,
    private val revenueCatIdentitySync: RevenueCatIdentitySync,
) : SubscriptionSyncRepository {
    private val _subscriptionState = MutableStateFlow<KomodSubscriptionState?>(null)
    override val subscriptionState: StateFlow<KomodSubscriptionState?> = _subscriptionState.asStateFlow()

    override suspend fun sync(): KomodSubscriptionState {
        revenueCatIdentitySync.awaitReadyForCurrentUser()
        val state = subscriptionApiService.syncSubscription().toDomain()
        _subscriptionState.value = state
        return state
    }
}

internal fun SyncSubscriptionDto.toDomain(): KomodSubscriptionState = KomodSubscriptionState(
    plan = parseSubscriptionPlan(plan),
    status = parseSubscriptionStatus(status),
    expiresAt = expiresAt,
    willRenew = willRenew,
    store = store,
    productId = productId,
)

internal fun parseSubscriptionStatus(raw: String): SubscriptionStatus {
    val status = when (raw.trim().lowercase()) {
        "active" -> SubscriptionStatus.Active
        "cancelled", "canceled" -> SubscriptionStatus.Cancelled
        "expired" -> SubscriptionStatus.Expired
        else -> SubscriptionStatus.Unknown
    }

    if (status == SubscriptionStatus.Unknown) {
        // A logging failure must never affect parsing's result — same reasoning as
        // SubscriptionRepositoryImpl.parseSubscriptionPlan.
        runCatching {
            AppLogger.e("SubscriptionSyncRepository", "Unrecognized subscription status from backend. Raw: \"$raw\"")
        }
    }

    return status
}
