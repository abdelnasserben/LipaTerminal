package com.kori.terminal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kori.terminal.data.secure.SecureSettingsStore
import com.kori.terminal.ui.boot.BootScreen
import com.kori.terminal.ui.dashboard.DashboardScreen
import com.kori.terminal.ui.dashboard.DashboardViewModel
import com.kori.terminal.ui.setup.SetupScreen
import com.kori.terminal.ui.setup.SetupViewModel
import com.kori.terminal.ui.terminal.TerminalScreen
import com.kori.terminal.ui.terminal.TerminalViewModel
import com.kori.terminal.ui.theme.KoriTerminalTheme

private object Routes {
    const val Boot = "boot"
    const val Setup = "setup"
    const val Dashboard = "dashboard"
    const val Terminal = "terminal"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            KoriTerminalTheme {
                val navController = rememberNavController()
                val store = remember { SecureSettingsStore(applicationContext) }

                NavHost(
                    navController = navController,
                    startDestination = Routes.Boot
                ) {
                    composable(Routes.Boot) {
                        val cfg by store.configFlow().collectAsState(initial = null)

                        LaunchedEffect(cfg) {
                            val dest = if (cfg == null) Routes.Setup else Routes.Dashboard
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
                                navController.navigate(Routes.Dashboard) {
                                    popUpTo(Routes.Setup) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(Routes.Dashboard) {
                        val dashboardVm: DashboardViewModel = viewModel(
                            factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                                @Suppress("UNCHECKED_CAST")
                                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                    return DashboardViewModel(store) as T
                                }
                            }
                        )

                        DashboardScreen(
                            viewModel = dashboardVm,
                            onOpenTerminal = {
                                navController.navigate(Routes.Terminal)
                            },
                            onResetConfig = {
                                store.clear()
                                navController.navigate(Routes.Setup) {
                                    popUpTo(Routes.Dashboard) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(Routes.Terminal) {
                        val terminalVm: TerminalViewModel = viewModel(
                            factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                                @Suppress("UNCHECKED_CAST")
                                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                    return TerminalViewModel(store) as T
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
