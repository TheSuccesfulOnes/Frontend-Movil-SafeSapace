package com.experimentos.mobile.activity.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.experimentos.mobile.activity.data.ActivityApi
import com.experimentos.mobile.activity.data.ActivityResponse
import com.experimentos.mobile.activity.data.VoteRequest
import com.experimentos.mobile.shared.presentation.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ActivityUiState(
    val isLoading: Boolean = true,
    val activities: List<ActivityResponse> = emptyList(),
    /** Options already accepted by the API for each activity. */
    val selectedOptions: Map<Long, Long> = emptyMap(),
    /** Local choices waiting for explicit confirmation with the Votar button. */
    val pendingOptions: Map<Long, Long> = emptyMap(),
    val isSubmitting: Boolean = false,
    val message: String? = null,
    val errorMessage: String? = null,
)

class ActivityViewModel(private val activityApi: ActivityApi) : ViewModel() {
    private val mutableState = MutableStateFlow(ActivityUiState())
    val state: StateFlow<ActivityUiState> = mutableState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(isLoading = true, errorMessage = null)
            runCatching { activityApi.getOpen() }
                .onSuccess { activities ->
                    mutableState.value = mutableState.value.copy(isLoading = false, activities = activities)
                }
                .onFailure { error ->
                    mutableState.value = mutableState.value.copy(
                        isLoading = false,
                        errorMessage = error.toUserMessage("No se pudieron cargar las actividades."),
                    )
                }
        }
    }

    fun choose(activityId: Long, optionId: Long) {
        val activity = mutableState.value.activities.firstOrNull { it.id == activityId }
        if (activity == null || activity.status != "OPEN" || activity.options.none { it.id == optionId }) {
            return
        }

        mutableState.value = mutableState.value.copy(
            pendingOptions = mutableState.value.pendingOptions + (activityId to optionId),
            errorMessage = null,
            message = null,
        )
    }

    fun vote(activityId: Long) {
        val optionId = mutableState.value.pendingOptions[activityId] ?: return

        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(isSubmitting = true, errorMessage = null)
            runCatching { activityApi.vote(activityId, VoteRequest(optionId)) }
                .onSuccess {
                    mutableState.value = mutableState.value.copy(
                        isSubmitting = false,
                        selectedOptions = mutableState.value.selectedOptions + (activityId to optionId),
                        pendingOptions = mutableState.value.pendingOptions - activityId,
                        message = "Voto registrado. Puedes cambiarlo cuando quieras.",
                    )
                    refresh()
                }
                .onFailure { error ->
                    mutableState.value = mutableState.value.copy(
                        isSubmitting = false,
                        errorMessage = error.toUserMessage("No se pudo registrar el voto."),
                    )
                }
        }
    }

    private suspend fun refresh() {
        runCatching { activityApi.getOpen() }
            .onSuccess { activities ->
                mutableState.value = mutableState.value.copy(activities = activities)
            }
    }
}
