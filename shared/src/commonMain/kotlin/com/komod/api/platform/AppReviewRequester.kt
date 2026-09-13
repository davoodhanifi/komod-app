package com.komod.api.platform

/**
 * Wraps each platform's native "rate this app" mechanism. [requestReview] asks the OS to show
 * its in-app review sheet (SKStoreReviewController on iOS, the Play In-App Review API on
 * Android) — both platforms own the actual decision of whether anything is shown at all and
 * silently no-op once their own yearly/quota limits are hit, so callers must never assume it
 * appeared. [openStoreListing] is the guaranteed fallback: it deep-links straight to the app's
 * store page, for a user-initiated "Rate us" entry point (see ProfileScreen) where silently
 * doing nothing would be a bad experience.
 */
expect class AppReviewRequester() {
    fun requestReview()
    fun openStoreListing()
}
