package com.experimentos.mobile.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.experimentos.mobile.profile.data.ProfileApi
import com.experimentos.mobile.profile.data.ProfileResponse
import com.experimentos.mobile.profile.data.UpdateAccountRequest
import com.experimentos.mobile.profile.data.UpdatePreferencesRequest
import com.experimentos.mobile.shared.data.AppearanceStore
import com.experimentos.mobile.shared.data.SessionStore
import com.experimentos.mobile.shared.presentation.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = true,
    val profile: ProfileResponse? = null,
    val isSaving: Boolean = false,
    val message: String? = null,
    val errorMessage: String? = null,
)

class ProfileViewModel(
    private val profileApi: ProfileApi,
    private val appearanceStore: AppearanceStore,
    private val sessionStore: SessionStore,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = mutableState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(isLoading = true, errorMessage = null)
            runCatching { profileApi.getProfile() }
                .onSuccess { profile ->
                    appearanceStore.syncAccountPreferences(
                        accountKey = "user:${profile.userId}",
                        language = profile.language,
                        theme = profile.theme,
                    )
                    mutableState.value = mutableState.value.copy(isLoading = false, profile = profile)
                }
                .onFailure { error ->
                    mutableState.value = mutableState.value.copy(
                        isLoading = false,
                        errorMessage = error.toUserMessage("No se pudo cargar tu perfil."),
                    )
                }
        }
    }

    fun clearFeedback() {
        mutableState.value = mutableState.value.copy(message = null, errorMessage = null)
    }

    fun updatePreferences(language: String, theme: String) {
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(isSaving = true, errorMessage = null)
            runCatching { profileApi.updatePreferences(UpdatePreferencesRequest(language, theme)) }
                .onSuccess { profile ->
                    appearanceStore.syncAccountPreferences(
                        accountKey = "user:${profile.userId}",
                        language = profile.language,
                        theme = profile.theme,
                    )
                    mutableState.value = mutableState.value.copy(
                        isSaving = false,
                        profile = profile,
                        message = "Preferencias actualizadas.",
                    )
                }
                .onFailure { error ->
                    mutableState.value = mutableState.value.copy(
                        isSaving = false,
                        errorMessage = error.toUserMessage("No se pudieron actualizar las preferencias."),
                    )
                }
        }
    }

    fun updateAccount(username: String, email: String, displayName: String) {
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(isSaving = true, errorMessage = null)
            runCatching {
                profileApi.updateAccount(
                    UpdateAccountRequest(
                        username = username.trim(),
                        email = email.trim().ifBlank { null },
                        displayName = displayName.trim(),
                    ),
                )
            }
                .onSuccess { response ->
                    appearanceStore.syncAccountPreferences(
                        accountKey = "user:${response.profile.userId}",
                        language = response.profile.language,
                        theme = response.profile.theme,
                    )
                    sessionStore.updateAccount(
                        token = response.token,
                        username = response.profile.username,
                        displayName = response.profile.displayName,
                        userId = response.profile.userId,
                    )
                    mutableState.value = mutableState.value.copy(
                        isSaving = false,
                        profile = response.profile,
                        message = "Datos de cuenta actualizados.",
                    )
                }
                .onFailure { error ->
                    mutableState.value = mutableState.value.copy(
                        isSaving = false,
                        errorMessage = error.toUserMessage("No se pudieron actualizar tus datos."),
                    )
                }
        }
    }
}
