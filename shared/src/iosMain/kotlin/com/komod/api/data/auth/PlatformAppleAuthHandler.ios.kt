@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.komod.api.data.auth

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Apple
import io.github.jan.supabase.auth.providers.builtin.IDToken
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import platform.AuthenticationServices.ASAuthorization
import platform.AuthenticationServices.ASAuthorizationAppleIDCredential
import platform.AuthenticationServices.ASAuthorizationAppleIDProvider
import platform.AuthenticationServices.ASAuthorizationController
import platform.AuthenticationServices.ASAuthorizationControllerDelegateProtocol
import platform.AuthenticationServices.ASAuthorizationControllerPresentationContextProvidingProtocol
import platform.AuthenticationServices.ASAuthorizationErrorCanceled
import platform.AuthenticationServices.ASAuthorizationErrorDomain
import platform.AuthenticationServices.ASAuthorizationScopeEmail
import platform.AuthenticationServices.ASAuthorizationScopeFullName
import platform.AuthenticationServices.ASPresentationAnchor
import platform.CoreCrypto.CC_SHA256
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSUUID
import platform.UIKit.UIApplication
import platform.UIKit.UIWindow
import platform.darwin.NSObject
import platform.posix.memcpy
import kotlinx.serialization.json.put

internal actual class PlatformAppleAuthHandler actual constructor() {
    actual suspend fun signInWithApple(supabaseClient: SupabaseClient) {
        // Apple's expected nonce flow: the *hashed* nonce goes to Apple (embedded in the
        // identity token's `nonce` claim), while Supabase is given the original *raw* nonce
        // so it can hash it itself and verify it matches that claim.
        val rawNonce = NSUUID().UUIDString()
        val hashedNonce = sha256Hex(rawNonce)
        val authorization = requestAppleAuthorization(hashedNonce)
        supabaseClient.auth.signInWith(IDToken) {
            this.idToken = authorization.identityToken
            this.provider = Apple
            this.nonce = rawNonce
        }

        // Apple only ever hands back the user's name on their very first authorization for
        // this app — never on subsequent logins. Persist it into Supabase user_metadata now so
        // it's available going forward; when Apple doesn't provide one (every login after the
        // first, or if the user declined to share it), skip this entirely so whatever's already
        // on the profile — including nothing — is left exactly as it was.
        val fullName = authorization.fullName
        if (!fullName.isNullOrBlank()) {
            supabaseClient.auth.updateUser {
                data {
                    put("full_name", fullName)
                }
            }
        }
    }
}

private data class AppleAuthorization(
    val identityToken: String,
    val fullName: String?,
)

// Retains the delegate for the lifetime of the in-flight request — ASAuthorizationController
// only holds a *weak* reference to its delegate/presentationContextProvider, so nothing else
// keeps it alive while the native sheet is on screen.
private var activeAppleSignInDelegate: NSObject? = null

private suspend fun requestAppleAuthorization(hashedNonce: String): AppleAuthorization {
    return suspendCancellableCoroutine { continuation ->
        val request = ASAuthorizationAppleIDProvider().createRequest().apply {
            requestedScopes = listOf(ASAuthorizationScopeFullName, ASAuthorizationScopeEmail)
            this.nonce = hashedNonce
        }
        val controller = ASAuthorizationController(authorizationRequests = listOf(request))

        val delegate = object : NSObject(),
            ASAuthorizationControllerDelegateProtocol,
            ASAuthorizationControllerPresentationContextProvidingProtocol {

            override fun authorizationController(
                controller: ASAuthorizationController,
                didCompleteWithAuthorization: ASAuthorization,
            ) {
                activeAppleSignInDelegate = null
                val credential = didCompleteWithAuthorization.credential as? ASAuthorizationAppleIDCredential
                val token = credential?.identityToken?.toUtf8String()
                if (token != null) {
                    continuation.resume(
                        AppleAuthorization(identityToken = token, fullName = credential.composedFullName()),
                    )
                } else {
                    continuation.resumeWithException(
                        IllegalStateException("Apple sign-in did not return an identity token."),
                    )
                }
            }

            override fun authorizationController(
                controller: ASAuthorizationController,
                didCompleteWithError: NSError,
            ) {
                activeAppleSignInDelegate = null
                if (didCompleteWithError.domain == ASAuthorizationErrorDomain &&
                    didCompleteWithError.code.toInt() == ASAuthorizationErrorCanceled.toInt()
                ) {
                    continuation.resumeWithException(AppleSignInCancelledException())
                } else {
                    continuation.resumeWithException(
                        IllegalStateException(didCompleteWithError.localizedDescription),
                    )
                }
            }

            override fun presentationAnchorForAuthorizationController(
                controller: ASAuthorizationController,
            ): ASPresentationAnchor {
                return keyWindow()
            }
        }

        activeAppleSignInDelegate = delegate
        controller.delegate = delegate
        controller.presentationContextProvider = delegate
        controller.performRequests()
    }
}

private fun keyWindow(): UIWindow {
    val windows = UIApplication.sharedApplication.windows
    val keyWindow = windows.firstOrNull { (it as? UIWindow)?.isKeyWindow() == true } as? UIWindow
    return keyWindow ?: UIApplication.sharedApplication.keyWindow as UIWindow
}

// Apple only populates this on the user's very first authorization for this app; on every
// later login `fullName` is either absent or an empty NSPersonNameComponents, so this
// legitimately (and expectedly) returns null in that case.
private fun ASAuthorizationAppleIDCredential.composedFullName(): String? {
    val components = fullName ?: return null
    return composeAppleFullName(components.givenName, components.familyName)
}

// Split out from the extension above so it's testable directly with plain strings —
// ASAuthorizationAppleIDCredential/NSPersonNameComponents can't be constructed in a test
// (Apple disables their initializers outside a real authorization response).
internal fun composeAppleFullName(givenName: String?, familyName: String?): String? {
    val given = givenName?.trim().orEmpty()
    val family = familyName?.trim().orEmpty()
    return listOf(given, family).filter { it.isNotEmpty() }.joinToString(" ").ifBlank { null }
}

private fun NSData.toUtf8String(): String? {
    val size = length.toInt()
    if (size == 0) return null
    val bytes = ByteArray(size).also { array ->
        array.usePinned { pinned ->
            memcpy(pinned.addressOf(0), bytes, length)
        }
    }
    return bytes.decodeToString()
}

private const val Sha256DigestLength = 32

internal fun sha256Hex(input: String): String {
    val inputBytes = input.encodeToByteArray()
    val digest = UByteArray(Sha256DigestLength)
    inputBytes.usePinned { pinnedInput ->
        digest.usePinned { pinnedDigest ->
            CC_SHA256(pinnedInput.addressOf(0), inputBytes.size.convert(), pinnedDigest.addressOf(0))
        }
    }
    return digest.toHexString()
}

private fun UByteArray.toHexString(): String {
    val hexChars = "0123456789abcdef"
    val result = StringBuilder(size * 2)
    for (byte in this) {
        val value = byte.toInt()
        result.append(hexChars[value shr 4])
        result.append(hexChars[value and 0x0F])
    }
    return result.toString()
}
