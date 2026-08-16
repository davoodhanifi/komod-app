package com.komod.api.data.auth

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Apple

internal actual class PlatformAppleAuthHandler actual constructor() {
    actual suspend fun signInWithApple(supabaseClient: SupabaseClient) {
        supabaseClient.auth.signInWith(Apple)
    }
}
