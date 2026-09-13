package com.komod.api.data.preferences

import android.content.Context
import com.komod.api.platform.AndroidAppContextHolder

actual class ReviewPromptPreferences {
    private val prefs = AndroidAppContextHolder.context.getSharedPreferences("review_prompt_prefs", Context.MODE_PRIVATE)

    actual fun getPositiveEventCount(): Int = prefs.getInt(KEY_POSITIVE_EVENT_COUNT, 0)
    actual fun setPositiveEventCount(count: Int) {
        prefs.edit().putInt(KEY_POSITIVE_EVENT_COUNT, count).apply()
    }

    actual fun getPromptCount(): Int = prefs.getInt(KEY_PROMPT_COUNT, 0)
    actual fun setPromptCount(count: Int) {
        prefs.edit().putInt(KEY_PROMPT_COUNT, count).apply()
    }

    actual fun getFirstEventAtEpochMillis(): Long = prefs.getLong(KEY_FIRST_EVENT_AT, 0L)
    actual fun setFirstEventAtEpochMillis(epochMillis: Long) {
        prefs.edit().putLong(KEY_FIRST_EVENT_AT, epochMillis).apply()
    }

    actual fun getLastPromptAtEpochMillis(): Long = prefs.getLong(KEY_LAST_PROMPT_AT, 0L)
    actual fun setLastPromptAtEpochMillis(epochMillis: Long) {
        prefs.edit().putLong(KEY_LAST_PROMPT_AT, epochMillis).apply()
    }

    actual fun hasOptedOut(): Boolean = prefs.getBoolean(KEY_OPTED_OUT, false)
    actual fun setOptedOut(optedOut: Boolean) {
        prefs.edit().putBoolean(KEY_OPTED_OUT, optedOut).apply()
    }

    private companion object {
        const val KEY_POSITIVE_EVENT_COUNT = "positive_event_count"
        const val KEY_PROMPT_COUNT = "prompt_count"
        const val KEY_FIRST_EVENT_AT = "first_event_at"
        const val KEY_LAST_PROMPT_AT = "last_prompt_at"
        const val KEY_OPTED_OUT = "opted_out"
    }
}
