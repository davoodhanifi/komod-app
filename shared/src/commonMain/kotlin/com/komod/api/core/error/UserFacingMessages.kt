package com.komod.api.core.error

/**
 * The full set of pre-approved strings [ErrorMapper] is allowed to return. Nothing
 * derived from a throwable (message, class name, HTTP status, response body) is ever
 * shown to the user — only copy from this object or a context's [ErrorContext.fallbackMessage].
 */
object UserFacingMessages {
    const val Generic = "Something went wrong. Please try again."
    const val NoInternet = "No internet connection. Please check your connection and try again."
    const val Timeout = "The request took too long. Please try again."
    const val SessionExpired = "Your session has expired. Please sign in again."
    const val UploadFailed = "We couldn't upload your photo. Please try again."
    const val AiProcessingFailed = "We couldn't process your photo. Please try another one."
}
