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

    // Sticky once the user taps a plan card — after that, neither a fresh offering fetch nor a
    // subscription re-sync (e.g. on ON_RESUME) is allowed to move their selection out from under
    // them via applyDefaultSelection().
    private var hasUserSelectedPlan = false

    // RevenueCat's activeSubscriptions (App Store/Play product IDs), refreshed alongside the
    // backend sync in refreshSubscriptionState() — the source of truth for
    // applyDefaultSelection()'s auto-select match. Null until that first refresh completes
    // (whether it succeeds or fails); never derived from KomodSubscriptionState, which stays
    // the sole source of truth for the "Current" badge/entitlements elsewhere on this screen.
    private var activeProductIdentifiers: Set<String>? = null

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
                    _uiState.update { it.copy(plansState = PaywallPlansState.Success(plans)) }
                    applyDefaultSelection()
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
                // Captured as a side effect regardless of what happens to sync() below — this is
                // RevenueCat's own state, no reason to let a backend hiccup throw it away.
                activeProductIdentifiers = billingRepository.getActiveProductIdentifiers()
                subscriptionSyncRepository.sync()
            }
                .onSuccess { state ->
                    _uiState.update { it.copy(isSyncing = false, subscriptionState = state, syncFailedMessage = null) }
                }
                .onFailure {
                    _uiState.update { it.copy(isSyncing = false) }
                    // getActiveProductIdentifiers() itself may be what threw, in which case
                    // activeProductIdentifiers was never assigned above — fall back to "none
                    // known" so applyDefaultSelection() below can still resolve to the
                    // pre-existing default instead of waiting forever.
                    if (activeProductIdentifiers == null) activeProductIdentifiers = emptySet()
                }
            applyDefaultSelection()
        }
    }

    // Auto-selects the plan the user is actively subscribed to per RevenueCat's own CustomerInfo
    // (never KomodSubscriptionState — that stays the sole source of truth for the "Current"
    // badge/entitlements elsewhere on this screen) by matching activeProductIdentifiers'
    // App Store/Play product IDs against each loaded plan's monthly/yearly StoreProduct ID —
    // RevenueCat's own recommended way to check "is the customer subscribed to product X".
    // Waits for both the offering and CustomerInfo to be known (called from both completions,
    // since either can finish last) rather than guessing early, so there's no wrong-then-correct
    // flicker — nothing is auto-selected until both are in. Falls back to the first plan (the
    // pre-existing default) when there's no active subscription or the active one isn't part of
    // the currently loaded offering. A no-op once the user has picked a plan themselves.
    private fun applyDefaultSelection() {
        if (hasUserSelectedPlan) return
        val activeIds = activeProductIdentifiers ?: return
        _uiState.update { state ->
            val plans = (state.plansState as? PaywallPlansState.Success)?.plans ?: return@update state
            val activePlan = plans.firstOrNull { plan ->
                plan.monthlyPackage.storeProduct.id in activeIds || plan.yearlyPackage.storeProduct.id in activeIds
            }
            state.copy(selectedPlan = (activePlan ?: plans.firstOrNull())?.plan)
        }
    }

    fun selectPlan(plan: SubscriptionPlan) {
        hasUserSelectedPlan = true
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
