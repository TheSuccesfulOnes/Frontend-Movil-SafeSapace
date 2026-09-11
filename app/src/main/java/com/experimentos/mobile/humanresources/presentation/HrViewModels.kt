package com.experimentos.mobile.humanresources.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.experimentos.mobile.activity.data.ActivityApi
import com.experimentos.mobile.activity.data.ActivityResponse
import com.experimentos.mobile.activity.data.CreateActivityRequest
import com.experimentos.mobile.comment.data.CommentApi
import com.experimentos.mobile.comment.data.CommentResponse
import com.experimentos.mobile.mood.data.Mood
import com.experimentos.mobile.mood.data.MoodApi
import com.experimentos.mobile.mood.data.MoodSummary
import com.experimentos.mobile.shared.presentation.toUserMessage
import com.experimentos.mobile.survey.data.CreateSurveyRequest
import com.experimentos.mobile.survey.data.SurveyApi
import com.experimentos.mobile.survey.data.SurveyResponse
import com.experimentos.mobile.survey.data.SurveyType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HrHomeUiState(
    val isLoading: Boolean = true,
    val summary: MoodSummary? = null,
    val errorMessage: String? = null,
)

class HrHomeViewModel(private val moodApi: MoodApi) : ViewModel() {
    private val mutableState = MutableStateFlow(HrHomeUiState())
    val state: StateFlow<HrHomeUiState> = mutableState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(isLoading = true, errorMessage = null)
            runCatching { moodApi.getSummary() }
                .onSuccess { summary ->
                    mutableState.value = HrHomeUiState(isLoading = false, summary = summary)
                }
                .onFailure { error ->
                    mutableState.value = HrHomeUiState(
                        isLoading = false,
                        errorMessage = error.toUserMessage("No se pudo cargar el resumen de bienestar."),
                    )
                }
        }
    }
}

data class HrContentUiState(
    val isLoading: Boolean = true,
    val surveys: List<SurveyResponse> = emptyList(),
    val activities: List<ActivityResponse> = emptyList(),
    val comments: Map<Long, List<CommentResponse>> = emptyMap(),
    val expandedSurveyId: Long? = null,
    val commentsLoadingId: Long? = null,
    val commentsErrorMessage: String? = null,
    val isSubmitting: Boolean = false,
    val message: String? = null,
    val errorMessage: String? = null,
)

