package com.komod.api.data.repository

import com.komod.api.data.auth.SupabaseAuthDataSource
import com.komod.api.domain.model.SubscriptionType
import com.komod.api.domain.model.User
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

class AuthRepositoryImpl(
    private val authDataSource: SupabaseAuthDataSource,
) : AuthRepository {

    override val sessionStatus = authDataSource.sessionStatus

    private val _callbackError = MutableSharedFlow<Throwable>()
    override val callbackError = _callbackError.asSharedFlow()

    override suspend fun signInWithGoogle() {
        authDataSource.signInWithGoogle()
    }

    override suspend fun handleOAuthCallback(url: String) {
        runCatching {
            authDataSource.handleOAuthCallback(url)
        }.onFailure { error ->
            _callbackError.emit(error)
        }
    }

    override fun currentUserOrNull(): User? {
        val userInfo = authDataSource.currentUserOrNull() ?: return null
        val metadata = userInfo.userMetadata
        val displayName = userInfo.userMetadata?.get("full_name")?.jsonPrimitive?.contentOrNull
            ?: userInfo.userMetadata?.get("name")?.jsonPrimitive?.contentOrNull
            ?: userInfo.email?.substringBefore('@')
        val photoUrl = userInfo.userMetadata?.get("avatar_url")?.jsonPrimitive?.contentOrNull
            ?: userInfo.userMetadata?.get("avatarUrl")?.jsonPrimitive?.contentOrNull
            ?: userInfo.userMetadata?.get("picture")?.jsonPrimitive?.contentOrNull
            ?: userInfo.userMetadata?.get("photo_url")?.jsonPrimitive?.contentOrNull
            ?: userInfo.userMetadata?.get("photoUrl")?.jsonPrimitive?.contentOrNull
        val subscriptionType = resolveSubscriptionType(metadata)

        return User(
            id = userInfo.id,
            email = userInfo.email,
            displayName = displayName,
            photoUrl = photoUrl,
            subscriptionType = subscriptionType,
        )
    }

    override suspend fun signOut() {
        authDataSource.signOut()
    }
}

private fun resolveSubscriptionType(metadata: JsonObject?): SubscriptionType {
    if (metadata == null) return SubscriptionType.FREE

    val paidValues = setOf("pro", "premium", "paid", "plus")
    val possibleTierValues = listOf(
        "subscription_type",
        "subscriptionType",
        "subscription",
        "plan",
        "tier",
        "membership",
    ).mapNotNull { key ->
        metadata[key]?.jsonPrimitive?.contentOrNull?.lowercase()
    }

    if (possibleTierValues.any { it in paidValues }) {
        return SubscriptionType.PRO
    }

    val isProValue = metadata["is_pro"]?.jsonPrimitive?.contentOrNull?.lowercase()
        ?: metadata["isPro"]?.jsonPrimitive?.contentOrNull?.lowercase()

    return if (isProValue == "true" || isProValue == "1" || isProValue == "yes") {
        SubscriptionType.PRO
    } else {
        SubscriptionType.FREE
    }
}
