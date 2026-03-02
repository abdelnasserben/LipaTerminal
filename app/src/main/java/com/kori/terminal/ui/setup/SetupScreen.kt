package com.kori.terminal.ui.setup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.kori.terminal.ui.components.LipaCard
import com.kori.terminal.ui.components.LipaScaffold
import com.kori.terminal.ui.components.LipaScreenContainer
import com.kori.terminal.ui.components.PrimaryActionButton

@Composable
fun SetupScreen(
    viewModel: SetupViewModel,
    onContinueToTerminal: () -> Unit
) {
    val state = viewModel.uiState.collectAsState().value
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.loadExistingOnce() }
    LaunchedEffect(state.isSaved) { if (state.isSaved) onContinueToTerminal() }
    LaunchedEffect(state.error) { state.error?.let { snackbarHostState.showSnackbar(it) } }

    LipaScaffold(title = "Device setup", snackbarHostState = snackbarHostState) { padding ->
        LipaScreenContainer(padding) {
            LipaCard {
                Column {
                    Text("Backend Configuration", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Credentials and endpoints are encrypted on-device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = state.koriBaseUrl,
                        onValueChange = viewModel::onKoriBaseUrlChanged,
                        label = { Text("Kori Base URL") },
                        singleLine = true
                    )

                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = state.keycloakTokenUrl,
                        onValueChange = viewModel::onKeycloakTokenUrlChanged,
                        label = { Text("Keycloak Token URL") },
                        singleLine = true
                    )

                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = state.clientId,
                        onValueChange = viewModel::onClientIdChanged,
                        label = { Text("Client ID") },
                        singleLine = true
                    )

                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = state.clientSecret,
                        onValueChange = viewModel::onClientSecretChanged,
                        label = { Text("Client Secret") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )

                    Spacer(Modifier.height(20.dp))
                    PrimaryActionButton(text = "Save and continue") { viewModel.save() }
                }
            }
        }
    }
}
