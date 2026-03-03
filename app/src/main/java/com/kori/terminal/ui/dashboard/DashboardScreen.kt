package com.kori.terminal.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.detectTapGestures
import com.kori.terminal.ui.components.LipaCard
import com.kori.terminal.ui.components.LipaScaffold
import com.kori.terminal.ui.components.LipaScreenContainer
import com.kori.terminal.ui.components.SecondaryActionButton
import com.kori.terminal.ui.theme.SignalGreen
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onOpenTerminal: () -> Unit,
    onResetConfig: suspend () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var resetTapCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) { viewModel.refresh() }
    LaunchedEffect(state.error) { state.error?.let { snackbarHostState.showSnackbar(it) } }

    LipaScaffold(title = "Dashboard", snackbarHostState = snackbarHostState) { padding ->
        LipaScreenContainer(padding) {
            Text("Dashboard", style = MaterialTheme.typography.headlineLarge)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatusCard(
                    modifier = Modifier.weight(1f),
                    title = "Reference",
                    value = state.actorRef ?: "-",
                    onValueClick = {
                        resetTapCount += 1
                        if (resetTapCount >= 11) {
                            resetTapCount = 0
                            scope.launch {
                                onResetConfig()
                                snackbarHostState.showSnackbar("Resetting settings...")
                            }
                        }
                    }
                )
                StatusCard(
                    modifier = Modifier.weight(1f),
                    title = "Status",
                    value = primaryValue(state.status),
                    highlightValue = true
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatusCard(
                    modifier = Modifier.weight(1f),
                    title = "Health",
                    value = primaryValue(state.health),
                    highlightValue = true
                )
                StatusCard(
                    modifier = Modifier.weight(1f),
                    title = "Mode",
                    value = primaryValue(state.config)
                )
            }

            if (state.loading) {
                LipaCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }

            PaymentActionCard(onClick = onOpenTerminal)
            SecondaryActionButton(text = "Refresh", onClick = viewModel::refresh)
        }
    }
}

@Composable
private fun StatusCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    highlightValue: Boolean = false,
    onValueClick: (() -> Unit)? = null
) {
    LipaCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = value.ifBlank { "No data available" },
                style = MaterialTheme.typography.bodyLarge,
                color = if (highlightValue) SignalGreen else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                modifier = if (onValueClick != null) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(onTap = { onValueClick() })
                    }
                } else {
                    Modifier
                }
            )
        }
    }
}

@Composable
private fun PaymentActionCard(onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = SignalGreen, shape = shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 28.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF0A3E45)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Open Terminal",
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFF0A3E45)
            )
        }
    }
}

private fun primaryValue(data: Map<String, String>): String {
    return data.entries
        .firstOrNull { !it.key.equals("terminalUid", ignoreCase = true) }
        ?.value
        .orEmpty()
}
