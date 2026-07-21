package com.komod.api.data.preferences

import platform.Foundation.NSUserDefaults

actual class WeatherPreferences {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun isEnabled(): Boolean = defaults.boolForKey(KEY_ENABLED)

    actual fun setEnabled(enabled: Boolean) {
        defaults.setBool(enabled, forKey = KEY_ENABLED)
    }

    private companion object {
        const val KEY_ENABLED = "use_weather"
    }
}
