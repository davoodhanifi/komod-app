package com.komod.api.data.preferences

expect class WeatherPreferences() {
    fun isEnabled(): Boolean
    fun setEnabled(enabled: Boolean)
}
