package com.experimentos.mobile.authentication.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.experimentos.mobile.authentication.data.AuthApi
import com.experimentos.mobile.authentication.domain.AuthRepository
import com.experimentos.mobile.authentication.domain.DefaultAuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PasswordRecoveryUiState(
    val isLoading: Boolean = false,
    val requestSent: Boolean = false,
    val completed: Boolean = false,
    val errorMessage: String? = null,
)

class PasswordRecoveryViewModel(
    private val repository: AuthRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(PasswordRecoveryUiState())
    val state: StateFlow<PasswordRecoveryUiState> = mutableState.asStateFlow()

    fun requestRecovery(identifier: String) {
        if (identifier.isBlank()) {
            mutableState.value = PasswordRecoveryUiState(errorMessage = "Completa todos los campos.")
            return
        }
        viewModelScope.launch {
            mutableState.value = PasswordRecoveryUiState(isLoading = true)
            repository.requestPasswordRecovery(identifier)
                .onSuccess { mutableState.value = PasswordRecoveryUiState(requestSent = true) }
                .onFailure { error ->
                    mutableState.value = PasswordRecoveryUiState(errorMessage = error.toAuthUserMessage())
                }
        }
    }

    fun confirmRecovery(token: String, newPassword: String, confirmPassword: String) {
        when {
            token.isBlank() || newPassword.isBlank() || confirmPassword.isBlank() -> {
                mutableState.value = mutableState.value.copy(errorMessage = "Completa todos los campos.")
            }
            newPassword != confirmPassword -> {
                mutableState.value = mutableState.value.copy(errorMessage = "Las contraseñas no coinciden.")
            }
            newPassword.length < 8 -> {
                mutableState.value = mutableState.value.copy(
                    errorMessage = "La contraseña debe tener al menos 8 caracteres.",
                )
            }
            else -> viewModelScope.launch {
                mutableState.value = mutableState.value.copy(isLoading = true, errorMessage = null)
                repository.confirmPasswordRecovery(token, newPassword, confirmPassword)
                    .onSuccess { mutableState.value = PasswordRecoveryUiState(completed = true) }
                    .onFailure { error ->
                        mutableState.value = mutableState.value.copy(
                            isLoading = false,
                            errorMessage = error.toAuthUserMessage(),
                        )
                    }
            }
        }
    }
}

class PasswordRecoveryViewModelFactory(
    private val repository: AuthRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(PasswordRecoveryViewModel::class.java))
        return PasswordRecoveryViewModel(repository) as T
    }

    companion object {
        fun forApi(api: AuthApi): PasswordRecoveryViewModelFactory =
            PasswordRecoveryViewModelFactory(DefaultAuthRepository(api))
    }
}
