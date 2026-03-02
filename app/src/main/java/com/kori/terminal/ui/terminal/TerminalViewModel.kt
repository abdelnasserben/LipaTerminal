package com.kori.terminal.ui.terminal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kori.terminal.data.auth.KeycloakAuthService
import com.kori.terminal.data.payments.PayByCardResponse
import com.kori.terminal.data.payments.PaymentService
import com.kori.terminal.data.secure.SecureSettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID

enum class PaymentStep {
    EnterAmount,
    TapCard,
    EnterPin,
    Processing,
    Done
}

data class TerminalUiState(
    // Auth
    val authLoading: Boolean = false,
    val actorRef: String? = null,
    val token: String? = null,
    val authError: String? = null,

    // Payment
    val step: PaymentStep = PaymentStep.EnterAmount,
    val amountText: String = "",
    val amountLocked: Boolean = false,
    val cardUid: String? = null,
    val pinText: String = "",
    val paymentLoading: Boolean = false,
    val paymentResult: PayByCardResponse? = null,
    val paymentError: String? = null
)

class TerminalViewModel(
    private val store: SecureSettingsStore
) : ViewModel() {

    private val authService = KeycloakAuthService()
    private val paymentService = PaymentService()

    private val _uiState = MutableStateFlow(TerminalUiState())
    val uiState: StateFlow<TerminalUiState> = _uiState

    fun authenticate() {
        viewModelScope.launch {
            _uiState.update { it.copy(authLoading = true, authError = null) }

            val config = withTimeoutOrNull(1500) {
                store.configFlow().filterNotNull().firstOrNull()
            }

            if (config == null) {
                _uiState.update {
                    it.copy(authLoading = false, authError = "Configuration absente. Enregistre d'abord la configuration.")
                }
                return@launch
            }

            val result = authService.authenticate(config)

            _uiState.update { s ->
                result.fold(
                    onSuccess = { ok ->
                        s.copy(
                            authLoading = false,
                            token = ok.accessToken,
                            actorRef = ok.actorRef,
                            authError = null
                        )
                    },
                    onFailure = { err ->
                        s.copy(
                            authLoading = false,
                            authError = err.message ?: err.toString()
                        )
                    }
                )
            }
        }
    }

    // ----- Payment flow -----

    fun onAmountChanged(v: String) {
        if (_uiState.value.amountLocked) return
        val filtered = v.filter { it.isDigit() || it == '.' || it == ',' }
        _uiState.update { it.copy(amountText = filtered, paymentError = null) }
    }

    fun lockAmountAndStartNfc() {
        val amt = _uiState.value.amountText.trim()
        if (amt.isBlank()) {
            _uiState.update { it.copy(paymentError = "Saisis un montant") }
            return
        }
        _uiState.update {
            it.copy(
                amountLocked = true,
                step = PaymentStep.TapCard,
                paymentError = null
            )
        }
    }

    fun onCardUidScanned(uid: String) {
        if (_uiState.value.step != PaymentStep.TapCard) return
        _uiState.update { it.copy(cardUid = uid, step = PaymentStep.EnterPin, paymentError = null) }
    }

    fun simulateCard() {
        if (_uiState.value.step != PaymentStep.TapCard) return
        onCardUidScanned("045B913E7A2C19")
    }

    fun onPinChanged(v: String) {
        val filtered = v.filter { it.isDigit() }
        _uiState.update { it.copy(pinText = filtered.take(4), paymentError = null) }
    }

    fun pay() {
        viewModelScope.launch {
            val token = _uiState.value.token
            if (token.isNullOrBlank()) {
                _uiState.update { it.copy(paymentError = "Non authentifié. Clique sur “S'authentifier”.") }
                return@launch
            }

            val config = withTimeoutOrNull(1500) {
                store.configFlow().filterNotNull().firstOrNull()
            } ?: run {
                _uiState.update { it.copy(paymentError = "Configuration absente.") }
                return@launch
            }

            val amount = parseAmount(_uiState.value.amountText)
            if (amount == null || amount <= 0.0) {
                _uiState.update { it.copy(paymentError = "Montant invalide") }
                return@launch
            }

            val uid = _uiState.value.cardUid
            if (uid.isNullOrBlank()) {
                _uiState.update { it.copy(paymentError = "Carte absente (UID)") }
                return@launch
            }

            val pin = _uiState.value.pinText
            if (!Regex("^\\d{4}$").matches(pin)) {
                _uiState.update { it.copy(paymentError = "PIN invalide (4 chiffres)") }
                return@launch
            }

            val idempotencyKey = UUID.randomUUID().toString()

            _uiState.update {
                it.copy(
                    step = PaymentStep.Processing,
                    paymentLoading = true,
                    paymentError = null,
                    paymentResult = null
                )
            }

            val result = paymentService.payByCard(
                baseUrl = config.koriBaseUrl,
                bearerToken = token,
                idempotencyKey = idempotencyKey,
                amount = amount,
                cardUid = uid,
                pin = pin
            )

            _uiState.update { s ->
                result.fold(
                    onSuccess = { resp ->
                        s.copy(
                            step = PaymentStep.Done,
                            paymentLoading = false,
                            paymentResult = resp,
                            paymentError = null,
                            pinText = "" // sécurité: purge PIN après usage
                        )
                    },
                    onFailure = { err ->
                        s.copy(
                            step = PaymentStep.EnterPin, // retour PIN pour corriger / retry
                            paymentLoading = false,
                            paymentError = err.message ?: err.toString(),
                            pinText = "" // purge PIN même en erreur
                        )
                    }
                )
            }
        }
    }

    fun resetPayment() {
        _uiState.update {
            it.copy(
                step = PaymentStep.EnterAmount,
                amountText = "",
                amountLocked = false,
                cardUid = null,
                pinText = "",
                paymentLoading = false,
                paymentResult = null,
                paymentError = null
            )
        }
    }

    private fun parseAmount(text: String): Double? {
        val normalized = text.trim().replace(',', '.')
        return normalized.toDoubleOrNull()
    }
}