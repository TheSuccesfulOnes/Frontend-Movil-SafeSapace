package com.experimentos.mobile.ai.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.experimentos.mobile.ai.data.AiApi
import com.experimentos.mobile.ai.data.ConversationResponse
import com.experimentos.mobile.ai.data.MessageResponse
import com.experimentos.mobile.shared.presentation.AppStrings
import com.experimentos.mobile.shared.presentation.EmptyState
import com.experimentos.mobile.shared.presentation.ErrorState
import com.experimentos.mobile.shared.presentation.LocalAppStrings
import com.experimentos.mobile.shared.presentation.LoadingState

@Composable
fun AiScreen(
    aiApi: AiApi,
    modifier: Modifier = Modifier,
    sessionKey: String = "default",
) {
    val strings = LocalAppStrings.current
    val aiViewModel: AiViewModel = viewModel(
        key = "ai-$sessionKey",
        factory = AiViewModelFactory(aiApi),
    )
    val state by aiViewModel.state.collectAsStateWithLifecycle()
    var message by remember { mutableStateOf("") }
    var conversationToRename by remember { mutableStateOf<ConversationResponse?>(null) }
    var conversationToDelete by remember { mutableStateOf<ConversationResponse?>(null) }

    LaunchedEffect(Unit) {
        aiViewModel.clearSelection()
        aiViewModel.loadConversations()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "Chat AI",
                        modifier = Modifier.padding(start = 10.dp),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = aiViewModel::createConversation,
                        enabled = !state.isSubmitting,
                        modifier = Modifier.height(40.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                    ) {
                        Text(strings.t("+ Nueva"))
                    }
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp)
                .fillMaxSize(),
        ) {
            state.errorMessage?.let {
                ErrorState(message = it, modifier = Modifier.padding(bottom = 8.dp))
            }
            when {
                state.isLoading -> LoadingState(Modifier.fillMaxSize())
                state.selectedConversationId == null -> ConversationHistory(
                    conversations = state.conversations,
                    onOpen = aiViewModel::selectConversation,
                    onRename = { conversationToRename = it },
                    onDelete = { conversationToDelete = it },
                )
                else -> ChatConversation(
                    conversation = state.conversations.firstOrNull {
                        it.id == state.selectedConversationId
                    },
                    messages = state.messages,
                    message = message,
                    enabled = !state.isSubmitting,
                    isSending = state.isSending,
                    onBackToHistory = aiViewModel::clearSelection,
                    onMessageChange = { message = it },
                    onSend = {
                        aiViewModel.sendMessage(
                            content = message,
                            language = if (strings.isEnglish) "en" else "es",
                        )
                        message = ""
                    },
                )
            }
        }
    }

    conversationToRename?.let { conversation ->
        RenameConversationDialog(
            conversation = conversation,
            onDismiss = { conversationToRename = null },
            onConfirm = { title ->
                aiViewModel.renameConversation(conversation.id, title)
                conversationToRename = null
            },
        )
    }
    conversationToDelete?.let { conversation ->
        DeleteConversationDialog(
            conversation = conversation,
            onDismiss = { conversationToDelete = null },
            onConfirm = {
                aiViewModel.deleteConversation(conversation.id)
                conversationToDelete = null
            },
        )
    }
}

@Composable
private fun ConversationHistory(
    conversations: List<ConversationResponse>,
    onOpen: (Long) -> Unit,
    onRename: (ConversationResponse) -> Unit,
    onDelete: (ConversationResponse) -> Unit,
) {
    val strings = LocalAppStrings.current
    if (conversations.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = strings.t("Aún no hay conversaciones"),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(strings.t("Historial de conversaciones"), style = MaterialTheme.typography.headlineMedium)
        Text(
            strings.t("Selecciona una conversación para continuar."),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(conversations, key = { it.id }) { conversation ->
                ConversationHistoryItem(
                    conversation = conversation,
                    onOpen = { onOpen(conversation.id) },
                    onRename = { onRename(conversation) },
                    onDelete = { onDelete(conversation) },
                )
            }
        }
    }
}

