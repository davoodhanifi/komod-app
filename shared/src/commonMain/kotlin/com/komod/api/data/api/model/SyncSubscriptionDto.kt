package com.komod.api.data.api.model

import kotlinx.serialization.Serializable

// plan/status are decoded as raw Strings (not Kotlin enums), same reasoning as
// CurrentSubscriptionDto.plan: an unrecognized value must never fail deserialization outright.
// expiresAt is passed through as the raw ISO-8601 string the backend sends — mobile never
// parses or compares it locally (see KomodSubscriptionState).
@Serializable
data class SyncSubscriptionDto(
    val plan: String,
    val status: String,
    val expiresAt: String? = null,
    val willRenew: Boolean = false,
    val store: String? = null,
    val productId: String? = null,
)
