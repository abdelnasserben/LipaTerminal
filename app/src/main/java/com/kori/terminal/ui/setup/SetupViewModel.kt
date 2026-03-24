package com.kori.terminal.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kori.terminal.data.secure.AppConfig
import com.kori.terminal.data.secure.SecureSettingsStore
import com.kori.terminal.ui.setup.SetupViewModel.Companion.PROD_API_BASE_URL
import com.kori.terminal.ui.setup.SetupViewModel.Companion.PROD_KEYCLOAK_TOKEN_URL
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SetupUiState(
    val koriBaseUrl: String = PROD_API_BASE_URL,
    val keycloakTokenUrl: String = PROD_KEYCLOAK_TOKEN_URL,
    val clientId: String = "",
    val clientSecret: String = "",
    val isSaved: Boolean = false,
    val error: String? = null
)

class SetupViewModel(
    private val store: SecureSettingsStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState

    fun loadExistingOnce() {
        viewModelScope.launch {
            val existing = store.configFlow().first() ?: return@launch
            _uiState.update {
                it.copy(
                    koriBaseUrl = PROD_API_BASE_URL,
                    keycloakTokenUrl = PROD_KEYCLOAK_TOKEN_URL,
                    clientId = existing.clientId,
                    clientSecret = existing.clientSecret,
                    error = null,
                    isSaved = false
                )
            }
        }
    }

    fun onClientIdChanged(v: String) = _uiState.update { it.copy(clientId = v, isSaved = false, error = null) }
    fun onClientSecretChanged(v: String) = _uiState.update { it.copy(clientSecret = v, isSaved = false, error = null) }

    fun save() {
        viewModelScope.launch {
            val s = _uiState.value
            val cfg = AppConfig(
                koriBaseUrl = PROD_API_BASE_URL,
                keycloakTokenUrl = PROD_KEYCLOAK_TOKEN_URL,
                clientId = s.clientId,
                clientSecret = s.clientSecret
            )

            if (!cfg.isValid()) {
                _uiState.update { it.copy(error = "All fields are required.", isSaved = false) }
                return@launch
            }

            store.saveConfig(cfg)
            _uiState.update {
                it.copy(
                    koriBaseUrl = PROD_API_BASE_URL,
                    keycloakTokenUrl = PROD_KEYCLOAK_TOKEN_URL,
                    isSaved = true,
                    error = null
                )
            }
        }
    }

    companion object {
        const val PROD_API_BASE_URL = "https://api.dabel.fr"
        const val PROD_KEYCLOAK_TOKEN_URL = "https://auth.dabel.fr"
    }
}
