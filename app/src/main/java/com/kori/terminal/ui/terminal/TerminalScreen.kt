package com.kori.terminal.ui.terminal

import android.app.Activity
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.kori.terminal.R
import com.kori.terminal.data.nfc.NfcUidReader
import com.kori.terminal.ui.components.LipaScaffold
import com.kori.terminal.ui.components.PrimaryActionButton
import com.kori.terminal.ui.components.SecondaryActionButton
import com.kori.terminal.ui.theme.BrandBlue
import com.kori.terminal.ui.theme.LightBackground
import com.kori.terminal.ui.theme.SignalGreen
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

private val HeaderShape = RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp)
private val AmountFieldShape = RoundedCornerShape(14.dp)
private val KeyShape = RoundedCornerShape(16.dp)

@Composable
fun TerminalScreen(
    viewModel: TerminalViewModel,
    onBackToDashboard: () -> Unit
) {
    val s = viewModel.uiState.collectAsState().value
    val activity = LocalContext.current as Activity
    val view = LocalView.current
    val nfcReader = remember { NfcUidReader(activity) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val darkBackground = s.step == PaymentStep.TapCard
    SideEffect {
        val window = activity.window
        val barColor = if (darkBackground) BrandBlue else LightBackground
        window.statusBarColor = barColor.toArgb()
        window.navigationBarColor = barColor.toArgb()
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !darkBackground
            isAppearanceLightNavigationBars = !darkBackground
        }
    }

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
        when (s.step) {
            PaymentStep.EnterAmount -> EnterAmountBlock(
                amount = s.amountText,
                padding = padding,
                onAmountChange = viewModel::onAmountChanged,
                onConfirm = viewModel::lockAmountAndStartNfc,
                onBackToDashboard = onBackToDashboard
            )

            PaymentStep.TapCard -> TapCardBlock(
                amount = s.amountText,
                padding = padding,
                isNfcAvailable = nfcReader.isAvailable(),
                onSimulate = viewModel::simulateCard,
                onBackToDashboard = onBackToDashboard
            )

            PaymentStep.EnterPin -> EnterPinBlock(
                state = s,
                padding = padding,
                onPinChange = viewModel::onPinChanged,
                onPay = viewModel::pay,
                onBackToDashboard = onBackToDashboard
            )

            PaymentStep.Processing -> ProcessingBlock(padding = padding, onBackToDashboard = onBackToDashboard)
            PaymentStep.Done -> DoneBlock(state = s, padding = padding, onReset = viewModel::resetPayment, onBackToDashboard = onBackToDashboard)
        }
    }
}

@Composable
private fun EnterAmountBlock(
    amount: String,
    padding: PaddingValues,
    onAmountChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onBackToDashboard: () -> Unit
) {
    AmountLikeScreen(
        title = "Enter Amount",
        amount = amount,
        padding = padding,
        actionLabel = "Confirm Amount",
        onAction = onConfirm,
        onBackToDashboard = onBackToDashboard,
        onInput = { key ->
            when (key) {
                "←" -> onAmountChange(amount.dropLast(1))
                "C" -> onAmountChange("")
                else -> onAmountChange((amount + key).take(9))
            }
        }
    )
}

@Composable
private fun EnterPinBlock(
    state: TerminalUiState,
    padding: PaddingValues,
    onPinChange: (String) -> Unit,
    onPay: () -> Unit,
    onBackToDashboard: () -> Unit
) {
    AmountLikeScreen(
        title = "Enter PIN",
        amount = state.amountText,
        padding = padding,
        actionLabel = "Pay ${formatKmf(state.amountText)}",
        onAction = onPay,
        onBackToDashboard = onBackToDashboard,
        subtitle = "Card: ${state.cardUid ?: "-"}",
        valueOverride = if (state.pinText.isBlank()) "----" else "•".repeat(state.pinText.length),
        onInput = { key ->
            when (key) {
                "←" -> onPinChange(state.pinText.dropLast(1))
                "C" -> onPinChange("")
                else -> onPinChange((state.pinText + key).take(4))
            }
        }
    )
}

