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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

    LaunchedEffect(Unit) { viewModel.refresh() }
    LaunchedEffect(state.error) { state.error?.let { snackbarHostState.showSnackbar(it) } }

    LipaScaffold(title = "Dashboard", snackbarHostState = snackbarHostState) { padding ->
        LipaScreenContainer(padding) {
            Text("Terminal Dashboard", style = MaterialTheme.typography.headlineLarge)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatusCard(
                    modifier = Modifier.weight(1f),
                    title = "Reference",
                    value = state.actorRef ?: "-"
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
            SecondaryActionButton(text = "Reset Settings") {
                scope.launch { onResetConfig() }
            }
        }
    }
}

@Composable
private fun StatusCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    highlightValue: Boolean = false
) {
    LipaCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = value.ifBlank { "No data available" },
                style = MaterialTheme.typography.headlineSmall,
                color = if (highlightValue) SignalGreen else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
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
