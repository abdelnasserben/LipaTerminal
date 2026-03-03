package com.kori.terminal.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kori.terminal.ui.components.InfoRow
import com.kori.terminal.ui.components.LipaCard
import com.kori.terminal.ui.components.LipaScaffold
import com.kori.terminal.ui.components.LipaScreenContainer
import com.kori.terminal.ui.components.PrimaryActionButton
import com.kori.terminal.ui.components.SecondaryActionButton
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
            LipaCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Terminal Status", style = MaterialTheme.typography.titleLarge)
                    InfoRow("Reference", state.actorRef ?: "-")
                    if (state.loading) {
                        Spacer(Modifier.height(8.dp))
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }

            DashboardBlock("Status", state.status)
            DashboardBlock("Health", state.health)
            DashboardBlock("Mode", state.config)

            SecondaryActionButton(text = "Refresh", onClick = viewModel::refresh)
            PrimaryActionButton(text = "Open Terminal", onClick = onOpenTerminal)
            SecondaryActionButton(text = "Reset Settings") {
                scope.launch { onResetConfig() }
            }
        }
    }
}

@Composable
private fun DashboardBlock(title: String, data: Map<String, String>) {
    val primaryValue = data.entries
        .firstOrNull { !it.key.equals("terminalUid", ignoreCase = true) }
        ?.value
        .orEmpty()

    LipaCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (primaryValue.isBlank()) {
                Text("No data available", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text(
                    text = primaryValue,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}
