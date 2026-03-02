package com.kori.terminal.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onOpenTerminal: () -> Unit,
    onResetConfig: suspend () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    LaunchedEffect(state.error) {
        state.error?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Lipa Terminal — Dashboard") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top
        ) {
            if (state.loading) {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
            }

            Text("actorRef: ${state.actorRef ?: "-"}")
            Spacer(Modifier.height(12.dp))

            DashboardBlock("/terminal/me/status", state.status)
            Spacer(Modifier.height(12.dp))
            DashboardBlock("/terminal/me/health", state.health)
            Spacer(Modifier.height(12.dp))
            DashboardBlock("/terminal/me/config", state.config)

            Spacer(Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(modifier = Modifier.weight(1f), onClick = viewModel::refresh) {
                    Text("Rafraîchir")
                }
                Button(modifier = Modifier.weight(1f), onClick = onOpenTerminal) {
                    Text("Ouvrir terminal")
                }
            }

            Spacer(Modifier.height(12.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { scope.launch { onResetConfig() } }
            ) {
                Text("Réinitialiser la configuration")
            }
        }
    }
}

@Composable
private fun DashboardBlock(title: String, data: Map<String, String>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title)
            Spacer(Modifier.height(8.dp))
            if (data.isEmpty()) {
                Text("Aucune donnée")
            } else {
                data.entries.sortedBy { it.key }.forEachIndexed { index, (key, value) ->
                    Text("$key: $value")
                    if (index < data.size - 1) {
                        Spacer(Modifier.height(6.dp))
                        Divider()
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}
