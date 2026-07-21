package com.komod.api.data.preferences

import android.content.Context
import com.komod.api.platform.AndroidAppContextHolder

actual class WeatherPreferences {
    private val prefs = AndroidAppContextHolder.context.getSharedPreferences("weather_prefs", Context.MODE_PRIVATE)

    actual fun isEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, false)

    actual fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    private companion object {
        const val KEY_ENABLED = "use_weather"
    }
}
