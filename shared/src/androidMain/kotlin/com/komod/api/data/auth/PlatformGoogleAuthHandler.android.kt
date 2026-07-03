package com.komod.api.data.auth

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google

internal actual class PlatformGoogleAuthHandler actual constructor() {
    actual suspend fun signInWithGoogle(supabaseClient: SupabaseClient) {
        supabaseClient.auth.signInWith(Google)
    }
}