@Composable
private fun ConversationHistoryItem(
    conversation: ConversationResponse,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val strings = LocalAppStrings.current
    var menuExpanded by remember(conversation.id) { mutableStateOf(false) }

    Card(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    conversation.localizedDisplayTitle(strings),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    strings.t("Toca para abrir el chat"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = strings.t("Opciones de conversación"))
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(strings.t("Editar nombre")) },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onRename()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(strings.t("Eliminar conversación")) },
                        leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatConversation(
    conversation: ConversationResponse?,
    messages: List<MessageResponse>,
    message: String,
    enabled: Boolean,
    isSending: Boolean,
    onBackToHistory: () -> Unit,
    onMessageChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    val strings = LocalAppStrings.current
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, isSending) {
        val targetIndex = if (isSending) messages.size else messages.lastIndex
        if (targetIndex >= 0) {
            listState.animateScrollToItem(targetIndex)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(
            onClick = onBackToHistory,
            modifier = Modifier.height(40.dp),
            contentPadding = PaddingValues(horizontal = 14.dp),
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            Spacer(modifier = Modifier.size(6.dp))
                Text(strings.t("Regresar"))
        }
        conversation?.let {
            Text(
                text = it.localizedDisplayTitle(strings),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
        Text(
            text = strings.t("Hoy"),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(messages, key = { it.id }) { item -> MessageBubble(item) }
            if (messages.isEmpty() && !isSending) {
                item { EmptyState("Escribe algo cuando quieras comenzar la conversación.") }
            }
            if (isSending) {
                item(key = "assistant-typing") { TypingIndicatorBubble() }
            }
        }
        MessageComposer(
            message = message,
            enabled = enabled,
            onMessageChange = onMessageChange,
            onSend = onSend,
        )
        Text(
            text = strings.t("Orientación general; no sustituye la atención profesional."),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TypingIndicatorBubble() {
    val strings = LocalAppStrings.current
    val transition = rememberInfiniteTransition(label = "typing-indicator")
    val density = LocalDensity.current
    val dotOffsets = (0..2).map { index ->
        val delayMillis = index * 150
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 900
                    0f at 0
                    0f at delayMillis
                    -4f at delayMillis + 150
                    0f at delayMillis + 300
                    0f at 900
                },
                repeatMode = RepeatMode.Restart,
            ),
            label = "typing-dot-$index",
        )
    }

    Card(
        modifier = Modifier
            .widthIn(max = 88.dp)
            .semantics { contentDescription = strings.t("El asistente está escribiendo") },
        shape = RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            dotOffsets.forEach { offset ->
                Text(
                    text = ".",
                    modifier = Modifier.offset {
                        IntOffset(
                            x = 0,
                            y = with(density) { offset.value.dp.roundToPx() },
                        )
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

@Composable
private fun RenameConversationDialog(
    conversation: ConversationResponse,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val strings = LocalAppStrings.current
    var title by remember(conversation.id) {
        mutableStateOf(conversation.localizedDisplayTitle(strings))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.t("Editar nombre")) },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(strings.t("Nombre de la conversación")) },
                singleLine = true,
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(title) }, enabled = title.isNotBlank()) {
                Text(strings.t("Guardar"))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.t("Cancelar")) } },
    )
}

@Composable
private fun DeleteConversationDialog(
    conversation: ConversationResponse,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val strings = LocalAppStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.t("Eliminar conversación")) },
        text = {
            Text(
                if (strings.isEnglish) {
                    "Do you want to permanently delete “${conversation.localizedDisplayTitle(strings)}”? This action cannot be undone."
                } else {
                    "¿Quieres eliminar definitivamente “${conversation.localizedDisplayTitle(strings)}”? Esta acción no se puede deshacer."
                },
            )
        },
        confirmButton = {
            Button(onClick = onConfirm) { Text(strings.t("Eliminar")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.t("Cancelar")) } },
    )
}

@Composable
private fun MessageBubble(message: MessageResponse) {
    val strings = LocalAppStrings.current
    val isUserMessage = message.sender == "USER"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUserMessage) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {
        if (!isUserMessage) {
            Surface(
                modifier = Modifier.padding(end = 6.dp).size(26.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Spa,
                        contentDescription = strings.t("Asistente"),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }
        Card(
            modifier = Modifier.widthIn(max = 330.dp),
            shape = if (isUserMessage) {
                RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)
            } else {
                RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)
            },
            colors = CardDefaults.cardColors(
                containerColor = if (isUserMessage) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                },
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Text(
                text = message.content,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                color = if (isUserMessage) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

private fun ConversationResponse.localizedDisplayTitle(strings: AppStrings): String =
    if (title.isBlank() || title == "New conversation") {
        strings.t("Nueva conversación")
    } else {
        title
    }

@Composable
private fun MessageComposer(
    message: String,
    enabled: Boolean,
    onMessageChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    val strings = LocalAppStrings.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        OutlinedTextField(
            value = message,
            onValueChange = onMessageChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text(strings.t("Escribe tu mensaje...")) },
            shape = RoundedCornerShape(26.dp),
            maxLines = 3,
            enabled = enabled,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
            ),
        )
        FilledIconButton(
            onClick = onSend,
            enabled = enabled && message.isNotBlank(),
            modifier = Modifier.size(52.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Send,
                contentDescription = strings.t("Enviar mensaje"),
            )
        }
    }
}
