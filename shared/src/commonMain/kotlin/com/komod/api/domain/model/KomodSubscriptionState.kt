package com.komod.api.domain.model

// Mirrors the backend's SubscriptionStatus enum (POST /subscription/sync). Unknown covers any
// value this build doesn't recognize yet — same reasoning as SubscriptionPlan.Unknown.
enum class SubscriptionStatus {
    Active,
    Cancelled,
    Expired,
    Unknown,
}

// The backend's authoritative Komod subscription state, returned by POST /subscription/sync
// after it independently reconciles with RevenueCat. This — never a RevenueCat entitlement
// check performed locally on the device — is the source of truth for "what Komod plan is this
// user on right now".
//
// Mobile never computes expiry locally: a plan stays [plan] until the backend itself reports
// it as Expired (at which point the backend also flips [plan] to Rack) — there is no date math
// on this side, by design (see SubscriptionSyncRepository).
data class KomodSubscriptionState(
    val plan: SubscriptionPlan,
    val status: SubscriptionStatus,
    val expiresAt: String?,
    val willRenew: Boolean,
    val store: String?,
    val productId: String?,
)
