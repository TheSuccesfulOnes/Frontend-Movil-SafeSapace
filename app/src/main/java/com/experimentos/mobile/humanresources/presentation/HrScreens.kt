package com.experimentos.mobile.humanresources.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.experimentos.mobile.activity.data.ActivityApi
import com.experimentos.mobile.activity.data.ActivityResponse
import com.experimentos.mobile.comment.data.CommentApi
import com.experimentos.mobile.comment.data.CommentResponse
import com.experimentos.mobile.mood.data.Mood
import com.experimentos.mobile.mood.data.MoodApi
import com.experimentos.mobile.mood.data.MoodSummary
import com.experimentos.mobile.shared.designsystem.AdminCoral
import com.experimentos.mobile.shared.designsystem.AdminDark
import com.experimentos.mobile.shared.designsystem.AdminGold
import com.experimentos.mobile.shared.designsystem.AdminSage
import com.experimentos.mobile.shared.presentation.EmptyState
import com.experimentos.mobile.shared.presentation.ErrorState
import com.experimentos.mobile.shared.presentation.InlineError
import com.experimentos.mobile.shared.presentation.LoadingState
import com.experimentos.mobile.shared.presentation.BrandCircleIcon
import com.experimentos.mobile.shared.presentation.LocalAppStrings
import com.experimentos.mobile.shared.presentation.SafeSpaceTopBar
import com.experimentos.mobile.shared.presentation.SafeSpaceTopBarAction
import com.experimentos.mobile.shared.presentation.ScreenHeader
import com.experimentos.mobile.shared.presentation.WelcomeRow
import com.experimentos.mobile.survey.data.SurveyApi
import com.experimentos.mobile.survey.data.SurveyResponse
import com.experimentos.mobile.survey.data.SurveyType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private val spanishLocale = Locale.forLanguageTag("es-ES")

private data class MoodCategory(
    val mood: Mood,
    val label: String,
    val color: Color,
)

private val moodCategories = listOf(
    MoodCategory(Mood.VERY_GOOD, "Muy bien", AdminDark),
    MoodCategory(Mood.GOOD, "Bien", AdminSage),
    MoodCategory(Mood.BAD, "Mal", AdminGold),
    MoodCategory(Mood.VERY_BAD, "Muy mal", AdminCoral),
)

/** HR landing screen with an aggregate, privacy-conscious wellbeing summary. */
@Composable
fun HrHomeScreen(
    moodApi: MoodApi,
    username: String,
    displayName: String,
    modifier: Modifier = Modifier,
    sessionKey: String = "default",
) {
    val strings = LocalAppStrings.current
    val viewModel: HrHomeViewModel = viewModel(
        key = "hr-home-$sessionKey",
        factory = HrHomeViewModelFactory(moodApi),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val visibleName = displayName.ifBlank { username }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            SafeSpaceTopBar(
                title = "SafeSpace",
                leadingContent = { BrandCircleIcon() },
                action = {
                    SafeSpaceTopBarAction(
                        contentDescription = strings.t("Actualizar resumen"),
                        onClick = viewModel::load,
                        icon = Icons.Default.Refresh,
                    )
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = innerPadding.calculateTopPadding() + 20.dp,
                end = 16.dp,
                bottom = innerPadding.calculateBottomPadding() + 28.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                WelcomeRow(name = visibleName)
            }
            item {
                HrDashboardHeader(date = state.summary?.date)
            }
            item {
                when {
                    state.isLoading -> LoadingState()
                    state.errorMessage != null -> ErrorState(
                        message = state.errorMessage!!,
                        onRetry = viewModel::load,
                    )
                    state.summary != null -> HrMoodSummaryCard(summary = state.summary!!)
                }
            }
            if (state.summary != null) {
                item {
                    HrEmployeesCard(
                        activeEmployees = state.summary!!.activeEmployees,
                        responseRate = state.summary!!.responseRate,
                    )
                }
            }
        }
    }
}

