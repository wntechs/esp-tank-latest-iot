package com.wntechs.tankcontroller.ui

sealed class NavRoute(val route: String) {
    data object Login : NavRoute("login")
    data object Register : NavRoute("register")
    data object Dashboard : NavRoute("dashboard")
    data object Config : NavRoute("config")
    data object Settings : NavRoute("settings")
}
