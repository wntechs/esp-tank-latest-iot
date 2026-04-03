package com.wntechs.tankcontroller.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wntechs.tankcontroller.data.model.LoginRequest
import com.wntechs.tankcontroller.data.model.RegisterRequest
import com.wntechs.tankcontroller.data.repository.UserRepository
import com.wntechs.tankcontroller.util.AppResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false,
    val name: String? = null,
    val email: String? = null
)

class AuthViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userRepository.authState.collect { state ->
                _uiState.update { it.copy(
                    isLoggedIn = state.isLoggedIn,
                    name = state.name,
                    email = state.email
                ) }
            }
        }
    }

    fun login(email: String, password: String, deviceName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = userRepository.login(LoginRequest(email, password, deviceName))
            _uiState.update { it.copy(isLoading = false) }
            if (result is AppResult.Error) {
                _uiState.update { it.copy(error = result.message) }
            }
        }
    }

    fun register(name: String, email: String, password: String, passwordConfirmation: String, deviceName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = userRepository.register(RegisterRequest(name, email, password, passwordConfirmation, deviceName))
            _uiState.update { it.copy(isLoading = false) }
            if (result is AppResult.Error) {
                _uiState.update { it.copy(error = result.message) }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            userRepository.logout()
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
