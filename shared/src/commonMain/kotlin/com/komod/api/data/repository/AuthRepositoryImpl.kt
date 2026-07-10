package com.komod.api.data.repository

import com.komod.api.data.auth.SupabaseAuthDataSource
import com.komod.api.domain.model.User
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
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
        val displayName = userInfo.userMetadata?.get("full_name")?.jsonPrimitive?.contentOrNull
            ?: userInfo.userMetadata?.get("name")?.jsonPrimitive?.contentOrNull
            ?: userInfo.email?.substringBefore('@')
        val photoUrl = userInfo.userMetadata?.get("avatar_url")?.jsonPrimitive?.contentOrNull
            ?: userInfo.userMetadata?.get("avatarUrl")?.jsonPrimitive?.contentOrNull
            ?: userInfo.userMetadata?.get("picture")?.jsonPrimitive?.contentOrNull
            ?: userInfo.userMetadata?.get("photo_url")?.jsonPrimitive?.contentOrNull
            ?: userInfo.userMetadata?.get("photoUrl")?.jsonPrimitive?.contentOrNull

        return User(
            id = userInfo.id,
            email = userInfo.email,
            displayName = displayName,
            photoUrl = photoUrl,
        )
    }

    override suspend fun signOut() {
        authDataSource.signOut()
    }
}
