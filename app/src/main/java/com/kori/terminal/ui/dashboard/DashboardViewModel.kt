package com.kori.terminal.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kori.terminal.data.auth.KeycloakAuthService
import com.kori.terminal.data.secure.SecureSettingsStore
import com.kori.terminal.data.terminalme.TerminalMeService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val loading: Boolean = false,
    val actorRef: String? = null,
    val status: Map<String, String> = emptyMap(),
    val health: Map<String, String> = emptyMap(),
    val config: Map<String, String> = emptyMap(),
    val error: String? = null
)

class DashboardViewModel(
    private val store: SecureSettingsStore,
    private val authService: KeycloakAuthService = KeycloakAuthService(),
    private val terminalMeService: TerminalMeService = TerminalMeService()
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }

            val config = store.configFlow().filterNotNull().firstOrNull()
            if (config == null) {
                _uiState.update { it.copy(loading = false, error = "Configuration absente") }
                return@launch
            }

            val auth = authService.authenticate(config)
            val authResult = auth.getOrElse { err ->
                _uiState.update { it.copy(loading = false, error = err.message ?: "Erreur authentification") }
                return@launch
            }

            val meResult = terminalMeService.loadAll(config.koriBaseUrl, authResult.accessToken)
            meResult.fold(
                onSuccess = { snapshot ->
                    _uiState.update {
                        it.copy(
                            loading = false,
                            actorRef = authResult.actorRef,
                            status = snapshot.status,
                            health = snapshot.health,
                            config = snapshot.config,
                            error = null
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update { it.copy(loading = false, actorRef = authResult.actorRef, error = err.message) }
                }
            )
        }
    }
}
