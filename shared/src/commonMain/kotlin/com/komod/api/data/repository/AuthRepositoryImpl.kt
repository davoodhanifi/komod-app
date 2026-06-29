package com.komod.api.data.repository

import com.komod.api.data.auth.SupabaseAuthDataSource
import com.komod.api.domain.model.User
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

class AuthRepositoryImpl(
    private val authDataSource: SupabaseAuthDataSource,
) : AuthRepository {

    override val sessionStatus = authDataSource.sessionStatus

    override suspend fun signInWithGoogle() {
        authDataSource.signInWithGoogle()
    }

    override suspend fun handleOAuthCallback(url: String) {
        authDataSource.handleOAuthCallback(url)
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