@Composable
private fun HrDashboardHeader(date: String?) {
    val strings = LocalAppStrings.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(strings.t("Resumen de Bienestar"), style = MaterialTheme.typography.headlineMedium)
        Text(
            text = formatDashboardDate(date, strings),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun HrMoodSummaryCard(summary: MoodSummary) {
    val strings = LocalAppStrings.current
    val positiveCount = (summary.distribution[Mood.VERY_GOOD] ?: 0L) +
        (summary.distribution[Mood.GOOD] ?: 0L)
    val positivePercentage = calculatePercentage(positiveCount, summary.totalResponses)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(strings.t("Estado Emocional General"), style = MaterialTheme.typography.titleLarge)
            Text(
                text = strings.t("Distribución de respuestas de la plantilla"),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            MoodDonutChart(
                distribution = summary.distribution,
                total = summary.totalResponses,
                positivePercentage = positivePercentage,
            )
            moodCategories.forEach { category ->
                MoodLegendRow(
                    category = category,
                    percentage = calculatePercentage(
                        summary.distribution[category.mood] ?: 0L,
                        summary.totalResponses,
                    ),
                )
            }
        }
    }
}

@Composable
private fun MoodDonutChart(
    distribution: Map<Mood, Long>,
    total: Long,
    positivePercentage: Int,
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val veryGoodColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(152.dp)) {
            val diameter = minOf(size.width, size.height)
            val strokeWidth = 20.dp.toPx()
            val topLeft = Offset(
                x = (size.width - diameter) / 2,
                y = (size.height - diameter) / 2,
            )
            val chartSize = Size(diameter, diameter)
            val chartStyle = Stroke(width = strokeWidth)

            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = chartSize,
                style = chartStyle,
            )

            if (total > 0) {
                var startAngle = -90f
                moodCategories.forEach { category ->
                    val count = (distribution[category.mood] ?: 0L).coerceAtLeast(0L)
                    val sweepAngle = count.toFloat() / total.toFloat() * 360f
                    if (sweepAngle > 0f) {
                        drawArc(
                            color = if (category.mood == Mood.VERY_GOOD) veryGoodColor else category.color,
                            startAngle = startAngle + 1f,
                            sweepAngle = (sweepAngle - 2f).coerceAtLeast(0f),
                            useCenter = false,
                            topLeft = topLeft,
                            size = chartSize,
                            style = chartStyle,
                        )
                    }
                    startAngle += sweepAngle
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (total > 0) "$positivePercentage%" else "—",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = if (total > 0) "Positivo" else "Sin datos",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun MoodLegendRow(
    category: MoodCategory,
    percentage: Int,
) {
    val strings = LocalAppStrings.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(10.dp),
            shape = CircleShape,
            color = if (category.mood == Mood.VERY_GOOD) {
                MaterialTheme.colorScheme.primary
            } else {
                category.color
            },
        ) {}
        Text(
            text = strings.t(category.label),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = "$percentage%",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun HrEmployeesCard(
    activeEmployees: Long,
    responseRate: Int,
) {
    val strings = LocalAppStrings.current
    val safeActiveEmployees = activeEmployees.coerceAtLeast(0L)
    val safeResponseRate = responseRate.coerceIn(0, 100)
    val formattedEmployees = NumberFormat.getIntegerInstance(Locale.US).format(safeActiveEmployees)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primary,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.22f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(34.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = strings.t("Empleados activos"),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = strings.t("Total en el ecosistema"),
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = formattedEmployees,
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.20f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = strings.t("Tasa de respuesta"),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "$safeResponseRate%",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            LinearProgressIndicator(
                progress = { safeResponseRate / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.22f),
            )
        }
    }
}

private fun formatDashboardDate(rawDate: String?, strings: com.experimentos.mobile.shared.presentation.AppStrings): String {
    val date = rawDate
        ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        ?: LocalDate.now()
    val locale = if (strings.isEnglish) Locale.ENGLISH else spanishLocale
    val pattern = if (strings.isEnglish) "EEEE, d MMMM" else "EEEE, d 'de' MMMM"
    val formatter = DateTimeFormatter.ofPattern(pattern, locale)
    return formatter.format(date).replaceFirstChar { it.titlecase(locale) }
}

private fun calculatePercentage(part: Long, total: Long): Int {
    if (total <= 0L) return 0
    return (part.coerceAtLeast(0L).toDouble() / total.toDouble() * 100)
        .roundToInt()
        .coerceIn(0, 100)
}

private enum class ManagementSection {
    SURVEYS,
    ACTIVITIES,
}

/** HR content management screen for the survey and activity endpoints. */
@Composable
fun HrContentScreen(
    surveyApi: SurveyApi,
    activityApi: ActivityApi,
    commentApi: CommentApi,
    modifier: Modifier = Modifier,
    sessionKey: String = "default",
) {
    val strings = LocalAppStrings.current
    val viewModel: HrContentViewModel = viewModel(
        key = "hr-content-$sessionKey",
        factory = HrContentViewModelFactory(surveyApi, activityApi, commentApi),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedSection by rememberSaveable(sessionKey) {
        mutableStateOf(ManagementSection.SURVEYS)
    }
    var showSurveyDialog by remember { mutableStateOf(false) }
    var showActivityDialog by remember { mutableStateOf(false) }
    var surveyToClose by remember { mutableStateOf<SurveyResponse?>(null) }
    var activityToClose by remember { mutableStateOf<ActivityResponse?>(null) }

    LaunchedEffect(state.message) {
        if (state.message == null) return@LaunchedEffect
        showSurveyDialog = false
        showActivityDialog = false
        surveyToClose = null
        activityToClose = null
        delay(4_500)
        viewModel.clearFeedback()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            SafeSpaceTopBar(
                title = "Gestión",
                action = {
                    SafeSpaceTopBarAction(
                        contentDescription = strings.t("Actualizar contenido"),
                        onClick = viewModel::load,
                        icon = Icons.Default.Refresh,
                    )
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = innerPadding.calculateTopPadding() + 20.dp,
                end = 16.dp,
                bottom = innerPadding.calculateBottomPadding() + 28.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                ScreenHeader(
                    title = "Contenido del equipo",
                    subtitle = "Crea contenido y revisa el estado de tus encuestas y actividades.",
                )
            }
            item {
                ManagementSectionSwitcher(
                    selectedSection = selectedSection,
                    surveyCount = state.surveys.size,
                    activityCount = state.activities.size,
                    onSelect = {
                        selectedSection = it
                        viewModel.clearFeedback()
                    },
                )
            }
            item {
                CreateContentButton(
                    section = selectedSection,
                    enabled = !state.isSubmitting,
                    onClick = {
                        viewModel.clearFeedback()
                        if (selectedSection == ManagementSection.SURVEYS) {
                            showSurveyDialog = true
                        } else {
                            showActivityDialog = true
                        }
                    },
                )
            }
            state.message?.let { message ->
                item { FeedbackMessage(message) }
            }
            state.errorMessage?.let { error ->
                item { ErrorState(error, onRetry = viewModel::load) }
            }
            if (state.isLoading) {
                item { LoadingState() }
            } else if (selectedSection == ManagementSection.SURVEYS) {
                item {
                    ContentSectionHeading(
                        title = "Encuestas",
                        description = "Revisa respuestas, comentarios y estado de cada encuesta.",
                    )
                }
                if (state.surveys.isEmpty()) {
                    item { EmptyState("Aún no hay encuestas creadas.") }
                } else {
                    items(state.surveys, key = { "survey-${it.id}" }) { survey ->
                        SurveyManagementCard(
                            survey = survey,
                            comments = state.comments[survey.id].orEmpty(),
                            commentsErrorMessage = state.commentsErrorMessage,
                            commentsLoading = state.commentsLoadingId == survey.id,
                            isCommentsExpanded = state.expandedSurveyId == survey.id,
                            isSubmitting = state.isSubmitting,
                            onPublish = { viewModel.changeSurveyStatus(survey.id, publish = true) },
                            onClose = { surveyToClose = survey },
                            onToggleComments = { viewModel.toggleSurveyComments(survey.id) },
                        )
                    }
                }
            } else {
                item {
                    ContentSectionHeading(
                        title = "Actividades",
                        description = "Consulta resultados actuales o finales y cierra las votaciones abiertas.",
                    )
                }
                if (state.activities.isEmpty()) {
                    item { EmptyState("Aún no hay actividades creadas.") }
                } else {
                    items(state.activities, key = { "activity-${it.id}" }) { activity ->
                        ActivityManagementCard(
                            activity = activity,
                            isSubmitting = state.isSubmitting,
                            onClose = { activityToClose = activity },
                        )
                    }
                }
            }
        }
    }

    if (showSurveyDialog) {
        CreateSurveyDialog(
            isSubmitting = state.isSubmitting,
            errorMessage = state.errorMessage,
            onDismiss = {
                viewModel.clearFeedback()
                showSurveyDialog = false
            },
            onCreate = viewModel::createSurvey,
        )
    }
    if (showActivityDialog) {
        CreateActivityDialog(
            isSubmitting = state.isSubmitting,
            errorMessage = state.errorMessage,
            onDismiss = {
                viewModel.clearFeedback()
                showActivityDialog = false
            },
            onCreate = viewModel::createActivity,
        )
    }
    surveyToClose?.let { survey ->
        ConfirmCloseDialog(
            title = strings.t("Cerrar encuesta"),
            message = strings.t("Las personas ya no podrán enviar nuevas respuestas a esta encuesta."),
            isSubmitting = state.isSubmitting,
            onDismiss = { surveyToClose = null },
            onConfirm = { viewModel.changeSurveyStatus(survey.id, publish = false) },
        )
    }
    activityToClose?.let { activity ->
        ConfirmCloseDialog(
            title = strings.t("Cerrar actividad"),
            message = strings.t("La actividad dejará de estar disponible para votación."),
            isSubmitting = state.isSubmitting,
            onDismiss = { activityToClose = null },
            onConfirm = { viewModel.closeActivity(activity.id) },
        )
    }
}

@Composable
private fun ManagementSectionSwitcher(
    selectedSection: ManagementSection,
    surveyCount: Int,
    activityCount: Int,
    onSelect: (ManagementSection) -> Unit,
) {
    val strings = LocalAppStrings.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ManagementSectionButton(
            selected = selectedSection == ManagementSection.SURVEYS,
            label = "${strings.t("Encuestas")} ($surveyCount)",
            icon = Icons.Default.Poll,
            onClick = { onSelect(ManagementSection.SURVEYS) },
            modifier = Modifier.weight(1f),
        )
        ManagementSectionButton(
            selected = selectedSection == ManagementSection.ACTIVITIES,
            label = "${strings.t("Actividades")} ($activityCount)",
            icon = Icons.Default.CalendarMonth,
            onClick = { onSelect(ManagementSection.ACTIVITIES) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ManagementSectionButton(
    selected: Boolean,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val buttonModifier = modifier.height(48.dp)
    if (selected) {
        Button(
            onClick = onClick,
            modifier = buttonModifier,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(label, modifier = Modifier.padding(start = 6.dp))
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = buttonModifier,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(label, modifier = Modifier.padding(start = 6.dp))
        }
    }
}

@Composable
private fun CreateContentButton(
    section: ManagementSection,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val strings = LocalAppStrings.current
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
    ) {
        Icon(Icons.Default.Add, contentDescription = null)
        Text(
            text = strings.t(
                if (section == ManagementSection.SURVEYS) {
                    "Nueva encuesta"
                } else {
                    "Nueva actividad"
                },
            ),
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun ContentSectionHeading(
    title: String,
    description: String,
) {
    val strings = LocalAppStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(strings.t(title), style = MaterialTheme.typography.titleLarge)
        Text(
            text = strings.t(description),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun FeedbackMessage(message: String) {
    val strings = LocalAppStrings.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = strings.translateMessage(message),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun SurveyManagementCard(
    survey: SurveyResponse,
    comments: List<CommentResponse>,
    commentsErrorMessage: String?,
    commentsLoading: Boolean,
    isCommentsExpanded: Boolean,
    isSubmitting: Boolean,
    onPublish: () -> Unit,
    onClose: () -> Unit,
    onToggleComments: () -> Unit,
) {
    val strings = LocalAppStrings.current
    ManagementCard(survey.title) {
        Text(
            text = survey.question,
            style = MaterialTheme.typography.bodyLarge,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatusTag(
                text = strings.t(
                    if (survey.type == SurveyType.DAILY) "Diaria" else "Semanal",
                ),
            )
            StatusTag(text = strings.surveyStatus(survey.status))
        }
        Text(
            text = "${survey.answers} ${strings.t("respuestas")}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.ChatBubbleOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = strings.t(
                    if (survey.allowComments) {
                        "Comentarios habilitados"
                    } else {
                        "Solo respuestas"
                    },
                ),
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (survey.allowComments) {
            OutlinedButton(
                onClick = onToggleComments,
                modifier = Modifier.fillMaxWidth(),
                enabled = !commentsLoading,
            ) {
                Icon(
                    imageVector = if (isCommentsExpanded) {
                        Icons.Default.ExpandLess
                    } else {
                        Icons.Default.ExpandMore
                    },
                    contentDescription = null,
                )
                Text(
                    text = if (isCommentsExpanded) {
                        strings.t("Ocultar comentarios")
                    } else {
                        strings.t("Ver comentarios")
                    },
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
        if (isCommentsExpanded) {
            ManagedSurveyComments(
                comments = comments,
                errorMessage = commentsErrorMessage,
                isLoading = commentsLoading,
            )
        }
        when (survey.status) {
            "PUBLISHED" -> {
                Button(
                    onClick = onClose,
                    enabled = !isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(strings.t("Cerrar encuesta"))
                }
            }
            "DRAFT" -> {
                Button(
                    onClick = onPublish,
                    enabled = !isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(strings.t("Publicar encuesta"))
                }
            }
            else -> {
                Text(
                    text = strings.t("Esta encuesta está cerrada y se conserva solo como historial."),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun ManagedSurveyComments(
    comments: List<CommentResponse>,
    errorMessage: String?,
    isLoading: Boolean,
) {
    val strings = LocalAppStrings.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        HorizontalDivider()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = strings.t("Comentarios"),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "${comments.size} ${strings.t("comentarios")}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
            )
        }
        if (isLoading) {
            LoadingState(modifier = Modifier.padding(vertical = 8.dp))
        } else if (errorMessage != null) {
            InlineError(errorMessage)
        } else if (comments.isEmpty()) {
            Text(
                text = strings.t("Aún no hay comentarios."),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        } else {
            comments.forEach { comment ->
                ManagedComment(comment = comment)
            }
        }
    }
}

@Composable
private fun ManagedComment(
    comment: CommentResponse,
    depth: Int = 0,
) {
    val strings = LocalAppStrings.current
    val isReply = depth > 0
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (isReply) 14.dp else 0.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
            color = if (isReply) {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.58f)
            } else {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.52f)
            },
            border = BorderStroke(
                1.dp,
                if (isReply) {
                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.28f)
                } else {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                },
            ),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = if (isReply) {
                        strings.t("Respuesta anónima")
                    } else {
                        strings.t("Comentario anónimo")
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(comment.content, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "♥ ${comment.likes}",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
        comment.replies.forEach { reply ->
            ManagedComment(comment = reply, depth = depth + 1)
        }
    }
}

@Composable
private fun ActivityManagementCard(
    activity: ActivityResponse,
    isSubmitting: Boolean,
    onClose: () -> Unit,
) {
    val strings = LocalAppStrings.current
    ManagementCard(activity.title) {
        activity.description?.takeIf(String::isNotBlank)?.let { description ->
            Text(description, style = MaterialTheme.typography.bodyLarge)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatusTag(text = strings.activityStatus(activity.status))
            StatusTag(text = "${activity.options.size} ${strings.t("opciones")}")
        }
        ActivityResults(activity)
        if (activity.status == "OPEN") {
            Button(
                onClick = onClose,
                enabled = !isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(strings.t("Cerrar actividad"))
            }
        } else {
            Text(
                text = strings.t("Esta actividad está cerrada y se conserva solo como historial."),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun ActivityResults(activity: ActivityResponse) {
    val strings = LocalAppStrings.current
    val totalVotes = activity.options.sumOf { it.votes }
    val highestVoteCount = activity.options.maxOfOrNull { it.votes } ?: 0L
    val hasVotes = totalVotes > 0L
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        HorizontalDivider()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = strings.t(
                        if (activity.status == "CLOSED") {
                            "Resultados finales"
                        } else {
                            "Resultados actuales"
                        },
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "$totalVotes ${strings.t("votos")}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            if (activity.status == "CLOSED" && hasVotes) {
                StatusTag(text = strings.t("Resultado final"))
            }
        }
        if (!hasVotes) {
            Text(
                text = strings.t("Aún no hay votos."),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        } else {
            activity.options.forEach { option ->
                val percentage = if (totalVotes == 0L) {
                    0
                } else {
                    (option.votes.toDouble() / totalVotes * 100).roundToInt().coerceIn(0, 100)
                }
                val isWinner = option.votes == highestVoteCount
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = option.label,
                            modifier = Modifier.weight(1f),
                            fontWeight = if (isWinner) FontWeight.SemiBold else FontWeight.Normal,
                        )
                        Text(
                            text = "$percentage%",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    LinearProgressIndicator(
                        progress = { percentage / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = if (isWinner) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.secondary
                        },
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                    Text(
                            text = "${option.votes} ${strings.t("votos")}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall,
                        )
                        if (isWinner) {
                            Text(
                                text = strings.t("Más votada"),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ManagementCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            content()
        }
    }
}

@Composable
private fun StatusTag(text: String) {
    val strings = LocalAppStrings.current
    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            text = strings.t(text),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun CreateSurveyDialog(
    isSubmitting: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onCreate: (String, String, SurveyType, Boolean) -> Unit,
) {
    val strings = LocalAppStrings.current
    var title by remember { mutableStateOf("") }
    var question by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(SurveyType.DAILY) }
    var comments by remember { mutableStateOf(true) }
    val canCreate = title.isNotBlank() && question.isNotBlank() && !isSubmitting

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.t("Nueva encuesta")) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = strings.t("Crea una pregunta clara para que el equipo pueda responderla."),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                errorMessage?.let { InlineError(it) }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(120) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(strings.t("Título")) },
                    placeholder = { Text(strings.t("Ej. Encuesta del día")) },
                    singleLine = true,
                    supportingText = { Text(strings.t("Nombre visible para el equipo")) },
                )
                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it.take(500) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(strings.t("Pregunta")) },
                    placeholder = { Text(strings.t("¿Cómo fue tu jornada hoy?")) },
                    minLines = 2,
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                )
                Text(strings.t("Frecuencia"), style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == SurveyType.DAILY,
                        onClick = { type = SurveyType.DAILY },
                        label = { Text(strings.t("Diaria")) },
                    )
                    FilterChip(
                        selected = type == SurveyType.WEEKLY,
                        onClick = { type = SurveyType.WEEKLY },
                        label = { Text(strings.t("Semanal")) },
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(strings.t("Permitir comentarios"), style = MaterialTheme.typography.titleSmall)
                        Text(
                            strings.t("Las personas podrán añadir contexto a su respuesta."),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Switch(checked = comments, onCheckedChange = { comments = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(title, question, type, comments) },
                enabled = canCreate,
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(strings.t("Crear encuesta"))
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text(strings.t("Cancelar"))
            }
        },
    )
}

@Composable
private fun CreateActivityDialog(
    isSubmitting: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onCreate: (String, String, String) -> Unit,
) {
    val strings = LocalAppStrings.current
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var options by remember { mutableStateOf("") }
    val optionCount = options.lines().count(String::isNotBlank)
    val canCreate = title.isNotBlank() && optionCount >= 2 && !isSubmitting

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.t("Nueva actividad")) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = strings.t("Crea una votación con al menos dos alternativas."),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                errorMessage?.let { InlineError(it) }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(120) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(strings.t("Título")) },
                    placeholder = { Text(strings.t("Ej. Actividad del fin de semana")) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it.take(500) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(strings.t("Descripción (opcional)")) },
                    placeholder = { Text(strings.t("Añade contexto para la votación")) },
                    minLines = 2,
                    maxLines = 4,
                )
                OutlinedTextField(
                    value = options,
                    onValueChange = { options = it.take(1000) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(strings.t("Opciones")) },
                    placeholder = { Text(strings.t("Una opción por línea")) },
                    supportingText = { Text("$optionCount ${strings.t("opciones")} · ${strings.t("mínimo 2")}") },
                    minLines = 3,
                    maxLines = 6,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(title, description, options) },
                enabled = canCreate,
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(strings.t("Crear actividad"))
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text(strings.t("Cancelar"))
            }
        },
    )
}

@Composable
private fun ConfirmCloseDialog(
    title: String,
    message: String,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val strings = LocalAppStrings.current
    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = { Text(strings.t(title)) },
        text = { Text(strings.t(message)) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Text(strings.t("Confirmar"))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text(strings.t("Mantener abierta"))
            }
        },
    )
}
