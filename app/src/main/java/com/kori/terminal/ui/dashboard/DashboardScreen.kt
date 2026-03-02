package com.kori.terminal.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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

    LipaScaffold(title = "Operations dashboard", snackbarHostState = snackbarHostState) { padding ->
        LipaScreenContainer(padding) {
            LipaCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Terminal status", style = MaterialTheme.typography.titleLarge)
                    InfoRow("Actor reference", state.actorRef ?: "-")
                    if (state.loading) {
                        Spacer(Modifier.height(8.dp))
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }

            DashboardBlock("Status", state.status)
            DashboardBlock("Health", state.health)
            DashboardBlock("Configuration", state.config)

            SecondaryActionButton(text = "Refresh", onClick = viewModel::refresh)
            PrimaryActionButton(text = "Open terminal", onClick = onOpenTerminal)
            SecondaryActionButton(text = "Reset configuration") {
                scope.launch { onResetConfig() }
            }
        }
    }
}

@Composable
private fun DashboardBlock(title: String, data: Map<String, String>) {
    LipaCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (data.isEmpty()) {
                Text("No data", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                data.entries.sortedBy { it.key }.forEach { (key, value) ->
                    InfoRow(label = key, value = value)
                }
            }
        }
    }
}
