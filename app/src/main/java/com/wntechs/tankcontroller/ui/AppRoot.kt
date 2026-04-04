package com.wntechs.tankcontroller.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
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
import com.wntechs.tankcontroller.ui.screens.DeviceSelector
import com.wntechs.tankcontroller.ui.screens.LoginScreen
import com.wntechs.tankcontroller.ui.screens.RegisterScreen
import com.wntechs.tankcontroller.ui.screens.SettingsScreen
import com.wntechs.tankcontroller.ui.screens.SplashScreen
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
    val currentRoute = backStack?.destination?.route ?: NavRoute.Splash.route
    val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()
    val pairingUiState by pairingViewModel.uiState.collectAsStateWithLifecycle()

    val isAuthScreen = currentRoute == NavRoute.Login.route || 
                      currentRoute == NavRoute.Register.route || 
                      currentRoute == NavRoute.Splash.route

    // Redirection Logic
    LaunchedEffect(authUiState.isInitializing, authUiState.isLoggedIn, currentRoute) {
        if (authUiState.isInitializing) return@LaunchedEffect

        val isLoggedIn = authUiState.isLoggedIn
        val isOnAuthScreen = currentRoute == NavRoute.Login.route || currentRoute == NavRoute.Register.route
        val isOnSplash = currentRoute == NavRoute.Splash.route || currentRoute == null

        if (isLoggedIn) {
            if (isOnSplash || isOnAuthScreen) {
                navController.navigate(NavRoute.Dashboard.route) {
                    popUpTo(NavRoute.Splash.route) { inclusive = true }
                }
            }
        } else {
            if (isOnSplash || !isOnAuthScreen) {
                navController.navigate(NavRoute.Login.route) {
                    popUpTo(NavRoute.Splash.route) { inclusive = true }
                }
            }
        }
    }

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
        NavRoute.Dashboard.route -> "Dashboard"
        NavRoute.Config.route -> "Configuration"
        NavRoute.Settings.route -> "Settings"
        NavRoute.Discovery.route -> "Pair Device"
        else -> "Tank Controller"
    }

    Scaffold(
        topBar = {
            if (!isAuthScreen) {
                Column {
                    TopAppBar(
                        title = { Text(title) },
                        navigationIcon = {
                            if (currentRoute == NavRoute.Discovery.route) {
                                IconButton(onClick = { navController.popBackStack() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            }
                        },
                        windowInsets = TopAppBarDefaults.windowInsets
                    )
                    if (authUiState.isLoggedIn && authUiState.devices.isNotEmpty() && currentRoute != NavRoute.Discovery.route) {
                        Box(modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 8.dp)
                        ) {
                            DeviceSelector(
                                devices = authUiState.devices,
                                selectedUuid = authUiState.selectedDeviceUuid,
                                onDeviceSelected = authViewModel::selectDevice,
                                onAddNewDevice = { 
                                    navController.navigate(NavRoute.Discovery.route) {
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            if (authUiState.isLoggedIn && !isAuthScreen) {
                NavigationBar {
                    val items = listOf(
                        NavRoute.Dashboard to ("Home" to Icons.Default.Home),
                        NavRoute.Config to ("Config" to Icons.Default.Build),
                        NavRoute.Settings to ("Settings" to Icons.Default.Settings),
                    )
                    items.forEach { (route, meta) ->
                        NavigationBarItem(
                            selected = currentRoute == route.route,
                            onClick = {
                                if (currentRoute != route.route) {
                                    navController.navigate(route.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                } else if (route.route == NavRoute.Dashboard.route) {
                                    navController.popBackStack(NavRoute.Dashboard.route, inclusive = false)
                                }
                            },
                            icon = { Icon(meta.second, null) },
                            label = { Text(meta.first) },
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavRoute.Splash.route,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            composable(NavRoute.Splash.route) {
                SplashScreen()
            }
            composable(NavRoute.Login.route) {
                LoginScreen(uiState = authUiState, onLogin = authViewModel::login, onNavigateToRegister = { navController.navigate(NavRoute.Register.route) })
            }
            composable(NavRoute.Register.route) {
                RegisterScreen(uiState = authUiState, onRegister = authViewModel::register, onNavigateToLogin = { navController.navigate(NavRoute.Login.route) })
            }
            composable(NavRoute.Discovery.route) {
                DeviceDiscoveryScreen(uiState = pairingUiState, onStartPairing = pairingViewModel::startPairing)
            }
            composable(NavRoute.Dashboard.route) {
                val ui by dashboardViewModel.uiState.collectAsStateWithLifecycle()
                DashboardScreen(
                    uiState = ui,
                    onTurnOn = { dashboardViewModel.turnManual(true) },
                    onTurnOff = { dashboardViewModel.turnManual(false) },
                    onReturnAuto = dashboardViewModel::returnAuto,
                    onOpenConfig = { navController.navigate(NavRoute.Config.route) },
                    onNavigateToDiscovery = { 
                        navController.navigate(NavRoute.Discovery.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(NavRoute.Config.route) {
                val ui by configurationViewModel.uiState.collectAsStateWithLifecycle()
                ConfigurationScreen(uiState = ui, onUpdateField = configurationViewModel::updateField, onSave = configurationViewModel::save)
            }
            composable(NavRoute.Settings.route) {
                val ui by settingsViewModel.uiState.collectAsStateWithLifecycle()
                SettingsScreen(
                    uiState = ui,
                    authUiState = authUiState,
                    onFamilySelected = settingsViewModel::selectFamily,
                    onModelSelected = settingsViewModel::selectModel,
                    onApplyPreset = settingsViewModel::applySelectedPreset,
                    onLogout = authViewModel::logout,
                    onResetDevice = settingsViewModel::resetDevice
                )
            }
        }
    }
}
