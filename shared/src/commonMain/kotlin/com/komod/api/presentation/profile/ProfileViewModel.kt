package com.komod.api.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.komod.api.core.error.ErrorContext
import com.komod.api.core.error.ErrorMapper
import com.komod.api.data.repository.SubscriptionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val subscriptionRepository: SubscriptionRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadSubscription()
    }

    // Also used to refresh (see ProfileScreen's refreshKey, mirroring
    // HomeScreen/WardrobeScreen's existing "refresh when returning" pattern).
    fun loadSubscription() {
        viewModelScope.launch {
            // Only blank to the Loading skeleton when there's nothing on screen yet — a
            // refresh triggered by returning to Profile keeps the last known plan/usage
            // visible instead of flashing back to a skeleton every time.
            if (_uiState.value.subscriptionState !is SubscriptionUiState.Success) {
                _uiState.update { it.copy(subscriptionState = SubscriptionUiState.Loading) }
            }
            runCatching { subscriptionRepository.getCurrentSubscription() }
                .onSuccess { subscription ->
                    _uiState.update { it.copy(subscriptionState = SubscriptionUiState.Success(subscription)) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            subscriptionState = SubscriptionUiState.Error(
                                message = ErrorMapper.toUserMessage(error, tag = "ProfileViewModel", context = ErrorContext.Subscription),
                            ),
                        )
                    }
                }
        }
    }
}
