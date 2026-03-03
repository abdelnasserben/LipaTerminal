package com.kori.terminal.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kori.terminal.ui.components.LipaScaffold
import com.kori.terminal.ui.components.LipaScreenContainer
import com.kori.terminal.ui.components.PrimaryActionButton
import com.kori.terminal.ui.theme.BrandBlue

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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BrandBlue)
                    .padding(horizontal = 28.dp, vertical = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "LIPA",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )

                Spacer(Modifier.height(20.dp))
                Text(
                    text = "Secure terminal access",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )

                Spacer(Modifier.height(44.dp))
                if (state.loading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                } else {
                    PrimaryActionButton(
                        text = "Authenticate",
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        viewModel.authenticate(onAuthenticated)
                    }
                }
            }
        }
    }
}
