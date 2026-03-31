package com.wntechs.tankcontroller.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.wntechs.tankcontroller.data.AppContainer
import com.wntechs.tankcontroller.ui.viewmodel.ConfigurationViewModel
import com.wntechs.tankcontroller.ui.viewmodel.DashboardViewModel
import com.wntechs.tankcontroller.ui.viewmodel.SettingsViewModel

class AppViewModelFactory(
    private val container: AppContainer,
    private val tankMeasurementsJson: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return when {
            modelClass.isAssignableFrom(DashboardViewModel::class.java) -> DashboardViewModel(container.deviceRepository) as T
            modelClass.isAssignableFrom(ConfigurationViewModel::class.java) -> ConfigurationViewModel(container.deviceRepository) as T
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(container.deviceRepository, tankMeasurementsJson) as T
            else -> error("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
