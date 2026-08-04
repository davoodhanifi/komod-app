package com.komod.api.core.error

/**
 * Fallback copy for a throwable [ErrorMapper] doesn't recognize as a specific
 * (network/timeout/session-expiry/etc.) case. Callers pick the context that best
 * describes the operation that failed; the mapper still checks for the more
 * specific cases first regardless of context.
 */
enum class ErrorContext(internal val fallbackMessage: String) {
    Generic(UserFacingMessages.Generic),
    Upload(UserFacingMessages.UploadFailed),
    AiProcessing(UserFacingMessages.AiProcessingFailed),
    Auth("Sign-in failed. Please try again."),
    Weather("Unable to load weather right now."),
}
