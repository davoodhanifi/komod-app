package com.komod.api.platform

import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

actual class AppSettingsOpener {
    actual fun openAppSettings() {
        val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
        // The single-arg openURL: is deprecated since iOS 10 and unreliable on recent iOS
        // versions; openURL:options:completionHandler: is the supported replacement. It's also
        // main-actor-bound, so dispatch explicitly in case this is ever called off the main
        // thread.
        dispatch_async(dispatch_get_main_queue()) {
            UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any>(), completionHandler = null)
        }
    }
}
