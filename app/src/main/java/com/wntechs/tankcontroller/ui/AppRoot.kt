package com.wntechs.tankcontroller.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.wntechs.tankcontroller.ui.screens.SettingsScreen
import com.wntechs.tankcontroller.ui.screens.ManualControlScreen
import com.wntechs.tankcontroller.ui.viewmodel.ConfigurationViewModel
import com.wntechs.tankcontroller.ui.viewmodel.DashboardViewModel
import com.wntechs.tankcontroller.ui.viewmodel.DiscoveryViewModel
import com.wntechs.tankcontroller.ui.viewmodel.SettingsViewModel

@Composable
fun AppRoot(factory: ViewModelProvider.Factory) {
    val navController = rememberNavController()
    val dashboardViewModel: DashboardViewModel = viewModel(factory = factory)
    val discoveryViewModel: DiscoveryViewModel = viewModel(factory = factory)
    val configurationViewModel: ConfigurationViewModel = viewModel(factory = factory)
    val settingsViewModel: SettingsViewModel = viewModel(factory = factory)

    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = currentRoute != NavRoute.Discovery.route

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    val items = listOf(
                        NavRoute.Dashboard to ("Dashboard" to Icons.Default.Home),
                        NavRoute.Manual to ("Manual" to Icons.Default.PowerSettingsNew),
                        NavRoute.Config to ("Config" to Icons.Default.Build),
                        NavRoute.Settings to ("Settings" to Icons.Default.Settings),
                    )
                    items.forEach { (route, meta) ->
                        NavigationBarItem(
                            selected = currentRoute == route.route,
                            onClick = { navController.navigate(route.route) },
                            icon = { Icon(meta.second, null) },
                            label = { Text(meta.first) },
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = NavRoute.Discovery.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(NavRoute.Discovery.route) {
                val ui by discoveryViewModel.uiState.collectAsStateWithLifecycle()
                DeviceDiscoveryScreen(
                    discoveredDevices = ui.discoveredDevices,
                    manualHost = ui.manualHost,
                    isScanning = ui.isScanning,
                    selectedIndex = ui.selectedIndex,
                    onManualHostChange = discoveryViewModel::updateManualHost,
                    onScanClick = discoveryViewModel::startScan,
                    onDeviceSelected = discoveryViewModel::selectIndex,
                    onConnectClick = {
                        discoveryViewModel.connectSelected()
                        navController.navigate(NavRoute.Dashboard.route)
                    },
                    onUseManualAddress = {
                        discoveryViewModel.connectManual()
                        navController.navigate(NavRoute.Dashboard.route)
                    },
                    message = ui.message,
                )
            }

            composable(NavRoute.Dashboard.route) {
                val ui by dashboardViewModel.uiState.collectAsStateWithLifecycle()
                DashboardScreen(
                    uiState = ui,
                    onRefresh = dashboardViewModel::refresh,
                    onOpenManual = { navController.navigate(NavRoute.Manual.route) },
                    onOpenConfig = { navController.navigate(NavRoute.Config.route) },
                )
            }

            composable(NavRoute.Manual.route) {
                val ui by dashboardViewModel.uiState.collectAsStateWithLifecycle()
                ManualControlScreen(
                    uiState = ui,
                    onTurnOn = { dashboardViewModel.turnManual(true) },
                    onTurnOff = { dashboardViewModel.turnManual(false) },
                    onReturnAuto = dashboardViewModel::returnAuto,
                )
            }

            composable(NavRoute.Config.route) {
                val ui by configurationViewModel.uiState.collectAsStateWithLifecycle()
                ConfigurationScreen(
                    uiState = ui,
                    onRefresh = configurationViewModel::load,
                    onSave = configurationViewModel::save,
                )
            }

            composable(NavRoute.Settings.route) {
                val ui by settingsViewModel.uiState.collectAsStateWithLifecycle()
                SettingsScreen(
                    uiState = ui,
                    onBaseUrlChanged = settingsViewModel::updateBaseUrl,
                    onPreferMdnsChanged = settingsViewModel::setPreferMdns,
                    onSave = settingsViewModel::save,
                    onReconnect = {
                        navController.navigate(NavRoute.Discovery.route)
                    },
                )
            }
        }
    }
}
