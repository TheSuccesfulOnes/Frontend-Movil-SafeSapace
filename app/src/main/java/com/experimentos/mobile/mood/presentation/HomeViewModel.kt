package com.experimentos.mobile.mood.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.io.EOFException
import com.experimentos.mobile.mood.data.Mood
import com.experimentos.mobile.mood.data.MoodApi
import com.experimentos.mobile.mood.data.SubmitMoodRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.experimentos.mobile.shared.presentation.toUserMessage

data class HomeUiState(
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val selectedMood: Mood? = null,
    val errorMessage: String? = null,
)

class HomeViewModel(private val moodApi: MoodApi) : ViewModel() {
    private val mutableState = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = mutableState.asStateFlow()

    init {
        loadToday()
    }

    fun loadToday() {
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(isLoading = true, errorMessage = null)
            runCatching { moodApi.getToday() }
                .onSuccess { response ->
                    mutableState.value = HomeUiState(
                        isLoading = false,
                        selectedMood = response?.mood,
                    )
                }
                .onFailure { error ->
                    if (error is EOFException) {
                        // The backend uses an empty 200 response when no mood exists for today.
                        mutableState.value = HomeUiState(isLoading = false)
                        return@onFailure
                    }
                    // A missing mood is a valid first-day state, not an application error.
                    mutableState.value = HomeUiState(
                        isLoading = false,
                        errorMessage = error.toUserMessage("No se pudo cargar tu estado de ánimo."),
                    )
                }
        }
    }

    fun submitMood(mood: Mood) {
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(isSubmitting = true, errorMessage = null)
            runCatching { moodApi.submitToday(SubmitMoodRequest(mood)) }
                .onSuccess { response ->
                    mutableState.value = HomeUiState(
                        isLoading = false,
                        isSubmitting = false,
                        selectedMood = response.mood,
                    )
                }
                .onFailure { error ->
                    mutableState.value = mutableState.value.copy(
                        isSubmitting = false,
                        errorMessage = error.toUserMessage("No se pudo guardar tu estado de ánimo."),
                    )
                }
        }
    }
}
