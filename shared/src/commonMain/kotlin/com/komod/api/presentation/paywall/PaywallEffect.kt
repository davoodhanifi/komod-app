package com.komod.api.presentation.paywall

sealed interface PaywallEffect {
    data class ShowSnackbar(val message: String) : PaywallEffect
    data object PurchaseCompleted : PaywallEffect
}
