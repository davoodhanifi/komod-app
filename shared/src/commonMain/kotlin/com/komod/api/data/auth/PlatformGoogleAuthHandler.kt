package com.komod.api.data.auth

import io.github.jan.supabase.SupabaseClient

internal expect class PlatformGoogleAuthHandler() {
    suspend fun signInWithGoogle(supabaseClient: SupabaseClient)
}
