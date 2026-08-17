package com.komod.api.data.repository

import io.github.jan.supabase.auth.user.Identity
import io.github.jan.supabase.auth.user.UserInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private fun appleIdentity(fullName: String? = null): Identity {
    val identityData = buildJsonObject {
        if (fullName != null) put("full_name", fullName)
    }
    return Identity(
        id = "identity-apple",
        identityData = identityData,
        provider = "apple",
        userId = "user-1",
    )
}

private fun googleIdentity(): Identity {
    return Identity(
        id = "identity-google",
        identityData = JsonObject(emptyMap()),
        provider = "google",
        userId = "user-1",
    )
}

private fun userInfo(
    email: String?,
    userMetadata: JsonObject? = null,
    identities: List<Identity>? = null,
): UserInfo {
    return UserInfo(
        aud = "authenticated",
        id = "user-1",
        email = email,
        userMetadata = userMetadata,
        identities = identities,
    )
}

class AuthRepositoryImplTest {

    @Test
    fun `Apple login with a full_name in metadata uses that as the display name`() {
        // Simulates the state right after PlatformAppleAuthHandler persists Apple's name via
        // updateUser on a first authorization (or a later login reading that persisted value).
        val info = userInfo(
            email = "abc123xyz@privaterelay.appleid.com",
            userMetadata = buildJsonObject { put("full_name", "Jane Appleseed") },
            identities = listOf(appleIdentity(fullName = "Jane Appleseed")),
        )

        val user = buildUser(info)

        assertEquals("Jane Appleseed", user.displayName)
    }

    @Test
    fun `Apple login without a name in metadata does not invent one from the email`() {
        // No full_name anywhere (never captured, or Apple didn't provide one this time) — must
        // not fall back to the private-relay email's local part, which reads as a random string.
        val info = userInfo(
            email = "abc123xyz@privaterelay.appleid.com",
            identities = listOf(appleIdentity(fullName = null)),
        )

        val user = buildUser(info)

        assertNull(user.displayName)
    }

    @Test
    fun `Apple login with a real email still does not invent a name from it`() {
        val info = userInfo(
            email = "real.person@icloud.com",
            identities = listOf(appleIdentity(fullName = null)),
        )

        val user = buildUser(info)

        assertNull(user.displayName)
    }

    @Test
    fun `a previously persisted Apple name survives a later login where Apple sends no name`() {
        // Apple only ever sends fullName on the very first authorization. On every subsequent
        // login the identity payload itself has no name, but the name persisted into
        // user_metadata from that first login is still there and must keep winning.
        val info = userInfo(
            email = "abc123xyz@privaterelay.appleid.com",
            userMetadata = buildJsonObject { put("full_name", "Jane Appleseed") },
            identities = listOf(appleIdentity(fullName = null)),
        )

        val user = buildUser(info)

        assertEquals("Jane Appleseed", user.displayName)
    }

    @Test
    fun `non-Apple sign-in still falls back to the email's local part exactly as before`() {
        val info = userInfo(
            email = "jane@gmail.com",
            identities = listOf(googleIdentity()),
        )

        val user = buildUser(info)

        assertEquals("jane", user.displayName)
    }

    @Test
    fun `non-Apple sign-in with no identities at all still falls back to the email`() {
        // e.g. plain email/password auth, which has no `identities` entries.
        val info = userInfo(email = "jane@gmail.com", identities = null)

        val user = buildUser(info)

        assertEquals("jane", user.displayName)
    }

    @Test
    fun `Apple users never get a photoUrl invented from nowhere`() {
        val info = userInfo(
            email = "abc123xyz@privaterelay.appleid.com",
            identities = listOf(appleIdentity()),
        )

        val user = buildUser(info)

        assertNull(user.photoUrl)
    }
}
