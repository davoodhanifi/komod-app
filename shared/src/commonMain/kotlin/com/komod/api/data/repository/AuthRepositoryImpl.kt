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

        return User(
            id = userInfo.id,
            email = userInfo.email,
            displayName = displayName,
        )
    }

    override suspend fun signOut() {
        authDataSource.signOut()
    }
}
