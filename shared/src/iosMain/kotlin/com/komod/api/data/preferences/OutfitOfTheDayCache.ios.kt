package com.komod.api.data.preferences

import platform.Foundation.NSUserDefaults

actual class OutfitOfTheDayCache {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun getRawValue(): String? = defaults.stringForKey(KEY_ENTRY)

    actual fun setRawValue(value: String) {
        defaults.setObject(value, forKey = KEY_ENTRY)
    }

    private companion object {
        const val KEY_ENTRY = "outfit_of_the_day_entry"
    }
}
