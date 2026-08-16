package com.komod.api.presentation.auth

enum class AuthProvider {
    Google,
    Apple,
}

data class LoginUiState(
    val loadingProvider: AuthProvider? = null,
    val error: String? = null,
) {
    val isLoading: Boolean get() = loadingProvider != null
}
