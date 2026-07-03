package com.komod.api

/**
 * Runtime configuration container.
 * Credentials (Supabase URL/key) are baked in at build time via BuildKonfig.
 * Extend this class for any runtime-only settings (e.g. feature flags).
 */
object AppConfig
