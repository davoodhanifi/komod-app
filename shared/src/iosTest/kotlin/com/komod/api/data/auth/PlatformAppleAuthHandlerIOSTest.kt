package com.komod.api.data.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

// Verifies the Apple Sign In nonce flow: the raw nonce sent to Supabase must NOT be the same
// value handed to Apple — Apple gets a SHA-256 hash of it (embedded in the identity token's
// `nonce` claim), while Supabase re-hashes the raw value itself to verify that claim.
class PlatformAppleAuthHandlerIOSTest {

    @Test
    fun `sha256Hex matches the known NIST test vector for 'abc'`() {
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            sha256Hex("abc"),
        )
    }

    @Test
    fun `hashing the same raw nonce twice produces the same hash`() {
        val rawNonce = "9E3F1B2A-1234-4EEE-8B77-000000000000"
        assertEquals(sha256Hex(rawNonce), sha256Hex(rawNonce))
    }

    @Test
    fun `the hashed nonce sent to Apple must differ from the raw nonce sent to Supabase`() {
        val rawNonce = "9E3F1B2A-1234-4EEE-8B77-000000000000"
        assertNotEquals(rawNonce, sha256Hex(rawNonce))
    }

    // composeAppleFullName: Apple only ever includes fullName on the very first authorization —
    // every login after that, ASAuthorizationAppleIDCredential.fullName is either absent or an
    // empty NSPersonNameComponents. These verify the first-login case combines given+family
    // correctly, and that the "nothing to show" cases all resolve to null rather than a blank
    // or partial string, so callers never persist garbage into user_metadata.

    @Test
    fun `composeAppleFullName joins a given and family name`() {
        assertEquals("Jane Appleseed", composeAppleFullName("Jane", "Appleseed"))
    }

    @Test
    fun `composeAppleFullName trims whitespace around each part`() {
        assertEquals("Jane Appleseed", composeAppleFullName("  Jane  ", "  Appleseed  "))
    }

    @Test
    fun `composeAppleFullName uses whichever single part is present`() {
        assertEquals("Jane", composeAppleFullName("Jane", null))
        assertEquals("Appleseed", composeAppleFullName(null, "Appleseed"))
    }

    @Test
    fun `composeAppleFullName returns null when both parts are null`() {
        assertNull(composeAppleFullName(null, null))
    }

    @Test
    fun `composeAppleFullName returns null when both parts are blank`() {
        // The empty-but-present NSPersonNameComponents case Apple sends on later logins.
        assertNull(composeAppleFullName("", "  "))
    }
}
