package com.komod.api.data.auth

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.parseSessionFromUrl
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.flow.StateFlow

class SupabaseAuthDataSource(
    val supabaseClient: SupabaseClient,
) {
    val sessionStatus: StateFlow<SessionStatus> = supabaseClient.auth.sessionStatus

    suspend fun signInWithGoogle() {
        supabaseClient.auth.signInWith(Google)
    }

    suspend fun handleOAuthCallback(url: String) {
        supabaseClient.auth.parseSessionFromUrl(url)
    }

    fun currentAccessToken(): String? = supabaseClient.auth.currentSessionOrNull()?.accessToken

    fun currentUserOrNull(): UserInfo? = supabaseClient.auth.currentUserOrNull()

    suspend fun signOut() {
        supabaseClient.auth.signOut()
    }
}
