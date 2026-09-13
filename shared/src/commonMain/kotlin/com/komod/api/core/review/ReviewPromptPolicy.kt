package com.komod.api.core.review

// The "smart" part of the review prompt: only ever surface it after the user has already had
// a few good moments with the app (not on their very first success), never within days of
// installing, throttle repeats generously since the OS-level pickers (SKStoreReviewController,
// Play In-App Review) apply their own quotas on top of this one, and give up for good after a
// handful of asks so an unhappy-but-not-uninstalling user isn't nagged forever.
internal const val MIN_POSITIVE_EVENTS_BEFORE_PROMPT = 3
internal const val MIN_DAYS_SINCE_FIRST_EVENT = 2
internal const val COOLDOWN_DAYS_BETWEEN_PROMPTS = 60
internal const val MAX_LIFETIME_PROMPTS = 4

private const val DAY_MILLIS = 24 * 60 * 60 * 1000L

// Extracted out of ReviewPromptManagerImpl so this decision is unit-testable on its own,
// independent of the platform-bound ReviewPromptPreferences (an expect class with no
// fake-friendly interface, same reasoning as shouldRecheckWeatherPermission/
// detectPlanLimitError in OutfitViewModel).
internal fun shouldShowReviewPrompt(
    positiveEventCount: Int,
    firstEventAtEpochMillis: Long,
    lastPromptAtEpochMillis: Long,
    promptCount: Int,
    hasOptedOut: Boolean,
    nowEpochMillis: Long,
): Boolean {
    if (hasOptedOut) return false
    if (promptCount >= MAX_LIFETIME_PROMPTS) return false
    if (positiveEventCount < MIN_POSITIVE_EVENTS_BEFORE_PROMPT) return false
    if (firstEventAtEpochMillis <= 0L) return false

    val daysSinceFirstEvent = (nowEpochMillis - firstEventAtEpochMillis) / DAY_MILLIS
    if (daysSinceFirstEvent < MIN_DAYS_SINCE_FIRST_EVENT) return false

    if (lastPromptAtEpochMillis > 0L) {
        val daysSinceLastPrompt = (nowEpochMillis - lastPromptAtEpochMillis) / DAY_MILLIS
        if (daysSinceLastPrompt < COOLDOWN_DAYS_BETWEEN_PROMPTS) return false
    }

    return true
}
