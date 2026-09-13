package com.komod.api.data.preferences

import platform.Foundation.NSUserDefaults

actual class ReviewPromptPreferences {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun getPositiveEventCount(): Int = defaults.integerForKey(KEY_POSITIVE_EVENT_COUNT).toInt()
    actual fun setPositiveEventCount(count: Int) {
        defaults.setInteger(count.toLong(), forKey = KEY_POSITIVE_EVENT_COUNT)
    }

    actual fun getPromptCount(): Int = defaults.integerForKey(KEY_PROMPT_COUNT).toInt()
    actual fun setPromptCount(count: Int) {
        defaults.setInteger(count.toLong(), forKey = KEY_PROMPT_COUNT)
    }

    actual fun getFirstEventAtEpochMillis(): Long = defaults.integerForKey(KEY_FIRST_EVENT_AT)
    actual fun setFirstEventAtEpochMillis(epochMillis: Long) {
        defaults.setInteger(epochMillis, forKey = KEY_FIRST_EVENT_AT)
    }

    actual fun getLastPromptAtEpochMillis(): Long = defaults.integerForKey(KEY_LAST_PROMPT_AT)
    actual fun setLastPromptAtEpochMillis(epochMillis: Long) {
        defaults.setInteger(epochMillis, forKey = KEY_LAST_PROMPT_AT)
    }

    actual fun hasOptedOut(): Boolean = defaults.boolForKey(KEY_OPTED_OUT)
    actual fun setOptedOut(optedOut: Boolean) {
        defaults.setBool(optedOut, forKey = KEY_OPTED_OUT)
    }

    private companion object {
        const val KEY_POSITIVE_EVENT_COUNT = "review_positive_event_count"
        const val KEY_PROMPT_COUNT = "review_prompt_count"
        const val KEY_FIRST_EVENT_AT = "review_first_event_at"
        const val KEY_LAST_PROMPT_AT = "review_last_prompt_at"
        const val KEY_OPTED_OUT = "review_opted_out"
    }
}
