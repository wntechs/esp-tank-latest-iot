package com.wntechs.tankcontroller.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wntechs.tankcontroller.ui.screens.ConfigurationScreen
import com.wntechs.tankcontroller.ui.screens.DashboardScreen
import com.wntechs.tankcontroller.ui.screens.DeviceDiscoveryScreen
import com.wntechs.tankcontroller.ui.screens.LoginScreen
import com.wntechs.tankcontroller.ui.screens.RegisterScreen
import com.wntechs.tankcontroller.ui.screens.SettingsScreen
import com.wntechs.tankcontroller.ui.viewmodel.AuthViewModel
import com.wntechs.tankcontroller.ui.viewmodel.ConfigurationViewModel
import com.wntechs.tankcontroller.ui.viewmodel.DashboardViewModel
import com.wntechs.tankcontroller.ui.viewmodel.PairingViewModel
import com.wntechs.tankcontroller.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(factory: ViewModelProvider.Factory) {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel(factory = factory)
    val dashboardViewModel: DashboardViewModel = viewModel(factory = factory)
    val configurationViewModel: ConfigurationViewModel = viewModel(factory = factory)
    val settingsViewModel: SettingsViewModel = viewModel(factory = factory)
    val pairingViewModel: PairingViewModel = viewModel(factory = factory)

    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()
    val pairingUiState by pairingViewModel.uiState.collectAsStateWithLifecycle()

    // Automatic redirection based on Auth state
    LaunchedEffect(authUiState.isLoggedIn) {
        if (authUiState.isLoggedIn) {
            // Redirect to Dashboard after login if on an auth screen
            if (currentRoute == NavRoute.Login.route || currentRoute == NavRoute.Register.route || currentRoute == null) {
                navController.navigate(NavRoute.Dashboard.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        } else {
            // Redirect to Login if logged out
            if (currentRoute != NavRoute.Login.route && currentRoute != NavRoute.Register.route) {
                navController.navigate(NavRoute.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    // Redirect to Dashboard after successful pairing
    LaunchedEffect(pairingUiState.pairingSuccess) {
        if (pairingUiState.pairingSuccess) {
            navController.navigate(NavRoute.Dashboard.route) {
                popUpTo(NavRoute.Discovery.route) { inclusive = true }
            }
        }
    }

    val title = when (currentRoute) {
        NavRoute.Login.route -> "Login"
        NavRoute.Register.route -> "Register"
        NavRoute.Dashboard.route -> "Water Tank Dashboard"
        NavRoute.Config.route -> "Tank Configuration"
        NavRoute.Settings.route -> "App Settings"
        NavRoute.Discovery.route -> "Pair New Device"
        else -> "Tank Controller"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                windowInsets = TopAppBarDefaults.windowInsets
            )
        },
        bottomBar = {
            if (authUiState.isLoggedIn) {
                NavigationBar {
                    val items = listOf(
                        Triple(NavRoute.Dashboard, "Dashboard", Icons.Default.Home),
                        Triple(NavRoute.Discovery, "Add", Icons.Default.Add),
                        Triple(NavRoute.Config, "Config", Icons.Default.Build),
                        Triple(NavRoute.Settings, "Settings", Icons.Default.Settings),
                    )
                    items.forEach { (route, label, icon) ->
                        NavigationBarItem(
                            selected = currentRoute == route.route,
                            onClick = {
                                navController.navigate(route.route) {
                                    popUpTo(NavRoute.Dashboard.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(icon, null) },
                            label = { Text(label) },
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = if (authUiState.isLoggedIn) NavRoute.Dashboard.route else NavRoute.Login.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(NavRoute.Login.route) {
                LoginScreen(
                    uiState = authUiState,
                    onLogin = authViewModel::login,
                    onNavigateToRegister = { navController.navigate(NavRoute.Register.route) }
                )
            }

            composable(NavRoute.Register.route) {
                RegisterScreen(
                    uiState = authUiState,
                    onRegister = authViewModel::register,
                    onNavigateToLogin = { navController.navigate(NavRoute.Login.route) }
                )
            }

            composable(NavRoute.Discovery.route) {
                DeviceDiscoveryScreen(
                    uiState = pairingUiState,
                    onStartPairing = pairingViewModel::startPairing
                )
            }

            composable(NavRoute.Dashboard.route) {
                val ui by dashboardViewModel.uiState.collectAsStateWithLifecycle()
                DashboardScreen(
                    uiState = ui,
                    onTurnOn = { dashboardViewModel.turnManual(true) },
                    onTurnOff = { dashboardViewModel.turnManual(false) },
                    onReturnAuto = dashboardViewModel::returnAuto,
                    onOpenConfig = { navController.navigate(NavRoute.Config.route) },
                    onNavigateToDiscovery = { navController.navigate(NavRoute.Discovery.route) }
                )
            }

            composable(NavRoute.Config.route) {
                val ui by configurationViewModel.uiState.collectAsStateWithLifecycle()
                ConfigurationScreen(
                    uiState = ui,
                    onUpdateField = configurationViewModel::updateField,
                    onSave = configurationViewModel::save,
                )
            }

            composable(NavRoute.Settings.route) {
                val ui by settingsViewModel.uiState.collectAsStateWithLifecycle()
                SettingsScreen(
                    uiState = ui,
                    authUiState = authUiState,
                    onBaseUrlChanged = settingsViewModel::updateBaseUrl,
                    onDeviceIdChanged = settingsViewModel::updateDeviceId,
                    onFamilySelected = settingsViewModel::selectFamily,
                    onModelSelected = settingsViewModel::selectModel,
                    onApplyPreset = settingsViewModel::applySelectedPreset,
                    onSave = settingsViewModel::save,
                    onLogout = authViewModel::logout
                )
            }
        }
    }
}
