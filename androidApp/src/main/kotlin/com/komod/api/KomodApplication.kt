package com.komod.api

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.komod.api.di.appModule
import com.komod.api.platform.AndroidAppContextHolder
import com.komod.api.platform.AndroidCurrentActivityHolder
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class KomodApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidAppContextHolder.context = this
        registerActivityLifecycleCallbacks(CurrentActivityTracker)
        startKoin {
            androidContext(this@KomodApplication)
            modules(appModule())
        }
    }
}

// Tracks the topmost resumed Activity into AndroidCurrentActivityHolder — see its kdoc for why
// that's needed (the Play In-App Review flow requires one). Cleared on pause rather than
// destroy so a backgrounded-but-not-yet-destroyed Activity is never mistaken for the active one.
private object CurrentActivityTracker : Application.ActivityLifecycleCallbacks {
    override fun onActivityResumed(activity: Activity) {
        AndroidCurrentActivityHolder.activity = activity
    }

    override fun onActivityPaused(activity: Activity) {
        if (AndroidCurrentActivityHolder.activity === activity) {
            AndroidCurrentActivityHolder.activity = null
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
