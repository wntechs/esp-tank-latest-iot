package com.wntechs.tankcontroller.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.wntechs.tankcontroller.data.AppContainer
import com.wntechs.tankcontroller.ui.viewmodel.AuthViewModel
import com.wntechs.tankcontroller.ui.viewmodel.ConfigurationViewModel
import com.wntechs.tankcontroller.ui.viewmodel.DashboardViewModel
import com.wntechs.tankcontroller.ui.viewmodel.PairingViewModel
import com.wntechs.tankcontroller.ui.viewmodel.SettingsViewModel

class AppViewModelFactory(
    private val container: AppContainer,
    private val tankMeasurementsJson: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return when {
            modelClass.isAssignableFrom(DashboardViewModel::class.java) -> DashboardViewModel(container.deviceRepository, container.userRepository) as T
            modelClass.isAssignableFrom(ConfigurationViewModel::class.java) -> ConfigurationViewModel(container.deviceRepository) as T
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(container.deviceRepository, container.userRepository, tankMeasurementsJson) as T
            modelClass.isAssignableFrom(AuthViewModel::class.java) -> AuthViewModel(container.userRepository) as T
            modelClass.isAssignableFrom(PairingViewModel::class.java) -> PairingViewModel(container.userRepository, container.deviceRepository, container.bleManager) as T
            else -> error("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