class HrContentViewModel(
    private val surveyApi: SurveyApi,
    private val activityApi: ActivityApi,
    private val commentApi: CommentApi,
) : ViewModel() {
    private val mutableState = MutableStateFlow(HrContentUiState())
    val state: StateFlow<HrContentUiState> = mutableState.asStateFlow()

    init {
        load()
    }

    fun clearFeedback() {
        mutableState.value = mutableState.value.copy(message = null, errorMessage = null)
    }

    fun load() {
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(
                isLoading = true,
                comments = emptyMap(),
                expandedSurveyId = null,
                commentsLoadingId = null,
                commentsErrorMessage = null,
                errorMessage = null,
            )
            val surveys = runCatching { surveyApi.getManaged() }
            val activities = runCatching { activityApi.getManaged() }
            if (surveys.isSuccess && activities.isSuccess) {
                mutableState.value = mutableState.value.copy(
                    isLoading = false,
                    surveys = surveys.getOrDefault(emptyList()),
                    activities = activities.getOrDefault(emptyList()),
                )
            } else {
                mutableState.value = mutableState.value.copy(
                    isLoading = false,
                    errorMessage = "No se pudo cargar el contenido administrable.",
                )
            }
        }
    }

    /** Loads anonymous survey comments only when the HR member opens a survey thread. */
    fun toggleSurveyComments(surveyId: Long) {
        if (mutableState.value.expandedSurveyId == surveyId) {
            mutableState.value = mutableState.value.copy(expandedSurveyId = null)
            return
        }

        mutableState.value = mutableState.value.copy(
            expandedSurveyId = surveyId,
            commentsErrorMessage = null,
        )
        if (mutableState.value.comments.containsKey(surveyId)) return

        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(commentsLoadingId = surveyId)
            runCatching { commentApi.getComments(surveyId) }
                .onSuccess { comments ->
                    val updatedComments = mutableState.value.comments.toMutableMap()
                    updatedComments[surveyId] = comments
                    mutableState.value = mutableState.value.copy(
                        comments = updatedComments,
                        commentsLoadingId = null,
                    )
                }
                .onFailure { error ->
                    mutableState.value = mutableState.value.copy(
                        commentsLoadingId = null,
                        commentsErrorMessage = error.toUserMessage(
                            "No se pudieron cargar los comentarios.",
                        ),
                    )
                }
        }
    }

    fun createSurvey(title: String, question: String, type: SurveyType, allowComments: Boolean) {
        if (title.isBlank() || question.isBlank()) {
            mutableState.value = mutableState.value.copy(errorMessage = "Completa el título y la pregunta.")
            return
        }
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(
                isSubmitting = true,
                message = null,
                errorMessage = null,
            )
            runCatching { surveyApi.create(CreateSurveyRequest(title.trim(), question.trim(), type, allowComments)) }
                .onSuccess { survey ->
                    mutableState.value = mutableState.value.copy(
                        isSubmitting = false,
                        surveys = listOf(survey) + mutableState.value.surveys,
                        message = "Encuesta creada como borrador.",
                    )
                }
                .onFailure { error ->
                    mutableState.value = mutableState.value.copy(
                        isSubmitting = false,
                        errorMessage = error.toUserMessage("No se pudo crear la encuesta."),
                    )
                }
        }
    }

    fun changeSurveyStatus(id: Long, publish: Boolean) {
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(
                isSubmitting = true,
                message = null,
                errorMessage = null,
            )
            runCatching { if (publish) surveyApi.publish(id) else surveyApi.close(id) }
                .onSuccess { updated ->
                    mutableState.value = mutableState.value.copy(
                        isSubmitting = false,
                        // The list endpoint only returns published surveys. Remove a closed
                        // survey locally so the screen never presents a stale action.
                        surveys = if (publish) {
                            mutableState.value.surveys.map { if (it.id == id) updated else it }
                        } else {
                            mutableState.value.surveys.filterNot { it.id == id }
                        },
                        message = "Estado de la encuesta actualizado.",
                    )
                }
                .onFailure { error ->
                    mutableState.value = mutableState.value.copy(
                        isSubmitting = false,
                        errorMessage = error.toUserMessage("No se pudo cambiar el estado de la encuesta."),
                    )
                }
        }
    }

    fun createActivity(title: String, description: String, optionsText: String) {
        val options = optionsText.lines().map(String::trim).filter(String::isNotBlank)
        if (title.isBlank() || options.size < 2) {
            mutableState.value = mutableState.value.copy(errorMessage = "Escribe un título y al menos dos opciones.")
            return
        }
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(
                isSubmitting = true,
                message = null,
                errorMessage = null,
            )
            runCatching { activityApi.create(CreateActivityRequest(title.trim(), description.trim(), options)) }
                .onSuccess { activity ->
                    mutableState.value = mutableState.value.copy(
                        isSubmitting = false,
                        activities = listOf(activity) + mutableState.value.activities,
                        message = "Actividad creada y abierta para votación.",
                    )
                }
                .onFailure { error ->
                    mutableState.value = mutableState.value.copy(
                        isSubmitting = false,
                        errorMessage = error.toUserMessage("No se pudo crear la actividad."),
                    )
                }
        }
    }

    fun closeActivity(id: Long) {
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(
                isSubmitting = true,
                message = null,
                errorMessage = null,
            )
            runCatching { activityApi.close(id) }
                .onSuccess {
                    mutableState.value = mutableState.value.copy(
                        isSubmitting = false,
                        activities = mutableState.value.activities.filterNot { it.id == id },
                        message = "Actividad cerrada.",
                    )
                }
                .onFailure { error ->
                    mutableState.value = mutableState.value.copy(
                        isSubmitting = false,
                        errorMessage = error.toUserMessage("No se pudo cerrar la actividad."),
                    )
                }
        }
    }
}
