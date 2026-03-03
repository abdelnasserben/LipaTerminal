package com.kori.terminal.ui.auth

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kori.terminal.ui.components.LipaCard
import com.kori.terminal.ui.components.LipaScaffold
import com.kori.terminal.ui.components.LipaScreenContainer
import com.kori.terminal.ui.components.PrimaryActionButton

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onAuthenticated: (Session) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.authenticate(onAuthenticated) }
    LaunchedEffect(state.error) { state.error?.let { snackbarHostState.showSnackbar(it) } }

    LipaScaffold(title = "Device Access", snackbarHostState = snackbarHostState) { padding ->
        LipaScreenContainer(padding) {
            Spacer(Modifier.height(16.dp))
            LipaCard {
                Column {
                    Text("Secure Access", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "The app verifies this device before allowing any action.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(20.dp))
                    if (state.loading) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                    } else {
                        PrimaryActionButton(text = "Retry Access") {
                            viewModel.authenticate(onAuthenticated)
                        }
                    }
                }
            }
        }
    }
}
