package com.komod.api.core.review

import com.komod.api.data.preferences.ReviewPromptPreferences
import com.komod.api.platform.AppReviewRequester
import kotlin.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Decides when to ask the user to rate Komod, and carries out that decision. ViewModels that
 * reach a "happy moment" (currently: a successful outfit generation, see OutfitViewModel) call
 * [recordPositiveEvent] and never touch anything store-related themselves; MainScaffold is the
 * single place that observes [pendingPromptRequest] and renders the custom two-step ask (see
 * RateAppDialog), the same "ViewModel requests, MainScaffold shows" split used for the Paywall
 * (PlanLimitNavigator).
 *
 * The two-step ask exists because a raw SKStoreReviewController/Play In-App Review popup can't
 * be aimed at only happy users — asking "Enjoying Komod?" first routes anyone who says no
 * towards feedback instead of a public 1-star review.
 */
interface ReviewPromptManager {
    val pendingPromptRequest: StateFlow<Boolean>
    fun recordPositiveEvent()
    fun onPromptDismissed()
    fun onUserRespondedEnjoying()
    fun onUserRespondedNotEnjoying()
}

class ReviewPromptManagerImpl(
    private val preferences: ReviewPromptPreferences,
    private val appReviewRequester: AppReviewRequester,
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : ReviewPromptManager {
    private val _pendingPromptRequest = MutableStateFlow(false)
    override val pendingPromptRequest: StateFlow<Boolean> = _pendingPromptRequest.asStateFlow()

    override fun recordPositiveEvent() {
        if (preferences.hasOptedOut() || _pendingPromptRequest.value) return

        val nowMillis = now()
        if (preferences.getFirstEventAtEpochMillis() <= 0L) {
            preferences.setFirstEventAtEpochMillis(nowMillis)
        }
        val updatedEventCount = preferences.getPositiveEventCount() + 1
        preferences.setPositiveEventCount(updatedEventCount)

        val shouldShow = shouldShowReviewPrompt(
            positiveEventCount = updatedEventCount,
            firstEventAtEpochMillis = preferences.getFirstEventAtEpochMillis(),
            lastPromptAtEpochMillis = preferences.getLastPromptAtEpochMillis(),
            promptCount = preferences.getPromptCount(),
            hasOptedOut = false,
            nowEpochMillis = nowMillis,
        )
        if (!shouldShow) return

        // Counted as "shown" here rather than from a separate onPromptShown() called by
        // MainScaffold once it actually renders the dialog — there's exactly one observer of
        // pendingPromptRequest, so there's no risk of counting a prompt that never displays.
        preferences.setPositiveEventCount(0)
        preferences.setLastPromptAtEpochMillis(nowMillis)
        preferences.setPromptCount(preferences.getPromptCount() + 1)
        _pendingPromptRequest.value = true
    }

    override fun onPromptDismissed() {
        _pendingPromptRequest.value = false
    }

    override fun onUserRespondedEnjoying() {
        _pendingPromptRequest.value = false
        // Assume a happy user who agreed to rate is satisfied; never ask again regardless of
        // whether the OS actually presented its own picker (that decision is out of our hands).
        preferences.setOptedOut(true)
        appReviewRequester.requestReview()
    }

    override fun onUserRespondedNotEnjoying() {
        _pendingPromptRequest.value = false
        // Deliberately not opted out forever: lastPromptAt/promptCount were already advanced
        // above, so the existing cooldown and MAX_LIFETIME_PROMPTS cap still apply and this
        // user simply won't be asked again for a long while.
    }
}
