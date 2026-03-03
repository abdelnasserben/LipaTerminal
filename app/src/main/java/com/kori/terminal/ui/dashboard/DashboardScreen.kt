package com.kori.terminal.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kori.terminal.ui.components.LipaScaffold
import com.kori.terminal.ui.components.LipaScreenContainer
import com.kori.terminal.ui.components.PrimaryActionButton
import com.kori.terminal.ui.theme.BrandBlue
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
    val view = LocalView.current

    LaunchedEffect(Unit) { viewModel.refresh() }
    LaunchedEffect(state.error) { state.error?.let { snackbarHostState.showSnackbar(it) } }

    LipaScaffold(
        title = "Dashboard",
        snackbarHostState = snackbarHostState,
        containerColor = BrandBlue
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BrandBlue)
        ) {
            LipaScreenContainer(padding) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Dashboard", style = MaterialTheme.typography.headlineLarge, color = Color.White)

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
                        DashboardCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(color = Color.White)
                            }
                        }
                    }

                    PrimaryActionButton(text = "Make Payment", onClick = onOpenTerminal)
                }
            }
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
    DashboardCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White)
            Text(
                text = value.ifBlank { "No data available" },
                style = MaterialTheme.typography.bodyLarge,
                color = if (highlightValue) SignalGreen else Color.White,
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
private fun DashboardCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.16f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.padding(16.dp)) { content() }
    }
}

private fun primaryValue(data: Map<String, String>): String {
    return data.entries
        .firstOrNull { !it.key.equals("terminalUid", ignoreCase = true) }
        ?.value
        .orEmpty()
}
