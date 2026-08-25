package com.komod.api.presentation.paywall

sealed interface PaywallEffect {
    data class ShowSnackbar(val message: String) : PaywallEffect

    // Purchase succeeded AND the backend sync confirmed it — safe to dismiss the paywall.
    // A purchase that succeeds but fails to sync does NOT emit this (see
    // PaywallViewModel.syncAfterPurchase); the screen stays open showing the retry banner.
    data object PurchaseCompleted : PaywallEffect
}
