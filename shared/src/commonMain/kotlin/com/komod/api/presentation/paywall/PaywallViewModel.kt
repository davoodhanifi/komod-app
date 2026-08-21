package com.komod.api.presentation.paywall

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.komod.api.core.error.ErrorContext
import com.komod.api.core.error.ErrorMapper
import com.komod.api.data.repository.BillingRepository
import com.komod.api.domain.model.BillingPeriod
import com.komod.api.domain.model.SubscriptionPlan
import com.revenuecat.purchases.kmp.models.Package
import com.revenuecat.purchases.kmp.models.PurchasesTransactionException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PaywallViewModel(
    private val billingRepository: BillingRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PaywallUiState())
    val uiState: StateFlow<PaywallUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<PaywallEffect>()
    val effects: SharedFlow<PaywallEffect> = _effects.asSharedFlow()

    init {
        loadPlans()
    }

    // Called on init and again whenever the screen resumes (see PaywallScreen's onResume
    // observer) — mirrors ProfileViewModel's "refresh on visit" pattern, so returning to the
    // paywall (e.g. after finishing a purchase in the system sheet, or from Settings) always
    // reflects RevenueCat's latest offerings/entitlements rather than what was true on open.
    fun loadPlans() {
        viewModelScope.launch {
            if (_uiState.value.plansState !is PaywallPlansState.Success) {
                _uiState.update { it.copy(plansState = PaywallPlansState.Loading) }
            }
            runCatching { billingRepository.getPaywallPlans() }
                .onSuccess { plans ->
                    _uiState.update {
                        it.copy(
                            plansState = PaywallPlansState.Success(plans),
                            selectedPlan = it.selectedPlan ?: plans.firstOrNull()?.plan,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            plansState = PaywallPlansState.Error(
                                ErrorMapper.toUserMessage(error, tag = "PaywallViewModel", context = ErrorContext.Paywall),
                            ),
                        )
                    }
                }
            refreshActiveEntitlement()
        }
    }

    fun refreshActiveEntitlement() {
        viewModelScope.launch {
            // Best-effort background sync — loadPlans()'s failure path above already surfaces
            // the primary "couldn't load the paywall" error, so a failure here just leaves the
            // last known active plan on screen instead of erroring a second time.
            runCatching { billingRepository.getActivePlan() }
                .onSuccess { plan -> _uiState.update { it.copy(activeEntitlementPlan = plan) } }
        }
    }

    fun selectPlan(plan: SubscriptionPlan) {
        _uiState.update { it.copy(selectedPlan = plan) }
    }

    fun selectBillingPeriod(period: BillingPeriod) {
        _uiState.update { it.copy(billingPeriod = period) }
    }

    fun purchaseSelectedPlan() {
        val state = _uiState.value
        if (state.isPurchasing) return
        val plans = (state.plansState as? PaywallPlansState.Success)?.plans ?: return
        val selected = plans.find { it.plan == state.selectedPlan } ?: return
        val rcPackage: Package = when (state.billingPeriod) {
            BillingPeriod.Monthly -> selected.monthlyPackage
            BillingPeriod.Yearly -> selected.yearlyPackage
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isPurchasing = true) }
            runCatching { billingRepository.purchase(rcPackage) }
                .onSuccess { activePlan ->
                    _uiState.update { it.copy(isPurchasing = false, activeEntitlementPlan = activePlan) }
                    _effects.emit(PaywallEffect.PurchaseCompleted)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isPurchasing = false) }
                    // A user-cancelled purchase (backing out of the App Store sheet) isn't an
                    // error — stay silent so they can simply try again, rather than showing a
                    // snackbar for something the user chose to do.
                    if (error !is PurchasesTransactionException || !error.userCancelled) {
                        _effects.emit(
                            PaywallEffect.ShowSnackbar(
                                ErrorMapper.toUserMessage(error, tag = "PaywallViewModel", context = ErrorContext.Paywall),
                            ),
                        )
                    }
                }
        }
    }

    fun restorePurchases() {
        if (_uiState.value.isRestoring) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRestoring = true) }
            runCatching { billingRepository.restorePurchases() }
                .onSuccess { activePlan ->
                    _uiState.update { it.copy(isRestoring = false, activeEntitlementPlan = activePlan) }
                    _effects.emit(
                        PaywallEffect.ShowSnackbar(
                            if (activePlan != null) {
                                "Your purchases have been restored."
                            } else {
                                "No previous purchases found to restore."
                            },
                        ),
                    )
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isRestoring = false) }
                    _effects.emit(
                        PaywallEffect.ShowSnackbar(
                            ErrorMapper.toUserMessage(error, tag = "PaywallViewModel", context = ErrorContext.Paywall),
                        ),
                    )
                }
        }
    }
}
