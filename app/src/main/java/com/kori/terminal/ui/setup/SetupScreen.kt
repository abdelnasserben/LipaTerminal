package com.kori.terminal.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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

    LipaScaffold(title = "Device Setup", snackbarHostState = snackbarHostState) { padding ->
        LipaScreenContainer(padding) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 10.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Initial Setup",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold
                )

                LipaCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Configure terminal access",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = state.koriBaseUrl,
                            onValueChange = viewModel::onServerBaseUrlChanged,
                            label = { Text("Server Base URL") },
                            singleLine = true
                        )

                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = state.keycloakTokenUrl,
                            onValueChange = viewModel::onServerTokenUrlChanged,
                            label = { Text("Server Token URL") },
                            singleLine = true
                        )

                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = state.clientId,
                            onValueChange = viewModel::onClientIdChanged,
                            label = { Text("App ID") },
                            singleLine = true
                        )

                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = state.clientSecret,
                            onValueChange = viewModel::onClientSecretChanged,
                            label = { Text("App Secret") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true
                        )

                        Spacer(Modifier.height(4.dp))
                        PrimaryActionButton(text = "Save and Continue") { viewModel.save() }
                    }
                }
            }
        }
    }
}
