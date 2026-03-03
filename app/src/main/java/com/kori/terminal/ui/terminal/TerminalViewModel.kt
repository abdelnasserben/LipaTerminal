package com.kori.terminal.ui.terminal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kori.terminal.data.payments.PayByCardResponse
import com.kori.terminal.data.payments.PaymentService
import com.kori.terminal.data.secure.SecureSettingsStore
import com.kori.terminal.ui.auth.Session
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
    val actorRef: String,
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
    private val store: SecureSettingsStore,
    private val session: Session
) : ViewModel() {

    private val paymentService = PaymentService()

    private val _uiState = MutableStateFlow(TerminalUiState(actorRef = session.actorRef))
    val uiState: StateFlow<TerminalUiState> = _uiState

    fun onAmountChanged(v: String) {
        if (_uiState.value.amountLocked) return
        val filtered = v.filter { it.isDigit() || it == '.' || it == ',' }
        _uiState.update { it.copy(amountText = filtered, paymentError = null) }
    }

    fun lockAmountAndStartNfc() {
        val amt = _uiState.value.amountText.trim()
        if (amt.isBlank()) {
            _uiState.update { it.copy(paymentError = "Enter an amount.") }
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
            val config = withTimeoutOrNull(1500) {
                store.configFlow().filterNotNull().firstOrNull()
            } ?: run {
                _uiState.update { it.copy(paymentError = "Missing settings.") }
                return@launch
            }

            val amount = parseAmount(_uiState.value.amountText)
            if (amount == null || amount <= 0.0) {
                _uiState.update { it.copy(paymentError = "Invalid amount.") }
                return@launch
            }

            val uid = _uiState.value.cardUid
            if (uid.isNullOrBlank()) {
                _uiState.update { it.copy(paymentError = "Card not detected.") }
                return@launch
            }

            val pin = _uiState.value.pinText
            if (!Regex("^\\d{4}$").matches(pin)) {
                _uiState.update { it.copy(paymentError = "Invalid security code (4 digits).") }
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
                bearerToken = session.token,
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
                            pinText = ""
                        )
                    },
                    onFailure = { err ->
                        s.copy(
                            step = PaymentStep.EnterPin,
                            paymentLoading = false,
                            paymentError = err.message ?: err.toString(),
                            pinText = ""
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
