package com.komod.api.platform

import android.app.Activity

// The Play In-App Review API's launchReviewFlow() needs a foreground Activity, unlike every
// other platform call in this app which only needs the Application Context held by
// AndroidAppContextHolder. KomodApplication keeps this updated via
// registerActivityLifecycleCallbacks so AppReviewRequester (a plain no-arg Koin single, like
// AppSettingsOpener) can reach it without every caller having to thread an Activity through.
object AndroidCurrentActivityHolder {
    var activity: Activity? = null
}
