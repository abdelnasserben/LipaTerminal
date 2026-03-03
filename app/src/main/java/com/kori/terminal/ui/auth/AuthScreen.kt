package com.kori.terminal.ui.auth

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.kori.terminal.R
import com.kori.terminal.ui.components.LipaScaffold
import com.kori.terminal.ui.components.PrimaryActionButton
import com.kori.terminal.ui.theme.BrandBlue

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onAuthenticated: (Session) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val view = LocalView.current

    LaunchedEffect(state.error) { state.error?.let { snackbarHostState.showSnackbar(it) } }

    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        window.statusBarColor = BrandBlue.toArgb()
        window.navigationBarColor = BrandBlue.toArgb()
        val insetsController = WindowCompat.getInsetsController(window, view)
        insetsController.isAppearanceLightStatusBars = false
        insetsController.isAppearanceLightNavigationBars = false
    }

    LipaScaffold(title = "Device Access", snackbarHostState = snackbarHostState) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BrandBlue)
                .padding(padding)
                .padding(horizontal = 28.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Lipa logo",
                modifier = Modifier.size(172.dp)
            )

            Spacer(Modifier.height(20.dp))

            Text(
                text = "Secure terminal access",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )

            Spacer(Modifier.height(12.dp))

            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Secure lock",
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(28.dp)
            )

            Spacer(Modifier.height(44.dp))

            if (state.loading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.secondary
                )
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
