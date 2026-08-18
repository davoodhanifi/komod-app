package com.komod.api.presentation.profile

import com.komod.api.domain.model.CurrentSubscription

sealed interface SubscriptionUiState {
    data object Loading : SubscriptionUiState
    data class Success(val subscription: CurrentSubscription) : SubscriptionUiState
    data class Error(val message: String) : SubscriptionUiState
}

data class ProfileUiState(
    val subscriptionState: SubscriptionUiState = SubscriptionUiState.Loading,
)
