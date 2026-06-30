package com.komod.api.data.repository

import com.komod.api.domain.model.User
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val sessionStatus: StateFlow<SessionStatus>
    val callbackError: SharedFlow<Throwable>

    suspend fun signInWithGoogle()

    suspend fun handleOAuthCallback(url: String)

    fun currentUserOrNull(): User?

    suspend fun signOut()
}
