package com.komod.api.presentation.paywall

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.komod.api.core.error.ErrorContext
import com.komod.api.core.error.ErrorMapper
import com.komod.api.data.repository.BillingRepository
import com.komod.api.data.repository.SubscriptionSyncRepository
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
    private val subscriptionSyncRepository: SubscriptionSyncRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PaywallUiState())
    val uiState: StateFlow<PaywallUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<PaywallEffect>()
    val effects: SharedFlow<PaywallEffect> = _effects.asSharedFlow()

    init {
        // Adopt whatever the shared repository already knows (e.g. synced moments ago from
        // another screen) immediately, rather than starting blank until this screen's own
        // sync completes.
        subscriptionSyncRepository.subscriptionState.value?.let { state ->
            _uiState.update { it.copy(subscriptionState = state) }
        }
        loadPlans()
    }

    // Called on init and again whenever the screen resumes (see PaywallScreen's onResume
    // observer) — mirrors ProfileViewModel's "refresh on visit" pattern, so returning to the
    // paywall (e.g. after finishing a purchase in the system sheet, or from Settings) always
    // reflects RevenueCat's latest offerings rather than what was true on open.
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
        }
        refreshSubscriptionState()
    }

    // The foreground/initial-load refresh (requirement: app foreground refreshes RevenueCat's
    // CustomerInfo, then syncs the backend, then updates the displayed plan from that
    // response). Deliberately silent on failure here — this is a routine background refresh,
    // not the "a purchase/restore just succeeded" path, so a transient failure just leaves the
    // last-known synced state on screen instead of surfacing an error for something the user
    // didn't just take an action to trigger.
    fun refreshSubscriptionState() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            runCatching {
                billingRepository.refreshCustomerInfo()
                subscriptionSyncRepository.sync()
            }
                .onSuccess { state ->
                    _uiState.update { it.copy(isSyncing = false, subscriptionState = state, syncFailedMessage = null) }
                }
                .onFailure {
                    _uiState.update { it.copy(isSyncing = false) }
                }
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
            _uiState.update { it.copy(isPurchasing = true, syncFailedMessage = null) }
            runCatching { billingRepository.purchase(rcPackage) }
                .onSuccess {
                    _uiState.update { it.copy(isPurchasing = false) }
                    // RevenueCat's purchase has already succeeded at this point — everything
                    // from here on is about syncing the backend's Komod plan to match it. A
                    // failure below must never read to the user as "your purchase failed".
                    syncAfterEntitlementChange(onSynced = { _effects.emit(PaywallEffect.PurchaseCompleted) })
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
            _uiState.update { it.copy(isRestoring = true, syncFailedMessage = null) }
            runCatching { billingRepository.restorePurchases() }
                .onSuccess {
                    _uiState.update { it.copy(isRestoring = false) }
                    // Same reasoning as the purchase path above: the RevenueCat restore has
                    // already succeeded, so a sync failure here is never reported as "restore
                    // failed" — only that the plan display needs a retry.
                    syncAfterEntitlementChange(
                        onSynced = { _effects.emit(PaywallEffect.ShowSnackbar("Your purchases have been restored.")) },
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

    // Shared by purchaseSelectedPlan()/restorePurchases(): both need "refresh RevenueCat's
    // cache, sync the backend, adopt the response — or leave a retryable banner if the sync
    // itself fails" and differ only in what happens once that succeeds.
    private suspend fun syncAfterEntitlementChange(onSynced: suspend () -> Unit) {
        runCatching {
            billingRepository.refreshCustomerInfo()
            subscriptionSyncRepository.sync()
        }
            .onSuccess { state ->
                _uiState.update { it.copy(subscriptionState = state, syncFailedMessage = null) }
                onSynced()
            }
            .onFailure { error ->
                _uiState.update {
                    it.copy(
                        syncFailedMessage = ErrorMapper.toUserMessage(error, tag = "PaywallViewModel", context = ErrorContext.Paywall),
                    )
                }
            }
    }

    // Retries only the backend sync — never re-runs the purchase/restore, since RevenueCat's
    // side of it already succeeded.
    fun retrySync() {
        if (_uiState.value.isSyncing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            runCatching { subscriptionSyncRepository.sync() }
                .onSuccess { state ->
                    _uiState.update { it.copy(isSyncing = false, subscriptionState = state, syncFailedMessage = null) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isSyncing = false,
                            syncFailedMessage = ErrorMapper.toUserMessage(error, tag = "PaywallViewModel", context = ErrorContext.Paywall),
                        )
                    }
                }
        }
    }
}
