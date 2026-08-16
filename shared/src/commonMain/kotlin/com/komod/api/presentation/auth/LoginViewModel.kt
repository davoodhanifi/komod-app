package com.komod.api.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.komod.api.core.error.ErrorContext
import com.komod.api.core.error.ErrorMapper
import com.komod.api.data.auth.AppleSignInCancelledException
import com.komod.api.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.callbackError.collect { error ->
                _uiState.value = LoginUiState(
                    error = ErrorMapper.toUserMessage(error, tag = "LoginViewModel", context = ErrorContext.Auth),
                )
            }
        }
    }

    fun signInWithGoogle() {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.value = LoginUiState(loadingProvider = AuthProvider.Google)

            runCatching {
                authRepository.signInWithGoogle()
            }.onSuccess {
                _uiState.value = LoginUiState()
            }.onFailure { throwable ->
                _uiState.value = LoginUiState(
                    error = ErrorMapper.toUserMessage(throwable, tag = "LoginViewModel", context = ErrorContext.Auth),
                )
            }
        }
    }

    fun signInWithApple() {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.value = LoginUiState(loadingProvider = AuthProvider.Apple)

            runCatching {
                authRepository.signInWithApple()
            }.onSuccess {
                _uiState.value = LoginUiState()
            }.onFailure { throwable ->
                // A user backing out of the native Apple sheet is not a failure worth
                // surfacing — just return to an idle, retryable state.
                _uiState.value = if (throwable is AppleSignInCancelledException) {
                    LoginUiState()
                } else {
                    LoginUiState(
                        error = ErrorMapper.toUserMessage(throwable, tag = "LoginViewModel", context = ErrorContext.Auth),
                    )
                }
            }
        }
    }

    // Called when the login screen resumes (e.g. the user switched back to the app after
    // backing out of the Android OAuth browser tab without completing it). Supabase's
    // signInWith(OAuthProvider) only launches the browser and returns — it has no way to
    // report "the user closed the tab" — so this is the only signal available that a
    // still-loading screen was abandoned rather than actually cancelled or failed.
    fun onScreenResumed() {
        if (_uiState.value.isLoading) {
            _uiState.value = LoginUiState()
        }
    }
}
