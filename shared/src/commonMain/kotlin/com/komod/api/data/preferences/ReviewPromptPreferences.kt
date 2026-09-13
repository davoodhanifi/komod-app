package com.komod.api.data.preferences

/**
 * Raw persisted counters behind the "ask for a store review" gate — see
 * com.komod.api.core.review.shouldShowReviewPrompt for how they're interpreted. This class only
 * stores and retrieves them; it makes no decisions of its own, so it stays a thin
 * SharedPreferences/NSUserDefaults wrapper like WeatherPreferences rather than something that
 * needs its own tests.
 */
expect class ReviewPromptPreferences() {
    fun getPositiveEventCount(): Int
    fun setPositiveEventCount(count: Int)
    fun getPromptCount(): Int
    fun setPromptCount(count: Int)
    fun getFirstEventAtEpochMillis(): Long
    fun setFirstEventAtEpochMillis(epochMillis: Long)
    fun getLastPromptAtEpochMillis(): Long
    fun setLastPromptAtEpochMillis(epochMillis: Long)
    fun hasOptedOut(): Boolean
    fun setOptedOut(optedOut: Boolean)
}
