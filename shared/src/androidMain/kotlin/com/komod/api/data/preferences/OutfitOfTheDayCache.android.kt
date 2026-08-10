package com.komod.api.data.preferences

import android.content.Context
import com.komod.api.platform.AndroidAppContextHolder

actual class OutfitOfTheDayCache {
    private val prefs = AndroidAppContextHolder.context.getSharedPreferences("outfit_of_the_day_prefs", Context.MODE_PRIVATE)

    actual fun getRawValue(): String? = prefs.getString(KEY_ENTRY, null)

    actual fun setRawValue(value: String) {
        prefs.edit().putString(KEY_ENTRY, value).apply()
    }

    private companion object {
        const val KEY_ENTRY = "outfit_of_the_day_entry"
    }
}
