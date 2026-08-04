package com.komod.api.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.komod.api.core.error.ErrorContext
import com.komod.api.core.error.ErrorMapper
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
            _uiState.value = LoginUiState(isLoading = true)

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
}
