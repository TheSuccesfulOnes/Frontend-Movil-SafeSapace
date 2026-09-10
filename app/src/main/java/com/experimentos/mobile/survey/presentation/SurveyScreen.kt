package com.experimentos.mobile.survey.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (destination == HUB_DESTINATION) {
                SafeSpaceTopBar(
                    title = "Encuestas",
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
                actionLabel = "Comenzar ahora",
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
                actionLabel = "Ver actividades",
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
    actionLabel: String,
    onOpen: () -> Unit,
) {
    val strings = LocalAppStrings.current
    Card(
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
            TextButton(onClick = onOpen) {
                Text("${strings.t(actionLabel)}  →")
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
) {
    val strings = LocalAppStrings.current
    var answerText by rememberSaveable(survey.id) { mutableStateOf("") }
    var commentText by rememberSaveable(survey.id) { mutableStateOf("") }
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
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it.take(1000) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(strings.t("Comentario anónimo")) },
                        placeholder = { Text(strings.t("Comparte tu experiencia...")) },
                        supportingText = { Text("${commentText.length}/1000") },
                        minLines = 2,
                    )
                    Button(
                        onClick = {
                            onComment(survey.id, commentText, null)
                            commentText = ""
                        },
                        enabled = commentText.isNotBlank() && !state.isSubmitting,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(strings.t("Publicar comentario"))
                    }
                    state.comments[survey.id].orEmpty().forEach { comment ->
                        CommentItem(
                            surveyId = survey.id,
                            comment = comment,
                            isSubmitting = state.isSubmitting,
                            onLike = onLike,
                            onReply = onComment,
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
) {
    val strings = LocalAppStrings.current
    var replyText by rememberSaveable(comment.id) { mutableStateOf("") }
    var replying by rememberSaveable(comment.id) { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.52f),
            shape = RoundedCornerShape(14.dp),
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(comment.content)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = { onLike(surveyId, comment.id) },
                        enabled = !isSubmitting,
                    ) {
                        Text("♡ ${comment.likes}")
                    }
                    TextButton(
                        onClick = { replying = !replying },
                        enabled = !isSubmitting,
                    ) {
                        Text(strings.t(if (replying) "Cerrar" else "Responder"))
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
        comment.replies.forEach { reply ->
            Surface(
                modifier = Modifier.padding(start = 16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    text = reply.content,
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
