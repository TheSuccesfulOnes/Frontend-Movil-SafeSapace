package com.experimentos.mobile.survey.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.experimentos.mobile.activity.data.ActivityApi
import com.experimentos.mobile.activity.presentation.ActivityCard
import com.experimentos.mobile.activity.presentation.ActivityViewModel
import com.experimentos.mobile.activity.presentation.ActivityViewModelFactory
import com.experimentos.mobile.comment.data.CommentApi
import com.experimentos.mobile.comment.data.CommentResponse
import com.experimentos.mobile.shared.presentation.EmptyState
import com.experimentos.mobile.shared.presentation.ErrorState
import com.experimentos.mobile.shared.presentation.LoadingState
import com.experimentos.mobile.shared.presentation.LocalAppStrings
import com.experimentos.mobile.shared.presentation.SafeSpaceTopBar
import com.experimentos.mobile.shared.presentation.SafeSpaceTopBarAction
import com.experimentos.mobile.shared.presentation.SectionCard
import com.experimentos.mobile.shared.presentation.SoftTag
import com.experimentos.mobile.survey.data.SurveyApi
import com.experimentos.mobile.survey.data.SurveyResponse
import com.experimentos.mobile.survey.data.SurveyType

private const val HUB_DESTINATION = "hub"
private const val DAILY_DESTINATION = "daily"
private const val WEEKLY_DESTINATION = "weekly"

/**
 * Survey area entry point for employees.
 *
 * The hub keeps the two workflows discoverable while the detail destinations
 * prevent long mixed lists and make the back action predictable.
 */
