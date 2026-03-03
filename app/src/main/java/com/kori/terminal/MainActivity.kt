package com.kori.terminal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kori.terminal.data.secure.SecureSettingsStore
import com.kori.terminal.data.secure.StoredSession
import com.kori.terminal.data.auth.JwtTokenState
import com.kori.terminal.ui.auth.AuthScreen
import com.kori.terminal.ui.auth.AuthViewModel
import com.kori.terminal.ui.auth.Session
import com.kori.terminal.ui.boot.BootScreen
import com.kori.terminal.ui.dashboard.DashboardScreen
import com.kori.terminal.ui.dashboard.DashboardViewModel
import com.kori.terminal.ui.setup.SetupScreen
import com.kori.terminal.ui.setup.SetupViewModel
import com.kori.terminal.ui.terminal.TerminalScreen
import com.kori.terminal.ui.terminal.TerminalViewModel
import com.kori.terminal.ui.theme.KoriTerminalTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private object Routes {
    const val Boot = "boot"
    const val Setup = "setup"
    const val Auth = "auth"
    const val Dashboard = "dashboard"
    const val Terminal = "terminal"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            KoriTerminalTheme {
                BackHandler(enabled = true) {}

                val navController = rememberNavController()
                val store = remember { SecureSettingsStore(applicationContext) }
                var session: Session? by remember { mutableStateOf(null) }

                LaunchedEffect(session?.token) {
                    val activeSession = session ?: return@LaunchedEffect
                    val expirationEpochSeconds = JwtTokenState.expirationEpochSeconds(activeSession.token)

                    if (expirationEpochSeconds == null) {
                        session = null
                        store.clearSession()
                        return@LaunchedEffect
                    }

                    val nowEpochSeconds = System.currentTimeMillis() / 1000
                    val remainingSeconds = expirationEpochSeconds - nowEpochSeconds

                    if (remainingSeconds <= 0) {
                        session = null
                        store.clearSession()
                        return@LaunchedEffect
                    }

                    delay(remainingSeconds * 1000)

                    if (session?.token == activeSession.token) {
                        session = null
                        store.clearSession()
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = Routes.Boot
                ) {
                    composable(Routes.Boot) {
                        LaunchedEffect(Unit) {
                            val cfg = store.configFlow().first()
                            val persistedSession = store.sessionFlow().first()

                            val dest = when {
                                cfg == null -> Routes.Setup
                                persistedSession != null && JwtTokenState.isNonExpired(persistedSession.token) -> {
                                    session = Session(
                                        token = persistedSession.token,
                                        actorRef = persistedSession.actorRef
                                    )
                                    Routes.Dashboard
                                }
                                persistedSession != null -> {
                                    store.clearSession()
                                    Routes.Auth
                                }
                                else -> Routes.Auth
                            }

                            navController.navigate(dest) {
                                popUpTo(Routes.Boot) { inclusive = true }
                            }
                        }

                        BootScreen()
                    }

                    composable(Routes.Setup) {
                        val setupVm: SetupViewModel = viewModel(
                            factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                                @Suppress("UNCHECKED_CAST")
                                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                    return SetupViewModel(store) as T
                                }
                            }
                        )

                        SetupScreen(
                            viewModel = setupVm,
                            onContinueToTerminal = {
                                navController.navigate(Routes.Auth) {
                                    popUpTo(Routes.Setup) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(Routes.Auth) {
                        val authVm: AuthViewModel = viewModel(
                            factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                                @Suppress("UNCHECKED_CAST")
                                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                    return AuthViewModel(store) as T
                                }
                            }
                        )

                        AuthScreen(
                            viewModel = authVm,
                            onAuthenticated = { authenticatedSession ->
                                session = authenticatedSession
                                setupVmSafeSaveSession(
                                    store = store,
                                    session = authenticatedSession
                                )
                                navController.navigate(Routes.Dashboard) {
                                    popUpTo(Routes.Auth) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(Routes.Dashboard) {
                        val activeSession = session
                        if (activeSession == null) {
                            LaunchedEffect(Unit) {
                                navController.navigate(Routes.Auth) {
                                    popUpTo(Routes.Dashboard) { inclusive = true }
                                }
                            }
                            BootScreen()
                        } else {
                            val dashboardVm: DashboardViewModel = viewModel(
                                key = "dashboard-${activeSession.actorRef}",
                                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                                    @Suppress("UNCHECKED_CAST")
                                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                        return DashboardViewModel(store, activeSession) as T
                                    }
                                }
                            )

                            DashboardScreen(
                                viewModel = dashboardVm,
                                onOpenTerminal = {
                                    navController.navigate(Routes.Terminal)
                                },
                                onResetConfig = {
                                    session = null
                                    store.clear()
                                    navController.navigate(Routes.Setup) {
                                        popUpTo(Routes.Dashboard) { inclusive = true }
                                    }
                                }
                            )
                        }
                    }

                    composable(Routes.Terminal) {
                        val activeSession = session
                        if (activeSession == null) {
                            LaunchedEffect(Unit) {
                                navController.navigate(Routes.Auth) {
                                    popUpTo(Routes.Terminal) { inclusive = true }
                                }
                            }
                            BootScreen()
                        } else {
                            val terminalVm: TerminalViewModel = viewModel(
                                key = "terminal-${activeSession.actorRef}",
                                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                                    @Suppress("UNCHECKED_CAST")
                                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                        return TerminalViewModel(store, activeSession) as T
                                    }
                                }
                            )

                            TerminalScreen(
                                viewModel = terminalVm,
                                onBackToDashboard = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun setupVmSafeSaveSession(
        store: SecureSettingsStore,
        session: Session
    ) {
        lifecycleScope.launch {
            store.saveSession(
                StoredSession(
                    token = session.token,
                    actorRef = session.actorRef
                )
            )
        }
    }
}
