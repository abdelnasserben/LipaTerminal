package com.kori.terminal.ui.terminal

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.kori.terminal.data.nfc.NfcUidReader
import com.kori.terminal.ui.components.InfoRow
import com.kori.terminal.ui.components.LipaCard
import com.kori.terminal.ui.components.LipaScaffold
import com.kori.terminal.ui.components.LipaScreenContainer
import com.kori.terminal.ui.components.PrimaryActionButton
import com.kori.terminal.ui.components.SecondaryActionButton
import kotlinx.coroutines.launch

@Composable
fun TerminalScreen(
    viewModel: TerminalViewModel,
    onBackToDashboard: () -> Unit
) {
    val s = viewModel.uiState.collectAsState().value
    val activity = LocalContext.current as Activity
    val nfcReader = remember { NfcUidReader(activity) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(s.step) {
        if (s.step == PaymentStep.TapCard) {
            nfcReader.enable(
                onUid = { uid -> activity.runOnUiThread { viewModel.onCardUidScanned(uid) } },
                onError = { msg -> activity.runOnUiThread { scope.launch { snackbarHostState.showSnackbar(msg) } } }
            )
        } else {
            nfcReader.disable()
        }
    }

    LaunchedEffect(s.paymentError) { s.paymentError?.let { snackbarHostState.showSnackbar(it) } }

    LipaScaffold(title = "Payment Terminal", snackbarHostState = snackbarHostState) { padding ->
        LipaScreenContainer(padding) {
            SecondaryActionButton(text = "Back to Dashboard", onClick = onBackToDashboard)

            LipaCard {
                Column {
                    Text("Current Session", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(10.dp))
                    InfoRow("Reference", s.actorRef)
                }
            }

            LipaCard {
                when (s.step) {
                    PaymentStep.EnterAmount -> EnterAmountBlock(s.amountText, viewModel::onAmountChanged, viewModel::lockAmountAndStartNfc)
                    PaymentStep.TapCard -> TapCardBlock(s.amountText, nfcReader.isAvailable(), viewModel::simulateCard)
                    PaymentStep.EnterPin -> EnterPinBlock(s, viewModel::onPinChanged, viewModel::pay)
                    PaymentStep.Processing -> ProcessingBlock()
                    PaymentStep.Done -> DoneBlock(s, viewModel::resetPayment)
                }
            }
        }
    }
}

@Composable
private fun EnterAmountBlock(amount: String, onAmountChange: (String) -> Unit, onConfirm: () -> Unit) {
    Column {
        Text("Enter amount", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = amount,
            onValueChange = onAmountChange,
            label = { Text("Amount") },
            singleLine = true
        )
        Spacer(Modifier.height(14.dp))
        PrimaryActionButton(text = "Confirm Amount", onClick = onConfirm)
    }
}

@Composable
private fun TapCardBlock(amount: String, isNfcAvailable: Boolean, onSimulate: () -> Unit) {
    Column {
        Text("Scan Card", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        InfoRow("Amount", amount)
        InfoRow("Reader", if (isNfcAvailable) "Ready" else "Unavailable")
        Spacer(Modifier.height(14.dp))
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.height(14.dp))
        SecondaryActionButton(text = "Simulate Card", onClick = onSimulate)
    }
}

@Composable
private fun EnterPinBlock(state: TerminalUiState, onPinChange: (String) -> Unit, onPay: () -> Unit) {
    Column {
        Text("Card Verification", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        InfoRow("Amount", state.amountText)
        InfoRow("Card", state.cardUid ?: "-")
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = state.pinText,
            onValueChange = onPinChange,
            label = { Text("Security Code (4 digits)") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(Modifier.height(14.dp))
        PrimaryActionButton(text = "Pay", enabled = !state.paymentLoading, onClick = onPay)
    }
}

@Composable
private fun ProcessingBlock() {
    Column {
        Text("Processing Payment", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.secondary)
    }
}

@Composable
private fun DoneBlock(state: TerminalUiState, onReset: () -> Unit) {
    Column {
        Text("Payment Approved", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(10.dp))
        state.paymentResult?.let {
            InfoRow("Transaction", it.transactionId)
            InfoRow("Merchant", it.merchantCode ?: "-")
            InfoRow("Amount", it.amount.toString())
            InfoRow("Fee", it.fee?.toString() ?: "-")
            InfoRow("Total Charged", it.totalDebited?.toString() ?: "-")
        }
        Spacer(Modifier.height(14.dp))
        SecondaryActionButton(text = "New Payment", onClick = onReset)
    }
}
