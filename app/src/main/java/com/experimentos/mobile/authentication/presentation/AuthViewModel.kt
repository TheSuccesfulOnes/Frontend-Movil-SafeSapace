package com.experimentos.mobile.authentication.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.experimentos.mobile.authentication.data.RegisterRequest
import com.experimentos.mobile.authentication.domain.AuthRepository
import com.experimentos.mobile.shared.data.SessionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val registrationCompleted: Boolean = false,
)

class AuthViewModel(
    private val repository: AuthRepository,
    private val sessionStore: SessionStore,
) : ViewModel() {
    private val mutableState = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = mutableState.asStateFlow()

    fun login(identifier: String, password: String) {
        if (identifier.isBlank() || password.isBlank()) {
            mutableState.value = AuthUiState(errorMessage = "Completa todos los campos.")
            return
        }

        viewModelScope.launch {
            mutableState.value = AuthUiState(isLoading = true)
            repository.login(identifier, password)
                .onSuccess { session -> sessionStore.save(session) }
                .onFailure { error ->
                    mutableState.value = AuthUiState(errorMessage = error.toAuthUserMessage())
                }
                .onSuccess { mutableState.value = AuthUiState() }
        }
    }

    fun register(username: String, email: String, password: String, confirmation: String) {
        when {
            username.isBlank() || email.isBlank() || password.isBlank() || confirmation.isBlank() -> {
                mutableState.value = AuthUiState(errorMessage = "Completa todos los campos.")
            }
            password != confirmation -> {
                mutableState.value = AuthUiState(errorMessage = "Las contraseñas no coinciden.")
            }
            password.length < 8 -> {
                mutableState.value = AuthUiState(errorMessage = "La contraseña debe tener al menos 8 caracteres.")
            }
            else -> viewModelScope.launch {
                mutableState.value = AuthUiState(isLoading = true)
                repository.register(RegisterRequest(username.trim(), email.trim(), password, confirmation))
                    .onSuccess { mutableState.value = AuthUiState(registrationCompleted = true) }
                    .onFailure { error -> mutableState.value = AuthUiState(errorMessage = error.toAuthUserMessage()) }
            }
        }
    }

    fun clearMessage() {
        mutableState.value = mutableState.value.copy(errorMessage = null)
    }

}
