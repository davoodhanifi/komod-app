package com.komod.api.presentation.paywall

import com.komod.api.domain.model.BillingPeriod
import com.komod.api.domain.model.KomodSubscriptionState
import com.komod.api.domain.model.PaywallPlan
import com.komod.api.domain.model.SubscriptionPlan

data class PaywallUiState(
    val plansState: PaywallPlansState = PaywallPlansState.Loading,
    val billingPeriod: BillingPeriod = BillingPeriod.Monthly,
    val selectedPlan: SubscriptionPlan? = null,
    // The backend's synchronized Komod subscription state (POST /subscription/sync) — the
    // sole source of truth for "what plan is the user on right now", including a cancelled
    // plan that stays active until the backend itself reports it Expired. Never set from a
    // RevenueCat purchase/restore/entitlement check locally. Null until the first sync
    // completes.
    val subscriptionState: KomodSubscriptionState? = null,
    val isPurchasing: Boolean = false,
    val isRestoring: Boolean = false,
    val isSyncing: Boolean = false,
    // Set when a purchase or restore succeeded on RevenueCat but the backend sync that must
    // follow it failed. The App Store purchase itself is never lost or reported as failed —
    // this only means the displayed Komod plan hasn't caught up yet, and the user can retry
    // the sync via retrySync().
    val syncFailedMessage: String? = null,
)

sealed interface PaywallPlansState {
    data object Loading : PaywallPlansState
    data class Success(val plans: List<PaywallPlan>) : PaywallPlansState
    data class Error(val message: String) : PaywallPlansState
}
