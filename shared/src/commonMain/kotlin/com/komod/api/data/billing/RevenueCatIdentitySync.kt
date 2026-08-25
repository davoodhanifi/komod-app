package com.komod.api.data.billing

import com.komod.api.core.error.AppLogger
import com.komod.api.data.repository.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * RevenueCat's identity state relative to the currently authenticated Supabase user. This is
 * the single source of truth for "is it safe to purchase/restore/sync yet" — never inferred
 * from [PurchasesService.isConfigured] (that only means the SDK was initialized, not that the
 * right identity is active) and never assumed after a fixed delay.
 */
sealed interface RevenueCatIdentityState {
    data object NotReady : RevenueCatIdentityState
    data object Syncing : RevenueCatIdentityState

    // The Supabase UUID RevenueCat is currently identified as, or null when correctly reset to
    // anonymous (no authenticated Supabase user — e.g. right after sign-out).
    data class Ready(val appUserId: String?) : RevenueCatIdentityState

    data class Failed(val message: String) : RevenueCatIdentityState
}

// Thrown by awaitReadyForCurrentUser() — callers (BillingRepository, SubscriptionSyncRepository)
// must not proceed with a purchase/restore/sync when this is thrown, and must not fabricate any
// subscription state as a result.
class RevenueCatIdentityNotReadyException(message: String) : RuntimeException(message)

/**
 * Keeps RevenueCat's identified appUserID aligned with the authenticated Supabase user ID —
 * the two must always match exactly, since the backend's POST /subscription/sync endpoint
 * assumes RevenueCat's appUserID *is* the Supabase user ID and reads RevenueCat itself using
 * it (no separate identity is ever sent or maintained).
 *
 * Identity mapping:
 *   Supabase user UUID == RevenueCat appUserID == backend authenticated user ID
 *
 * Anonymous-to-identified migration and "this appUserID already exists" merging are entirely
 * RevenueCat's own documented logIn() behavior — this class never inspects or branches on
 * that, it only decides *when* to call logIn()/logOut() and exposes the result as
 * [identityState] so callers can *wait* for it (see [awaitReadyForCurrentUser]) instead of
 * assuming it already happened by the time they run.
 *
 * Started eagerly on iOS (see MainViewController.kt's initKoin()). Harmless on Android: with
 * [PurchasesService.isConfigured] false there, [syncIdentity] never attempts a real SDK call,
 * so identity just stays [RevenueCatIdentityState.NotReady] — moot anyway, since
 * [BillingRepository.isBillingAvailable] already rejects purchases/restores there before
 * [awaitReadyForCurrentUser] would ever be reached.
 */
class RevenueCatIdentitySync(
    private val authRepository: AuthRepository,
    private val purchasesService: PurchasesService,
    // Overridable so tests can pass runTest's `backgroundScope` and drive this deterministically
    // — production always uses the default.
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val _identityState = MutableStateFlow<RevenueCatIdentityState>(RevenueCatIdentityState.NotReady)
    val identityState: StateFlow<RevenueCatIdentityState> = _identityState.asStateFlow()

    init {
        scope.launch {
            authRepository.currentUser
                // Keyed on just the id (not the whole User) so an unrelated profile field
                // (displayName, photoUrl) refreshing on the same still-authenticated user
                // never triggers a redundant logIn() call.
                .map { it?.id }
                .distinctUntilChanged()
                .collect { supabaseUserId -> syncIdentity(supabaseUserId) }
        }
    }

    // Re-attempts identity sync for whatever the current Supabase user is right now. Exposed so
    // a permanently-Failed state (all 3 attempts exhausted) can be retried later, without
    // waiting for the user to sign out and back in — awaitReadyForCurrentUser() below calls
    // this automatically the next time something actually needs identity to be ready.
    fun retry() {
        scope.launch { syncIdentity(authRepository.currentUser.value?.id) }
    }

    // Suspends until RevenueCat's identity is confirmed Ready for the *currently* authenticated
    // Supabase user (or confirmed reset to anonymous, if signed out) — every wait below is a
    // suspension on identityState's own emissions (Flow.first), never a fixed delay or a
    // time-based polling loop, so this resumes the instant the real logIn()/logOut() call
    // actually completes.
    //
    // Never returns without a confirmed-correct identity: if the last sync attempt is still
    // Failed, this retries it once (see retry()) before waiting again; if identity settles
    // Ready for a *different* user than the one currently signed in (a stale result from a
    // sign-out/sign-in race), this keeps waiting rather than treating that as ready.
    suspend fun awaitReadyForCurrentUser(): String {
        while (true) {
            when (val current = identityState.value) {
                is RevenueCatIdentityState.Ready -> {
                    val expected = authRepository.currentUser.value?.id
                    if (current.appUserId == expected) {
                        return current.appUserId
                            ?: throw RevenueCatIdentityNotReadyException(
                                "No authenticated user to purchase, restore, or sync for.",
                            )
                    }
                    identityState.first { it != current }
                }
                is RevenueCatIdentityState.Failed -> {
                    retry()
                    identityState.first { it != current }
                }
                RevenueCatIdentityState.NotReady, RevenueCatIdentityState.Syncing -> {
                    identityState.first { it != current }
                }
            }
        }
    }

    private suspend fun syncIdentity(supabaseUserId: String?) {
        if (!purchasesService.isConfigured) {
            // Nothing to identify on a platform RevenueCat was never configured on — stay
            // NotReady rather than attempting (and failing) a real SDK call. See class doc.
            return
        }
        _identityState.value = RevenueCatIdentityState.Syncing
        repeat(MaxAttempts) { attempt ->
            val result = runCatching {
                if (supabaseUserId != null) {
                    purchasesService.logIn(supabaseUserId)
                } else {
                    purchasesService.logOut()
                }
            }
            if (result.isSuccess) {
                _identityState.value = RevenueCatIdentityState.Ready(supabaseUserId)
                return
            }
            if (attempt == MaxAttempts - 1) {
                val error = result.exceptionOrNull()
                _identityState.value = RevenueCatIdentityState.Failed(
                    error?.message ?: "Failed to sync RevenueCat identity",
                )
                // A logging failure must never affect the state transition above — same
                // reasoning as SubscriptionRepositoryImpl.parseSubscriptionPlan/
                // PlanLimitDetection.isPlanLimitExceeded.
                runCatching {
                    AppLogger.e(
                        "RevenueCatIdentitySync",
                        "Failed to sync RevenueCat identity after $MaxAttempts attempts",
                        error,
                    )
                }
            } else {
                delay(RetryDelayMillis)
            }
        }
    }

    private companion object {
        const val MaxAttempts = 3
        const val RetryDelayMillis = 1000L
    }
}
