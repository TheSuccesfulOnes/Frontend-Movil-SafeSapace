package com.experimentos.mobile.ai.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.experimentos.mobile.ai.data.AiApi
import com.experimentos.mobile.ai.data.ConversationResponse
import com.experimentos.mobile.ai.data.MessageResponse
import com.experimentos.mobile.ai.data.SendMessageRequest
import com.experimentos.mobile.ai.data.UpdateConversationRequest
import com.experimentos.mobile.shared.presentation.toUserMessage
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AiUiState(
    val isLoading: Boolean = true,
    val conversations: List<ConversationResponse> = emptyList(),
    val selectedConversationId: Long? = null,
    val messages: List<MessageResponse> = emptyList(),
    val isSubmitting: Boolean = false,
    val isSending: Boolean = false,
    val errorMessage: String? = null,
)

class AiViewModel(private val aiApi: AiApi) : ViewModel() {
    private val mutableState = MutableStateFlow(AiUiState())
    val state: StateFlow<AiUiState> = mutableState.asStateFlow()

    fun loadConversations() {
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(isLoading = true, errorMessage = null)
            runCatching { aiApi.getConversations() }
                .onSuccess { conversations ->
                    mutableState.value = mutableState.value.copy(
                        isLoading = false,
                        conversations = conversations,
                        selectedConversationId = null,
                        isSending = false,
                    )
                }
                .onFailure { error ->
                    mutableState.value = mutableState.value.copy(
                        isLoading = false,
                        isSending = false,
                        errorMessage = error.toUserMessage("No se pudieron cargar tus conversaciones."),
                    )
                }
        }
    }

    fun createConversation() {
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(
                isSubmitting = true,
                isSending = false,
                errorMessage = null,
            )
            runCatching { aiApi.createConversation() }
                .onSuccess { conversation ->
                    mutableState.value = mutableState.value.copy(
                        isSubmitting = false,
                        isSending = false,
                        conversations = listOf(conversation) + mutableState.value.conversations,
                        selectedConversationId = conversation.id,
                        messages = emptyList(),
                    )
                }
                .onFailure { error ->
                    mutableState.value = mutableState.value.copy(
                        isSubmitting = false,
                        isSending = false,
                        errorMessage = error.toUserMessage("No se pudo crear la conversación."),
                    )
                }
        }
    }

    fun selectConversation(id: Long) {
        if (mutableState.value.isSubmitting) return

        mutableState.value = mutableState.value.copy(
            selectedConversationId = id,
            messages = emptyList(),
            errorMessage = null,
        )
        loadMessages(id)
    }

    fun clearSelection() {
        if (mutableState.value.isSubmitting) return

        mutableState.value = mutableState.value.copy(
            selectedConversationId = null,
            messages = emptyList(),
            errorMessage = null,
        )
    }

    fun renameConversation(id: Long, title: String) {
        val normalizedTitle = title.trim()
        if (normalizedTitle.isBlank()) return

        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(
                isSubmitting = true,
                isSending = false,
                errorMessage = null,
            )
            runCatching {
                aiApi.renameConversation(id, UpdateConversationRequest(normalizedTitle))
            }.onSuccess { updatedConversation ->
                mutableState.value = mutableState.value.copy(
                    isSubmitting = false,
                    isSending = false,
                    conversations = mutableState.value.conversations.map { conversation ->
                        if (conversation.id == updatedConversation.id) updatedConversation else conversation
                    },
                )
            }.onFailure { error ->
                mutableState.value = mutableState.value.copy(
                    isSubmitting = false,
                    isSending = false,
                    errorMessage = error.toUserMessage("No se pudo cambiar el nombre."),
                )
            }
        }
    }

    fun deleteConversation(id: Long) {
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(
                isSubmitting = true,
                isSending = false,
                errorMessage = null,
            )
            runCatching { aiApi.deleteConversation(id) }
                .onSuccess {
                    val currentState = mutableState.value
                    val isSelected = currentState.selectedConversationId == id
                    mutableState.value = currentState.copy(
                        isSubmitting = false,
                        isSending = false,
                        conversations = currentState.conversations.filterNot { it.id == id },
                        selectedConversationId = if (isSelected) null else currentState.selectedConversationId,
                        messages = if (isSelected) emptyList() else currentState.messages,
                    )
                }
                .onFailure { error ->
                    mutableState.value = mutableState.value.copy(
                        isSubmitting = false,
                        isSending = false,
                        errorMessage = error.toUserMessage("No se pudo eliminar la conversación."),
                    )
                }
        }
    }

    fun sendMessage(content: String, language: String) {
        val conversationId = mutableState.value.selectedConversationId
        if (conversationId == null) {
            mutableState.value = mutableState.value.copy(
                errorMessage = "Crea o selecciona una conversación primero.",
            )
            return
        }

        val normalizedContent = content.trim()
        if (normalizedContent.isBlank() || mutableState.value.isSubmitting) return

        val optimisticMessage = MessageResponse(
            id = -System.currentTimeMillis(),
            sender = "USER",
            content = normalizedContent,
            createdAt = Instant.now().toString(),
        )
        mutableState.value = mutableState.value.copy(
            isSubmitting = true,
            isSending = true,
            errorMessage = null,
            messages = mutableState.value.messages + optimisticMessage,
        )

        viewModelScope.launch {
            runCatching {
                aiApi.sendMessage(
                    conversationId,
                    SendMessageRequest(normalizedContent, language),
                )
            }
                .onSuccess { responseMessages ->
                    val currentState = mutableState.value
                    if (currentState.selectedConversationId == conversationId) {
                        mutableState.value = currentState.copy(
                            isSubmitting = false,
                            isSending = false,
                            messages = currentState.messages
                                .filterNot { it.id == optimisticMessage.id } + responseMessages,
                        )
                    }
                }
                .onFailure { error ->
                    val currentState = mutableState.value
                    if (currentState.selectedConversationId == conversationId) {
                        mutableState.value = currentState.copy(
                            isSubmitting = false,
                            isSending = false,
                            messages = currentState.messages
                                .filterNot { it.id == optimisticMessage.id },
                            errorMessage = error.toUserMessage("No se pudo enviar el mensaje."),
                        )
                    }
                }
        }
    }

    private fun loadMessages(id: Long) {
        viewModelScope.launch {
            runCatching { aiApi.getMessages(id) }
                .onSuccess { messages ->
                    if (mutableState.value.selectedConversationId == id) {
                        mutableState.value = mutableState.value.copy(messages = messages)
                    }
                }
                .onFailure { error ->
                    if (mutableState.value.selectedConversationId == id) {
                        mutableState.value = mutableState.value.copy(
                            errorMessage = error.toUserMessage("No se pudo cargar el historial."),
                        )
                    }
                }
        }
    }
}
