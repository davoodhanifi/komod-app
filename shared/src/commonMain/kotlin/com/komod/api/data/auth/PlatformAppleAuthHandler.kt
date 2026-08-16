package com.komod.api.data.auth

import io.github.jan.supabase.SupabaseClient

internal expect class PlatformAppleAuthHandler() {
    suspend fun signInWithApple(supabaseClient: SupabaseClient)
}

/** Thrown when the user dismisses Apple's native sign-in sheet without completing it. */
class AppleSignInCancelledException : Exception("Apple sign-in was cancelled.")
