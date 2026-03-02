package com.kori.terminal.ui.terminal

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.kori.terminal.data.nfc.NfcUidReader
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    viewModel: TerminalViewModel,
    onBackToDashboard: () -> Unit
) {
    val s = viewModel.uiState.collectAsState().value
    val context = LocalContext.current
    val activity = context as Activity

    val nfcReader = remember { NfcUidReader(activity) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(s.step) {
        if (s.step == PaymentStep.TapCard) {
            nfcReader.enable(
                onUid = { uid ->
                    activity.runOnUiThread { viewModel.onCardUidScanned(uid) }
                },
                onError = { msg ->
                    activity.runOnUiThread { scope.launch { snackbarHostState.showSnackbar(msg) } }
                }
            )
        } else {
            nfcReader.disable()
        }
    }

    LaunchedEffect(s.authError) {
        val err = s.authError ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(err)
    }
    LaunchedEffect(s.paymentError) {
        val err = s.paymentError ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(err)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Lipa Terminal") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Button(modifier = Modifier.fillMaxWidth(), onClick = onBackToDashboard) {
                Text("Retour dashboard")
            }

            Spacer(Modifier.height(24.dp))

            Text("Auth terminal")
            Spacer(Modifier.height(8.dp))

            when {
                s.authLoading -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                s.token != null -> {
                    Text("Authentifié ✅")
                    Text("actorRef: ${s.actorRef}")
                }

                else -> {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { viewModel.authenticate() }
                    ) { Text("S'authentifier (client_credentials)") }
                }
            }

            Spacer(Modifier.height(24.dp))
            Divider()
            Spacer(Modifier.height(24.dp))

            Text("Paiement")
            Spacer(Modifier.height(12.dp))

            when (s.step) {
                PaymentStep.EnterAmount -> {
                    Text("Marchand : saisir le montant")
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = s.amountText,
                        onValueChange = viewModel::onAmountChanged,
                        label = { Text("Montant") },
                        singleLine = true
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { viewModel.lockAmountAndStartNfc() }
                    ) { Text("Valider le montant") }
                }

                PaymentStep.TapCard -> {
                    Text("Montant verrouillé : ${s.amountText}")
                    Spacer(Modifier.height(12.dp))
                    Text("Client : approcher la carte NFC (ou simuler)")
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                    Text(if (nfcReader.isAvailable()) "NFC prêt ✅" else "NFC indisponible ❌")

                    Spacer(Modifier.height(16.dp))
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { viewModel.simulateCard() }
                    ) { Text("Simuler carte (debug)") }
                }

                PaymentStep.EnterPin -> {
                    Text("Montant verrouillé : ${s.amountText}")
                    Spacer(Modifier.height(8.dp))
                    Text("UID: ${s.cardUid}")

                    Spacer(Modifier.height(16.dp))
                    Text("Client : saisir le PIN (masqué)")
                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = s.pinText,
                        onValueChange = viewModel::onPinChanged,
                        label = { Text("PIN (4 chiffres)") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )

                    Spacer(Modifier.height(12.dp))
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !s.paymentLoading,
                        onClick = { viewModel.pay() }
                    ) { Text("Payer") }
                }

                PaymentStep.Processing -> {
                    Text("Paiement en cours…")
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                PaymentStep.Done -> {
                    val r = s.paymentResult
                    Text("Paiement OK ✅")
                    Spacer(Modifier.height(8.dp))
                    if (r != null) {
                        Text("transactionId: ${r.transactionId}")
                        Text("merchantCode: ${r.merchantCode ?: "-"}")
                        Text("amount: ${r.amount}")
                        Text("fee: ${r.fee ?: "-"}")
                        Text("totalDebited: ${r.totalDebited ?: "-"}")
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { viewModel.resetPayment() }
                    ) { Text("Nouveau paiement") }
                }
            }
        }
    }
}
