package com.komod.api.core.review

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// shouldShowReviewPrompt() is extracted out of ReviewPromptManagerImpl specifically so this
// decision is unit-testable on its own — ReviewPromptManagerImpl itself can't be constructed in
// this JVM test target since ReviewPromptPreferences is a platform `expect class` with no
// fake-friendly interface (see OutfitViewModelPlanLimitTest for the same reasoning elsewhere).
class ReviewPromptPolicyTest {

    private val dayMillis = 24 * 60 * 60 * 1000L
    private val now = 1_000_000_000_000L // arbitrary fixed "now" for all scenarios below

    @Test
    fun `does not show before enough positive events`() {
        assertFalse(
            shouldShowReviewPrompt(
                positiveEventCount = MIN_POSITIVE_EVENTS_BEFORE_PROMPT - 1,
                firstEventAtEpochMillis = now - MIN_DAYS_SINCE_FIRST_EVENT * dayMillis,
                lastPromptAtEpochMillis = 0L,
                promptCount = 0,
                hasOptedOut = false,
                nowEpochMillis = now,
            ),
        )
    }

    @Test
    fun `does not show on the first event even if the threshold count is hit later the same moment`() {
        assertFalse(
            shouldShowReviewPrompt(
                positiveEventCount = MIN_POSITIVE_EVENTS_BEFORE_PROMPT,
                firstEventAtEpochMillis = now,
                lastPromptAtEpochMillis = 0L,
                promptCount = 0,
                hasOptedOut = false,
                nowEpochMillis = now,
            ),
        )
    }

    @Test
    fun `shows once enough positive events and enough days since install have both passed`() {
        assertTrue(
            shouldShowReviewPrompt(
                positiveEventCount = MIN_POSITIVE_EVENTS_BEFORE_PROMPT,
                firstEventAtEpochMillis = now - MIN_DAYS_SINCE_FIRST_EVENT * dayMillis,
                lastPromptAtEpochMillis = 0L,
                promptCount = 0,
                hasOptedOut = false,
                nowEpochMillis = now,
            ),
        )
    }

    @Test
    fun `stays silent during the cooldown after a previous prompt`() {
        assertFalse(
            shouldShowReviewPrompt(
                positiveEventCount = MIN_POSITIVE_EVENTS_BEFORE_PROMPT,
                firstEventAtEpochMillis = now - 365 * dayMillis,
                lastPromptAtEpochMillis = now - (COOLDOWN_DAYS_BETWEEN_PROMPTS - 1) * dayMillis,
                promptCount = 1,
                hasOptedOut = false,
                nowEpochMillis = now,
            ),
        )
    }

    @Test
    fun `shows again once the cooldown has fully elapsed`() {
        assertTrue(
            shouldShowReviewPrompt(
                positiveEventCount = MIN_POSITIVE_EVENTS_BEFORE_PROMPT,
                firstEventAtEpochMillis = now - 365 * dayMillis,
                lastPromptAtEpochMillis = now - (COOLDOWN_DAYS_BETWEEN_PROMPTS + 1) * dayMillis,
                promptCount = 1,
                hasOptedOut = false,
                nowEpochMillis = now,
            ),
        )
    }

    @Test
    fun `never shows again once the lifetime prompt cap is reached`() {
        assertFalse(
            shouldShowReviewPrompt(
                positiveEventCount = MIN_POSITIVE_EVENTS_BEFORE_PROMPT,
                firstEventAtEpochMillis = now - 365 * dayMillis,
                lastPromptAtEpochMillis = now - (COOLDOWN_DAYS_BETWEEN_PROMPTS + 1) * dayMillis,
                promptCount = MAX_LIFETIME_PROMPTS,
                hasOptedOut = false,
                nowEpochMillis = now,
            ),
        )
    }

    @Test
    fun `never shows once the user has opted out`() {
        assertFalse(
            shouldShowReviewPrompt(
                positiveEventCount = MIN_POSITIVE_EVENTS_BEFORE_PROMPT,
                firstEventAtEpochMillis = now - 365 * dayMillis,
                lastPromptAtEpochMillis = 0L,
                promptCount = 0,
                hasOptedOut = true,
                nowEpochMillis = now,
            ),
        )
    }
}
