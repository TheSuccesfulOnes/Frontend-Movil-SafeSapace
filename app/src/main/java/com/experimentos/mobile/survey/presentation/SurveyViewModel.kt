package com.experimentos.mobile.survey.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.experimentos.mobile.comment.data.CommentApi
import com.experimentos.mobile.comment.data.CommentResponse
import com.experimentos.mobile.comment.data.CreateCommentRequest
import com.experimentos.mobile.shared.presentation.toUserMessage
import com.experimentos.mobile.survey.data.AnswerRequest
import com.experimentos.mobile.survey.data.SurveyApi
import com.experimentos.mobile.survey.data.SurveyResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SurveyUiState(
    val isLoading: Boolean = true,
    val surveys: List<SurveyResponse> = emptyList(),
    val comments: Map<Long, List<CommentResponse>> = emptyMap(),
    val expandedSurveyId: Long? = null,
    val answeredSurveyIds: Set<Long> = emptySet(),
    val isSubmitting: Boolean = false,
    val message: String? = null,
    val errorMessage: String? = null,
)

class SurveyViewModel(
    private val surveyApi: SurveyApi,
    private val commentApi: CommentApi,
) : ViewModel() {
    private val mutableState = MutableStateFlow(SurveyUiState())
    val state: StateFlow<SurveyUiState> = mutableState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(isLoading = true, errorMessage = null)
            runCatching { surveyApi.getPublished() }
                .onSuccess { surveys ->
                    mutableState.value = mutableState.value.copy(isLoading = false, surveys = surveys)
                }
                .onFailure { error ->
                    mutableState.value = mutableState.value.copy(
                        isLoading = false,
                        errorMessage = error.toUserMessage("No se pudieron cargar las encuestas."),
                    )
                }
        }
    }

    fun toggleComments(surveyId: Long) {
        if (mutableState.value.expandedSurveyId == surveyId) {
            mutableState.value = mutableState.value.copy(expandedSurveyId = null)
            return
        }

        mutableState.value = mutableState.value.copy(expandedSurveyId = surveyId, errorMessage = null)
        if (mutableState.value.comments.containsKey(surveyId)) return

        viewModelScope.launch {
            runCatching { commentApi.getComments(surveyId) }
                .onSuccess { comments ->
                    val updated = mutableState.value.comments.toMutableMap()
                    updated[surveyId] = comments
                    mutableState.value = mutableState.value.copy(comments = updated)
                }
                .onFailure { error ->
                    mutableState.value = mutableState.value.copy(
                        errorMessage = error.toUserMessage("No se pudieron cargar los comentarios."),
                    )
                }
        }
    }

    fun answer(surveyId: Long, answerText: String) {
        if (answerText.isBlank()) {
            mutableState.value = mutableState.value.copy(errorMessage = "Escribe una respuesta antes de enviarla.")
            return
        }

        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(isSubmitting = true, errorMessage = null)
            runCatching { surveyApi.answer(surveyId, AnswerRequest(answerText.trim())) }
                .onSuccess {
                    mutableState.value = mutableState.value.copy(
                        isSubmitting = false,
                        answeredSurveyIds = mutableState.value.answeredSurveyIds + surveyId,
                        message = "Respuesta enviada correctamente.",
                    )
                    // Refresh the aggregate answer count shown on the card.
                    refreshSurveys()
                }
                .onFailure { error ->
                    mutableState.value = mutableState.value.copy(
                        isSubmitting = false,
                        errorMessage = error.toUserMessage("No se pudo enviar la respuesta."),
                    )
                }
        }
    }

    fun createComment(surveyId: Long, content: String, parentId: Long? = null) {
        if (content.isBlank()) {
            mutableState.value = mutableState.value.copy(errorMessage = "Escribe un comentario antes de publicarlo.")
            return
        }

        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(isSubmitting = true, errorMessage = null)
            runCatching {
                commentApi.create(surveyId, CreateCommentRequest(content.trim(), parentId))
            }.onSuccess {
                refreshComments(surveyId)
                mutableState.value = mutableState.value.copy(
                    isSubmitting = false,
                    message = "Comentario publicado.",
                )
            }.onFailure { error ->
                mutableState.value = mutableState.value.copy(
                    isSubmitting = false,
                    errorMessage = error.toUserMessage("No se pudo publicar el comentario."),
                )
            }
        }
    }

    fun likeComment(surveyId: Long, commentId: Long) {
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(isSubmitting = true, errorMessage = null)
            runCatching { commentApi.like(surveyId, commentId) }
                .onSuccess {
                    refreshComments(surveyId)
                    mutableState.value = mutableState.value.copy(isSubmitting = false)
                }
                .onFailure { error ->
                    mutableState.value = mutableState.value.copy(
                        isSubmitting = false,
                        errorMessage = error.toUserMessage("No se pudo marcar el comentario."),
                    )
            }
        }
    }

    fun deleteComment(surveyId: Long, commentId: Long) {
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(isSubmitting = true, errorMessage = null)
            runCatching { commentApi.delete(surveyId, commentId) }
                .onSuccess {
                    refreshComments(surveyId)
                    mutableState.value = mutableState.value.copy(
                        isSubmitting = false,
                        message = "Comentario eliminado.",
                    )
                }
                .onFailure { error ->
                    mutableState.value = mutableState.value.copy(
                        isSubmitting = false,
                        errorMessage = error.toUserMessage("No se pudo eliminar el comentario."),
                    )
                }
        }
    }

    fun clearMessage() {
        mutableState.value = mutableState.value.copy(message = null, errorMessage = null)
    }

    private suspend fun refreshComments(surveyId: Long) {
        runCatching { commentApi.getComments(surveyId) }
            .onSuccess { comments ->
                val updated = mutableState.value.comments.toMutableMap()
                updated[surveyId] = comments
                mutableState.value = mutableState.value.copy(comments = updated)
            }
    }

    private suspend fun refreshSurveys() {
        runCatching { surveyApi.getPublished() }
            .onSuccess { surveys ->
                mutableState.value = mutableState.value.copy(surveys = surveys)
            }
    }
}
