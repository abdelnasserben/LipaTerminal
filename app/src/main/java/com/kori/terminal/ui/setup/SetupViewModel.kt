package com.kori.terminal.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kori.terminal.data.secure.AppConfig
import com.kori.terminal.data.secure.SecureSettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SetupUiState(
    val koriBaseUrl: String = "http://localhost:8081",
    val keycloakTokenUrl: String = "http://localhost:8080/realms/<REALM>/protocol/openid-connect/token",
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
                    koriBaseUrl = existing.koriBaseUrl,
                    keycloakTokenUrl = existing.keycloakTokenUrl,
                    clientId = existing.clientId,
                    clientSecret = existing.clientSecret,
                    error = null,
                    isSaved = false
                )
            }
        }
    }

    fun onKoriBaseUrlChanged(v: String) = _uiState.update { it.copy(koriBaseUrl = v, isSaved = false, error = null) }
    fun onKeycloakTokenUrlChanged(v: String) = _uiState.update { it.copy(keycloakTokenUrl = v, isSaved = false, error = null) }
    fun onClientIdChanged(v: String) = _uiState.update { it.copy(clientId = v, isSaved = false, error = null) }
    fun onClientSecretChanged(v: String) = _uiState.update { it.copy(clientSecret = v, isSaved = false, error = null) }

    fun save() {
        viewModelScope.launch {
            val s = _uiState.value
            val cfg = AppConfig(
                koriBaseUrl = s.koriBaseUrl,
                keycloakTokenUrl = s.keycloakTokenUrl,
                clientId = s.clientId,
                clientSecret = s.clientSecret
            )

            if (!cfg.isValid()) {
                _uiState.update { it.copy(error = "Tous les champs sont obligatoires.", isSaved = false) }
                return@launch
            }

            store.saveConfig(cfg)
            _uiState.update { it.copy(isSaved = true, error = null) }
        }
    }
}