package com.komod.api.data.repository

import com.komod.api.data.auth.SupabaseAuthDataSource
import com.komod.api.domain.model.SubscriptionType
import com.komod.api.domain.model.User
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

class AuthRepositoryImpl(
    private val authDataSource: SupabaseAuthDataSource,
) : AuthRepository {

    override val sessionStatus = authDataSource.sessionStatus
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _callbackError = MutableSharedFlow<Throwable>()
    override val callbackError = _callbackError.asSharedFlow()
    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser = _currentUser.asStateFlow()

    init {
        syncCurrentUser()
        repositoryScope.launch {
            sessionStatus.collect { status ->
                if (status is SessionStatus.Authenticated) {
                    syncCurrentUser()
                } else {
                    _currentUser.value = null
                }
            }
        }
    }

    override suspend fun signInWithGoogle() {
        authDataSource.signInWithGoogle()
    }

    override suspend fun handleOAuthCallback(url: String) {
        runCatching {
            authDataSource.handleOAuthCallback(url)
            syncCurrentUser()
        }.onFailure { error ->
            _callbackError.emit(error)
        }
    }

    override fun currentUserOrNull(): User? {
        val userInfo = authDataSource.currentUserOrNull() ?: return null
        val metadataSources = buildMetadataSources(userInfo)
        val displayName = findMetadataValue(
            metadataSources = metadataSources,
            keys = listOf("full_name", "name", "display_name", "preferred_username"),
        ) ?: composeFullName(
            firstName = findMetadataValue(
                metadataSources = metadataSources,
                keys = listOf("given_name", "givenName", "first_name", "firstName"),
            ),
            lastName = findMetadataValue(
                metadataSources = metadataSources,
                keys = listOf("family_name", "familyName", "last_name", "lastName"),
            ),
        )
            ?: userInfo.email?.substringBefore('@')
        val photoUrl = findMetadataValue(
            metadataSources = metadataSources,
            keys = listOf("avatar_url", "avatarUrl", "picture", "photo_url", "photoUrl", "picture_url"),
        )
        val subscriptionType = resolveSubscriptionType(metadataSources)

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
        _currentUser.value = null
    }

    private fun syncCurrentUser() {
        _currentUser.value = currentUserOrNull()
    }
}

private fun resolveSubscriptionType(metadataSources: List<JsonObject>): SubscriptionType {
    if (metadataSources.isEmpty()) return SubscriptionType.FREE

    val paidValues = setOf("pro", "premium", "paid", "plus")
    val possibleTierValues = listOf(
        "subscription_type",
        "subscriptionType",
        "subscription",
        "plan",
        "tier",
        "membership",
    ).mapNotNull { key -> findMetadataValue(metadataSources, listOf(key))?.lowercase() }

    if (possibleTierValues.any { it in paidValues }) {
        return SubscriptionType.PRO
    }

    val isProValue = findMetadataValue(metadataSources, listOf("is_pro", "isPro"))?.lowercase()

    return if (isProValue == "true" || isProValue == "1" || isProValue == "yes") {
        SubscriptionType.PRO
    } else {
        SubscriptionType.FREE
    }
}

private fun buildMetadataSources(userInfo: io.github.jan.supabase.auth.user.UserInfo): List<JsonObject> {
    return buildList {
        userInfo.userMetadata?.let(::add)
        userInfo.appMetadata?.let(::add)
        userInfo.identities?.forEach { identity ->
            add(identity.identityData)
        }
    }
}

private fun findMetadataValue(
    metadataSources: List<JsonObject>,
    keys: List<String>,
): String? {
    for (source in metadataSources) {
        for (key in keys) {
            val value = source[key]?.jsonPrimitive?.contentOrNull?.trim()
            if (!value.isNullOrEmpty()) return value
        }
    }
    return null
}

private fun composeFullName(firstName: String?, lastName: String?): String? {
    val parts = listOfNotNull(firstName?.trim(), lastName?.trim()).filter { it.isNotEmpty() }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" ")
}
