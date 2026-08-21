package com.komod.api.presentation.paywall

import com.komod.api.domain.model.BillingPeriod
import com.komod.api.domain.model.PaywallPlan
import com.komod.api.domain.model.SubscriptionPlan

data class PaywallUiState(
    val plansState: PaywallPlansState = PaywallPlansState.Loading,
    val billingPeriod: BillingPeriod = BillingPeriod.Monthly,
    val selectedPlan: SubscriptionPlan? = null,
    // Sourced from RevenueCat's CustomerInfo (see BillingRepository.getActivePlan) — never
    // set locally just because a purchase call returned successfully.
    val activeEntitlementPlan: SubscriptionPlan? = null,
    val isPurchasing: Boolean = false,
    val isRestoring: Boolean = false,
)

sealed interface PaywallPlansState {
    data object Loading : PaywallPlansState
    data class Success(val plans: List<PaywallPlan>) : PaywallPlansState
    data class Error(val message: String) : PaywallPlansState
}
