package com.wntechs.tankcontroller.ui

sealed class NavRoute(val route: String) {
    data object Discovery : NavRoute("discovery")
    data object Dashboard : NavRoute("dashboard")
    data object Config : NavRoute("config")
    data object Settings : NavRoute("settings")
}
