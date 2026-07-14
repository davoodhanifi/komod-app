package com.komod.api.domain.model

enum class SubscriptionType {
    FREE,
    PRO,
}

data class User(
    val id: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?,
    val subscriptionType: SubscriptionType = SubscriptionType.FREE,
)
