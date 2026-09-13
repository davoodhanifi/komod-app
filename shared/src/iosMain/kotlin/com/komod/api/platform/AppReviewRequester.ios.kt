package com.komod.api.platform

import platform.Foundation.NSURL
import platform.StoreKit.SKStoreReviewController
import platform.UIKit.UIApplication
import platform.UIKit.UISceneActivationStateForegroundActive
import platform.UIKit.UIWindowScene
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

// Product page for Komod - Outfit Planner on the Dutch App Store storefront; Apple resolves
// this to the viewer's local storefront automatically.
private const val APP_STORE_URL = "https://apps.apple.com/nl/app/komod-outfit-planner/id6795283073?l=en-GB"

actual class AppReviewRequester {
    // requestReviewInScene: requires the foreground-active UIWindowScene — SwiftUI's
    // WindowGroup-based iOSApp.swift adopts the scene-based lifecycle by default even without
    // an explicit UIApplicationSceneManifest entry, so one is always present once the app has
    // launched. Silently does nothing if none can be found (e.g. called before the first
    // window attaches) rather than crashing.
    actual fun requestReview() {
        dispatch_async(dispatch_get_main_queue()) {
            val scene = UIApplication.sharedApplication.connectedScenes
                .filterIsInstance<UIWindowScene>()
                .firstOrNull { it.activationState == UISceneActivationStateForegroundActive }
            scene?.let(SKStoreReviewController::requestReviewInScene)
        }
    }

    actual fun openStoreListing() {
        val url = NSURL.URLWithString(APP_STORE_URL) ?: return
        // Same main-actor/dispatch reasoning as AppSettingsOpener.openAppSettings().
        dispatch_async(dispatch_get_main_queue()) {
            UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any>(), completionHandler = null)
        }
    }
}