@Composable
private fun AmountLikeScreen(
    title: String,
    amount: String,
    padding: PaddingValues,
    actionLabel: String,
    onAction: () -> Unit,
    onBackToDashboard: () -> Unit,
    subtitle: String? = null,
    valueOverride: String? = null,
    onInput: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
            .padding(padding)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(HeaderShape)
                .background(BrandBlue)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            IconButton(onClick = onBackToDashboard) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            subtitle?.let {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = it,
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
            Spacer(Modifier.height(14.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF0D2748),
                shape = AmountFieldShape
            ) {
                val display = valueOverride ?: formatKmf(amount)
                Text(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    text = display,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Keypad(onKeyClick = onInput)
        Spacer(Modifier.height(18.dp))
        PrimaryActionButton(
            text = actionLabel,
            modifier = Modifier.padding(horizontal = 20.dp),
            enabled = amount.isNotBlank(),
            onClick = onAction
        )
    }
}

@Composable
private fun Keypad(onKeyClick: (String) -> Unit) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("←", "0", "C")
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { key ->
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(74.dp),
                        shape = KeyShape,
                        color = Color(0xFFF2F4F8),
                        tonalElevation = 1.dp,
                        onClick = { onKeyClick(key) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = key, style = MaterialTheme.typography.headlineSmall, color = Color(0xFF1C2430))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TapCardBlock(
    amount: String,
    padding: PaddingValues,
    isNfcAvailable: Boolean,
    onSimulate: () -> Unit,
    onBackToDashboard: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        Image(
            painter = painterResource(id = R.drawable.scan),
            contentDescription = "Scan background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BrandBlue.copy(alpha = 0.35f))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            IconButton(onClick = onBackToDashboard) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Spacer(Modifier.height(18.dp))
            Text(
                "Tap Card",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 6.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "You pay",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.padding(horizontal = 6.dp)
            )
            Text(
                formatKmf(amount),
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp)
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                ScanIndicator(ready = isNfcAvailable)
            }

            if (!isNfcAvailable) {
                SecondaryActionButton(text = "Simulate Card", onClick = onSimulate)
            }
        }
    }
}

@Composable
private fun ScanIndicator(ready: Boolean) {

    val infiniteTransition = rememberInfiniteTransition(label = "scan")

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Canvas(modifier = Modifier.size(180.dp)) {

        val c = center

        if (ready) {
            // Pulsing outer ring
            drawCircle(
                color = SignalGreen.copy(alpha = alpha),
                radius = 70f * pulse,
                center = c,
                style = Stroke(width = 8f)
            )

            // Solid center dot
            drawCircle(
                color = SignalGreen,
                radius = 14f,
                center = c
            )
        } else {
            drawCircle(
                color = Color.White.copy(alpha = 0.5f),
                radius = 14f,
                center = c
            )
        }
    }
}

@Composable
private fun ProcessingBlock(padding: PaddingValues, onBackToDashboard: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
            .padding(padding)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Processing Payment", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.height(16.dp))
        SecondaryActionButton(text = "Back to Dashboard", onClick = onBackToDashboard)
    }
}

@Composable
private fun DoneBlock(state: TerminalUiState, padding: PaddingValues, onReset: () -> Unit, onBackToDashboard: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
            .padding(padding)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Payment Approved", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text("Amount paid: ${formatKmf(state.amountText)}")
        state.paymentResult?.let {
            Spacer(Modifier.height(6.dp))
            Text("Transaction: ${it.transactionId}")
            Text("Total charged: ${it.totalDebited ?: "-"}")
        }
        Spacer(Modifier.height(16.dp))
        PrimaryActionButton(text = "New Payment", onClick = onReset)
        Spacer(Modifier.height(10.dp))
        SecondaryActionButton(text = "Back to Dashboard", onClick = onBackToDashboard)
    }
}

private fun formatKmf(rawAmount: String): String {
    val digits = rawAmount.filter { it.isDigit() }
    if (digits.isBlank()) return "0 KMF"
    val value = digits.toLongOrNull() ?: return "$digits KMF"
    val formatter = DecimalFormat("#,###", DecimalFormatSymbols(Locale.US))
    return "${formatter.format(value)} KMF"
}