@Composable
fun SurveysScreen(
    surveyApi: SurveyApi,
    commentApi: CommentApi,
    activityApi: ActivityApi,
    onOpenReport: () -> Unit,
    modifier: Modifier = Modifier,
    sessionKey: String = "default",
) {
    val strings = LocalAppStrings.current
    val surveyViewModel: SurveyViewModel = viewModel(
        key = "surveys-$sessionKey",
        factory = SurveyViewModelFactory(surveyApi, commentApi),
    )
    val surveyState by surveyViewModel.state.collectAsStateWithLifecycle()
    val activityViewModel: ActivityViewModel = viewModel(
        key = "activities-$sessionKey",
        factory = ActivityViewModelFactory(activityApi),
    )
    val activityState by activityViewModel.state.collectAsStateWithLifecycle()
    var destination by rememberSaveable(sessionKey) { mutableStateOf(HUB_DESTINATION) }
    val refreshContent = {
        surveyViewModel.load()
        activityViewModel.load()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (destination == HUB_DESTINATION) {
                SafeSpaceTopBar(
                    title = "Encuestas",
                    action = {
                        SafeSpaceTopBarAction(
                            contentDescription = strings.t("Actualizar contenido"),
                            onClick = refreshContent,
                            icon = Icons.Default.Refresh,
                        )
                    },
                )
            } else {
                SafeSpaceTopBar(
                    title = if (destination == DAILY_DESTINATION) {
                        strings.t("Encuesta Diaria")
                    } else {
                        strings.t("Actividad Semanal")
                    },
                    navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                    navigationContentDescription = "Regresar al Centro de Encuestas",
                    onNavigationClick = { destination = HUB_DESTINATION },
                    action = {
                        SafeSpaceTopBarAction(
                            contentDescription = strings.t("Actualizar contenido"),
                            onClick = refreshContent,
                            icon = Icons.Default.Refresh,
                        )
                    },
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        when (destination) {
            DAILY_DESTINATION -> DailySurveysContent(
                state = surveyState,
                onAnswer = surveyViewModel::answer,
                onToggleComments = surveyViewModel::toggleComments,
                onComment = surveyViewModel::createComment,
                onLike = surveyViewModel::likeComment,
                onDelete = surveyViewModel::deleteComment,
                contentPadding = innerPadding,
            )

            WEEKLY_DESTINATION -> WeeklyActivitiesContent(
                state = activityState,
                onLoad = activityViewModel::load,
                onChoose = activityViewModel::choose,
                onVote = activityViewModel::vote,
                contentPadding = innerPadding,
            )

            else -> SurveyHubContent(
                surveyState = surveyState,
                activityState = activityState,
                onOpenDaily = { destination = DAILY_DESTINATION },
                onOpenWeekly = { destination = WEEKLY_DESTINATION },
                onOpenReport = onOpenReport,
                contentPadding = innerPadding,
            )
        }
    }
}

@Composable
private fun SurveyHubContent(
    surveyState: SurveyUiState,
    activityState: com.experimentos.mobile.activity.presentation.ActivityUiState,
    onOpenDaily: () -> Unit,
    onOpenWeekly: () -> Unit,
    onOpenReport: () -> Unit,
    contentPadding: PaddingValues,
) {
    val strings = LocalAppStrings.current
    val dailySurveys = surveyState.surveys.filter { it.type == SurveyType.DAILY }
    val dailyStatus = when {
        surveyState.isLoading -> "Cargando"
        dailySurveys.isEmpty() -> "Sin encuestas"
        dailySurveys.all { it.answered || surveyState.answeredSurveyIds.contains(it.id) } -> "Respondidas"
        else -> "Pendiente"
    }
    val weeklyStatus = when {
        activityState.isLoading -> "Cargando"
        activityState.activities.isEmpty() -> "Sin actividades"
        else -> "Actividad abierta"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(start = 16.dp, top = 20.dp, end = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(strings.t("Centro de Encuestas"), style = MaterialTheme.typography.headlineMedium)
                Text(
                    strings.t("Tu opinión nos ayuda a construir un ambiente laboral saludable. Comparte tu experiencia y ayuda a mejorar el bienestar del equipo."),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        surveyState.errorMessage?.let { error ->
            item { ErrorState(error) }
        }
        activityState.errorMessage?.let { error ->
            item { ErrorState(error) }
        }
        item {
            SurveyCategoryCard(
                icon = Icons.Default.Poll,
                iconColor = MaterialTheme.colorScheme.primaryContainer,
                status = dailyStatus,
                title = "Encuestas diarias",
                description = "Breve chequeo de tu estado de ánimo y niveles de energía hoy. Toma menos de un minuto.",
                onOpen = onOpenDaily,
            )
        }
        item {
            SurveyCategoryCard(
                icon = Icons.Default.Event,
                iconColor = MaterialTheme.colorScheme.secondaryContainer,
                status = weeklyStatus,
                title = "Actividades semanales",
                description = "Participa en votaciones y elige las actividades que más te interesen.",
                onOpen = onOpenWeekly,
            )
        }
        item {
            SectionCard("Reportes") {
                Text(
                    strings.t("Comunica una situación de forma identificada o anónima."),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = onOpenReport, modifier = Modifier.fillMaxWidth()) {
                    Text(strings.t("Crear reporte"))
                }
            }
        }
    }
}

@Composable
private fun SurveyCategoryCard(
    icon: ImageVector,
    iconColor: Color,
    status: String,
    title: String,
    description: String,
    onOpen: () -> Unit,
) {
    val strings = LocalAppStrings.current
    Card(
        onClick = onOpen,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(13.dp),
                    color = iconColor,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(10.dp),
                    )
                }
                SurveyStatusTag(status)
            }
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(strings.t(title), style = MaterialTheme.typography.titleLarge)
                Text(
                    strings.t(description),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun DailySurveysContent(
    state: SurveyUiState,
    onAnswer: (Long, String) -> Unit,
    onToggleComments: (Long) -> Unit,
    onComment: (Long, String, Long?) -> Unit,
    onLike: (Long, Long) -> Unit,
    onDelete: (Long, Long) -> Unit,
    contentPadding: PaddingValues,
) {
    val strings = LocalAppStrings.current
    val dailySurveys = state.surveys.filter { it.type == SurveyType.DAILY }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text(
                strings.t("Responde las preguntas de hoy y comparte cómo fue tu jornada."),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        state.message?.let { message ->
            item { Text(strings.translateMessage(message), color = MaterialTheme.colorScheme.primary) }
        }
        state.errorMessage?.let { error ->
            item { ErrorState(error) }
        }
        if (state.isLoading) {
            item { LoadingState() }
        } else if (dailySurveys.isEmpty()) {
            item { EmptyState("Todavía no hay encuestas publicadas.") }
        } else {
            items(dailySurveys, key = { it.id }) { survey ->
                SurveyCard(
                    survey = survey,
                    state = state,
                    onAnswer = onAnswer,
                    onToggleComments = onToggleComments,
                    onComment = onComment,
                    onLike = onLike,
                    onDelete = onDelete,
                )
            }
        }
    }
}

@Composable
private fun WeeklyActivitiesContent(
    state: com.experimentos.mobile.activity.presentation.ActivityUiState,
    onLoad: () -> Unit,
    onChoose: (Long, Long) -> Unit,
    onVote: (Long) -> Unit,
    contentPadding: PaddingValues,
) {
    val strings = LocalAppStrings.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(strings.t("Actividad del fin de semana"), style = MaterialTheme.typography.headlineMedium)
                Text(
                    strings.t("Vota por la actividad que prefieres para nuestro próximo encuentro de equipo."),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        state.message?.let { message ->
            item { Text(strings.translateMessage(message), color = MaterialTheme.colorScheme.primary) }
        }
        state.errorMessage?.let { error ->
            item { ErrorState(error, onRetry = onLoad) }
        }
        if (state.isLoading) {
            item { LoadingState() }
        } else if (state.activities.isEmpty()) {
            item { EmptyState("No hay actividades abiertas por ahora.") }
        } else {
            items(state.activities, key = { "activity-${it.id}" }) { activity ->
                ActivityCard(
                    activity = activity,
                    selectedOptionId = state.selectedOptions[activity.id],
                    pendingOptionId = state.pendingOptions[activity.id],
                    isSubmitting = state.isSubmitting,
                    onSelect = onChoose,
                    onVote = { onVote(activity.id) },
                )
            }
        }
    }
}

@Composable
private fun SurveyStatusTag(status: String) {
    val strings = LocalAppStrings.current
    val isPositive = status == "Respondidas" || status == "Actividad abierta"
    Surface(
        shape = RoundedCornerShape(50),
        color = if (isPositive) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        },
    ) {
        Text(
            text = strings.t(status),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            color = if (isPositive) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSecondaryContainer
            },
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun SurveyCard(
    survey: SurveyResponse,
    state: SurveyUiState,
    onAnswer: (Long, String) -> Unit,
    onToggleComments: (Long) -> Unit,
    onComment: (Long, String, Long?) -> Unit,
    onLike: (Long, Long) -> Unit,
    onDelete: (Long, Long) -> Unit,
) {
    val strings = LocalAppStrings.current
    var answerText by rememberSaveable(survey.id) { mutableStateOf("") }
    val hasAnswered = survey.answered || state.answeredSurveyIds.contains(survey.id)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SurveyStatusTag(if (hasAnswered) "Respondida" else "Pendiente")
                Text(
                    text = "${survey.answers} ${strings.t("respuestas")}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Text(survey.title, style = MaterialTheme.typography.titleLarge)
            Text(survey.question, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (hasAnswered) {
                SoftTag("Respuesta registrada")
            } else {
                OutlinedTextField(
                    value = answerText,
                    onValueChange = { answerText = it.take(1000) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(strings.t("Tu respuesta")) },
                    placeholder = { Text(strings.t("Escribe tu respuesta...")) },
                    supportingText = { Text("${answerText.length}/1000") },
                    minLines = 2,
                )
                Button(
                    onClick = { onAnswer(survey.id, answerText) },
                    enabled = answerText.isNotBlank() && !state.isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(strings.t("Enviar respuesta"))
                }
            }
            if (survey.allowComments) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        strings.t("Comentarios anónimos"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelLarge,
                    )
                    OutlinedButton(onClick = { onToggleComments(survey.id) }) {
                        Text(strings.t(if (state.expandedSurveyId == survey.id) "Ocultar" else "Ver comentarios"))
                    }
                }
                if (state.expandedSurveyId == survey.id) {
                    HorizontalDivider()
                    state.comments[survey.id].orEmpty().forEach { comment ->
                        CommentItem(
                            surveyId = survey.id,
                            comment = comment,
                            isSubmitting = state.isSubmitting,
                            onLike = onLike,
                            onReply = onComment,
                            onDelete = onDelete,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentItem(
    surveyId: Long,
    comment: CommentResponse,
    isSubmitting: Boolean,
    onLike: (Long, Long) -> Unit,
    onReply: (Long, String, Long?) -> Unit,
    onDelete: (Long, Long) -> Unit,
) {
    val strings = LocalAppStrings.current
    var replyText by rememberSaveable(comment.id) { mutableStateOf("") }
    var replying by rememberSaveable(comment.id) { mutableStateOf(false) }
    var repliesExpanded by rememberSaveable(comment.id) { mutableStateOf(false) }
    var showDeleteConfirmation by rememberSaveable(comment.id) { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.38f),
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomEnd = 18.dp,
                bottomStart = 6.dp,
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(comment.content)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CommentActionButton(
                        icon = Icons.Default.FavoriteBorder,
                        label = comment.likes.toString(),
                        tint = MaterialTheme.colorScheme.secondary,
                        onClick = { onLike(surveyId, comment.id) },
                        enabled = !isSubmitting,
                    )
                    CommentActionButton(
                        icon = Icons.AutoMirrored.Filled.Reply,
                        label = strings.t(if (replying) "Cerrar" else "Responder"),
                        tint = MaterialTheme.colorScheme.primary,
                        onClick = { replying = !replying },
                        enabled = !isSubmitting,
                    )
                    if (comment.canDelete) {
                        DeleteCommentButton(
                            onClick = { showDeleteConfirmation = true },
                            enabled = !isSubmitting,
                        )
                    }
                }
                if (comment.replies.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { repliesExpanded = !repliesExpanded },
                        enabled = !isSubmitting,
                        modifier = Modifier.height(34.dp),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Icon(
                            imageVector = if (repliesExpanded) {
                                Icons.Default.ExpandLess
                            } else {
                                Icons.Default.ExpandMore
                            },
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = if (repliesExpanded) {
                                strings.t("Cerrar respuestas")
                            } else {
                                strings.replyCountLabel(comment.replies.size)
                            },
                        )
                    }
                }
            }
        }
        if (replying) {
            OutlinedTextField(
                value = replyText,
                onValueChange = { replyText = it.take(1000) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp),
                label = { Text(strings.t("Respuesta anónima")) },
                placeholder = { Text(strings.t("Escribe una respuesta...")) },
                supportingText = { Text("${replyText.length}/1000") },
            )
            Button(
                onClick = {
            onReply(surveyId, replyText, comment.id)
            replyText = ""
            replying = false
        },
                enabled = replyText.isNotBlank() && !isSubmitting,
                modifier = Modifier.padding(start = 16.dp),
            ) {
                Text(strings.t("Publicar respuesta"))
            }
        }
        if (repliesExpanded && comment.replies.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Surface(
                    modifier = Modifier
                        .width(2.dp)
                        .heightIn(min = 42.dp),
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(50),
                ) {}
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    comment.replies.forEach { reply ->
                        ReplyBubble(
                            surveyId = surveyId,
                            reply = reply,
                            isSubmitting = isSubmitting,
                            onDelete = onDelete,
                        )
                    }
                }
            }
        }
    }
    if (showDeleteConfirmation) {
        DeleteCommentDialog(
            isSubmitting = isSubmitting,
            onDismiss = { showDeleteConfirmation = false },
            onConfirm = {
                showDeleteConfirmation = false
                onDelete(surveyId, comment.id)
            },
        )
    }
}

@Composable
private fun CommentActionButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.height(34.dp),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            contentColor = tint,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
        Text(text = label, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DeleteCommentButton(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val strings = LocalAppStrings.current
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(34.dp),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f),
            contentColor = MaterialTheme.colorScheme.error,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
    ) {
        Icon(
            imageVector = Icons.Default.DeleteOutline,
            contentDescription = strings.t("Eliminar"),
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun ReplyBubble(
    surveyId: Long,
    reply: CommentResponse,
    isSubmitting: Boolean,
    onDelete: (Long, Long) -> Unit,
) {
    val strings = LocalAppStrings.current
    var showDeleteConfirmation by rememberSaveable(reply.id) { mutableStateOf(false) }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
        shape = RoundedCornerShape(
            topStart = 6.dp,
            topEnd = 16.dp,
            bottomEnd = 16.dp,
            bottomStart = 16.dp,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = strings.t("Respuesta"),
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                if (reply.canDelete) {
                    DeleteCommentButton(
                        onClick = { showDeleteConfirmation = true },
                        enabled = !isSubmitting,
                    )
                }
            }
            Text(
                text = reply.content,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
    if (showDeleteConfirmation) {
        DeleteCommentDialog(
            isSubmitting = isSubmitting,
            onDismiss = { showDeleteConfirmation = false },
            onConfirm = {
                showDeleteConfirmation = false
                onDelete(surveyId, reply.id)
            },
        )
    }
}

@Composable
private fun DeleteCommentDialog(
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val strings = LocalAppStrings.current
    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = { Text(strings.t("Eliminar comentario")) },
        text = { Text(strings.t("Esta acción no se puede deshacer.")) },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isSubmitting) {
                Text(strings.t("Eliminar"), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text(strings.t("Cancelar"))
            }
        },
    )
}
