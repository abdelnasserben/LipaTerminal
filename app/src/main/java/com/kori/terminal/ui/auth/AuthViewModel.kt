package com.kori.terminal.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kori.terminal.data.auth.KeycloakAuthService
import com.kori.terminal.data.secure.SecureSettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

data class Session(
    val token: String,
    val actorRef: String
)

data class AuthUiState(
    val loading: Boolean = false,
    val error: String? = null
)

class AuthViewModel(
    private val store: SecureSettingsStore,
    private val authService: KeycloakAuthService = KeycloakAuthService()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState

    fun authenticate(onSuccess: (Session) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }

            val config = withTimeoutOrNull(1500) {
                store.configFlow().filterNotNull().firstOrNull()
            }

            if (config == null) {
                _uiState.update {
                    it.copy(loading = false, error = "Configuration absente. Enregistre d'abord la configuration.")
                }
                return@launch
            }

            val result = authService.authenticate(config)

            result.fold(
                onSuccess = { auth ->
                    _uiState.update { it.copy(loading = false, error = null) }
                    onSuccess(Session(token = auth.accessToken, actorRef = auth.actorRef))
                },
                onFailure = { err ->
                    _uiState.update { it.copy(loading = false, error = err.message ?: err.toString()) }
                }
            )
        }
    }
}

