package com.kori.terminal.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.kori.terminal.ui.components.LipaScaffold
import com.kori.terminal.ui.components.LipaScreenContainer
import com.kori.terminal.ui.components.PrimaryActionButton
import com.kori.terminal.ui.theme.ApplySystemBars
import com.kori.terminal.ui.theme.BrandBlue

@Composable
fun SetupScreen(
    viewModel: SetupViewModel,
    onContinueToTerminal: () -> Unit
) {
    val state = viewModel.uiState.collectAsState().value
    val snackbarHostState = remember { SnackbarHostState() }
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Color.White,
        unfocusedBorderColor = Color.White.copy(alpha = 0.7f),
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedLabelColor = Color.White,
        unfocusedLabelColor = Color.White.copy(alpha = 0.8f),
        cursorColor = Color.White
    )

    LaunchedEffect(Unit) { viewModel.loadExistingOnce() }
    LaunchedEffect(state.isSaved) { if (state.isSaved) onContinueToTerminal() }
    LaunchedEffect(state.error) { state.error?.let { snackbarHostState.showSnackbar(it) } }

    ApplySystemBars(color = BrandBlue, useDarkIcons = false)

    LipaScaffold(title = "Device Setup", snackbarHostState = snackbarHostState) { padding ->
        LipaScreenContainer(padding) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BrandBlue)
                    .padding(top = 10.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Initial Setup",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.16f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Configure terminal access",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )

                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = state.clientId,
                            onValueChange = viewModel::onClientIdChanged,
                            label = { Text("App ID") },
                            colors = fieldColors,
                            singleLine = true
                        )

                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = state.clientSecret,
                            onValueChange = viewModel::onClientSecretChanged,
                            label = { Text("App Secret") },
                            colors = fieldColors,
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
