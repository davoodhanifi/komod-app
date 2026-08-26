package com.komod.api.core.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Shared signal for "a backend call just failed with PlanLimitExceeded — after showing
 * whatever message that screen already shows for it, send the user to the Paywall." Every
 * plan-gated ViewModel (AddItem, UploadReview, outfit generation, ...) calls [requestPaywall]
 * instead of holding a NavController or otherwise duplicating "navigate to Paywall" logic
 * itself; MainScaffold is the single place that observes [pendingPaywallRequest] and actually
 * calls `navController.navigate(Paywall)`.
 *
 * [pendingPaywallRequest] is a level, not a one-shot event stream: [requestPaywall] is
 * idempotent while a request is already pending, so several plan-limit errors firing in close
 * succession — even from different ViewModels — still result in exactly one Paywall
 * navigation. The observer calls [onPaywallShown] once it has actually navigated, which
 * re-arms it for a future request (e.g. the next time the user hits a limit after returning
 * from the Paywall without upgrading).
 */
interface PlanLimitNavigator {
    val pendingPaywallRequest: StateFlow<Boolean>
    fun requestPaywall()
    fun onPaywallShown()
}

class PlanLimitNavigatorImpl : PlanLimitNavigator {
    private val _pendingPaywallRequest = MutableStateFlow(false)
    override val pendingPaywallRequest: StateFlow<Boolean> = _pendingPaywallRequest.asStateFlow()

    override fun requestPaywall() {
        _pendingPaywallRequest.value = true
    }

    override fun onPaywallShown() {
        _pendingPaywallRequest.value = false
    }
}
