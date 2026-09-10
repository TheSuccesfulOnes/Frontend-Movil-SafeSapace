package com.experimentos.mobile.report.presentation

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.experimentos.mobile.report.data.CreateReportRequest
import com.experimentos.mobile.report.data.ReportApi
import com.experimentos.mobile.report.data.ReportResponse
import com.experimentos.mobile.shared.designsystem.AdminBlue
import com.experimentos.mobile.shared.designsystem.AdminCoral
import com.experimentos.mobile.shared.designsystem.AdminDark
import com.experimentos.mobile.shared.designsystem.AdminGold
import com.experimentos.mobile.shared.designsystem.AdminSage
import com.experimentos.mobile.shared.presentation.EmptyState
import com.experimentos.mobile.shared.presentation.ErrorState
import com.experimentos.mobile.shared.presentation.LoadingState
import com.experimentos.mobile.shared.presentation.InlineError
import com.experimentos.mobile.shared.presentation.LocalAppStrings
import com.experimentos.mobile.shared.presentation.ScreenHeader
import com.experimentos.mobile.shared.presentation.SafeSpaceTopBar
import com.experimentos.mobile.shared.presentation.SafeSpaceTopBarAction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Refresh
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateReportScreen(
    reportApi: ReportApi,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    sessionKey: String = "default",
) {
    val strings = LocalAppStrings.current
    val reportViewModel: ReportViewModel = viewModel(
        key = "create-report-$sessionKey",
        factory = ReportViewModelFactory(reportApi),
    )
    val state by reportViewModel.state.collectAsStateWithLifecycle()
    var category by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("") }
    var anonymous by remember { mutableStateOf(true) }
    var workplaceMenuExpanded by remember { mutableStateOf(false) }
    val workplaceOptions = listOf(
        "TI",
        "Atención al cliente",
        "Finanzas",
        "Recursos humanos",
        "Operaciones",
        "Marketing",
    )
    var showValidation by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    fun submitReport() {
        val error = when {
            category.isBlank() -> "Selecciona tu puesto de trabajo."
            title.isBlank() -> "Escribe un título para identificar el reporte."
            description.isBlank() -> "Describe brevemente lo que ocurrió."
            priority.isBlank() -> "Selecciona el nivel de prioridad."
            else -> null
        }

        if (error != null) {
            showValidation = true
            validationError = error
            return
        }

        showValidation = false
        validationError = null
        reportViewModel.create(
            CreateReportRequest(
                category = category.trim(),
                title = title.trim(),
                description = description.trim(),
                priority = priority,
                anonymous = anonymous,
            ),
            onSuccess = onBack,
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = onBack,
                        enabled = !state.isSubmitting,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = strings.t("Volver"),
                        )
                    }
                    Text(strings.t("Reportes"), style = MaterialTheme.typography.titleMedium)
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column {
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(
                        text = strings.t("Información del reporte"),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = strings.t("Los campos marcados son necesarios para poder revisar tu caso."),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )

                    ExposedDropdownMenuBox(
                        expanded = workplaceMenuExpanded,
                        onExpandedChange = { workplaceMenuExpanded = !workplaceMenuExpanded },
                    ) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = {},
                            modifier = Modifier
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth(),
                            label = { Text(strings.t("Puesto de trabajo")) },
                            placeholder = { Text(strings.t("Selecciona tu área")) },
                            leadingIcon = {
                                Icon(Icons.Default.Category, contentDescription = null)
                            },
                            supportingText = {
                                Text(strings.t("Selecciona el área a la que perteneces"))
                            },
                            isError = showValidation && category.isBlank(),
                            readOnly = true,
                            singleLine = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = workplaceMenuExpanded,
                                )
                            },
                        )
                        DropdownMenu(
                            expanded = workplaceMenuExpanded,
                            onDismissRequest = { workplaceMenuExpanded = false },
                        ) {
                            workplaceOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(strings.t(option)) },
                                    onClick = {
                                        category = option
                                        workplaceMenuExpanded = false
                                        showValidation = false
                                        validationError = null
                                    },
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it.take(160)
                            showValidation = false
                            validationError = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(strings.t("Título")) },
                        placeholder = { Text(strings.t("Resume el caso en pocas palabras")) },
                        leadingIcon = {
                            Icon(Icons.Default.Flag, contentDescription = null)
                        },
                        supportingText = {
                            Text("${title.length}/160")
                        },
                        isError = showValidation && title.isBlank(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = {
                            description = it.take(2000)
                            showValidation = false
                            validationError = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(strings.t("Descripción")) },
                        placeholder = { Text(strings.t("Cuéntanos qué ocurrió y cómo podemos ayudarte...")) },
                        leadingIcon = {
                            Icon(Icons.Default.Description, contentDescription = null)
                        },
                        supportingText = {
                            Text("${description.length}/2000")
                        },
                        isError = showValidation && description.isBlank(),
                        minLines = 4,
                        maxLines = 6,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                    )

                    Text(
                        text = strings.t("Nivel de prioridad"),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = strings.t("Selecciona qué tan urgente consideras este reporte."),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            PriorityChip(
                                label = "Baja",
                                selected = priority == "LOW",
                                onClick = { priority = "LOW" },
                                modifier = Modifier.weight(1f),
                            )
                            PriorityChip(
                                label = "Normal",
                                selected = priority == "NORMAL",
                                onClick = { priority = "NORMAL" },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            PriorityChip(
                                label = "Alta",
                                selected = priority == "HIGH",
                                onClick = { priority = "HIGH" },
                                modifier = Modifier.weight(1f),
                            )
                            PriorityChip(
                                label = "Urgente",
                                selected = priority == "URGENT",
                                onClick = { priority = "URGENT" },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                text = strings.t("Enviar de forma anónima"),
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Text(
                                    text = if (anonymous) {
                                        strings.t("Tu identidad no se mostrará al equipo de RR. HH.")
                                    } else {
                                        strings.t("Tu nombre podrá ser visible para el equipo de RR. HH.")
                                    },
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            Switch(
                                checked = anonymous,
                                onCheckedChange = { anonymous = it },
                            )
                        }
                    }

                    validationError?.let { InlineError(it) }
                    state.errorMessage?.let { InlineError(it) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = strings.t("Usa este espacio para comunicar una situación real de forma respetuosa."),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Button(
                        onClick = ::submitReport,
                        enabled = !state.isSubmitting &&
                            category.isNotBlank() &&
                            title.isNotBlank() &&
                            description.isNotBlank() &&
                            priority.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                    ) {
                        if (state.isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(strings.t("Enviar reporte"))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PriorityChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalAppStrings.current
    FilterChip(
        modifier = modifier,
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = strings.t(label),
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HrReportsScreen(
    reportApi: ReportApi,
    modifier: Modifier = Modifier,
    sessionKey: String = "default",
) {
    val strings = LocalAppStrings.current
    val reportViewModel: ReportViewModel = viewModel(
        key = "hr-reports-$sessionKey",
        factory = ReportViewModelFactory(reportApi, loadAll = true),
    )
    val state by reportViewModel.state.collectAsStateWithLifecycle()
    var selectedFilter by remember { mutableStateOf(ReportFilter.ALL) }
    var selectedReport by remember { mutableStateOf<ReportResponse?>(null) }
    val now = remember { Instant.now() }
    val visibleReports = remember(state.reports, selectedFilter) {
        state.reports.filter { report -> selectedFilter.matches(report.status) }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            SafeSpaceTopBar(
                title = "Reportes",
                action = {
                    SafeSpaceTopBarAction(
                        contentDescription = strings.t("Actualizar reportes"),
                        onClick = reportViewModel::loadReports,
                        icon = Icons.Default.Refresh,
                    )
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp,
                top = innerPadding.calculateTopPadding() + 20.dp,
                end = 16.dp,
                bottom = innerPadding.calculateBottomPadding() + 28.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = strings.t("Gestión de Reportes"),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = strings.t("Bandeja de entrada segura y confidencial."),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ReportFilterChip(
                        filter = ReportFilter.ALL,
                        selected = selectedFilter == ReportFilter.ALL,
                        onClick = { selectedFilter = ReportFilter.ALL },
                    )
                    ReportFilterChip(
                        filter = ReportFilter.IN_REVIEW,
                        selected = selectedFilter == ReportFilter.IN_REVIEW,
                        onClick = { selectedFilter = ReportFilter.IN_REVIEW },
                    )
                    ReportFilterChip(
                        filter = ReportFilter.ADDRESSED,
                        selected = selectedFilter == ReportFilter.ADDRESSED,
                        onClick = { selectedFilter = ReportFilter.ADDRESSED },
                    )
                }
            }
            state.message?.let { message ->
                item { FeedbackMessage(message) }
            }
            state.errorMessage?.let { error ->
                item { ErrorState(error, onRetry = reportViewModel::loadReports) }
            }
            when {
                state.isLoading -> item { LoadingState() }
                visibleReports.isEmpty() -> item {
                    EmptyState(
                        if (state.reports.isEmpty()) {
                            "Aún no hay reportes registrados."
                        } else {
                            "No hay reportes en este estado."
                        },
                    )
                }
                else -> items(visibleReports, key = { it.id }) { report ->
                    HrReportCard(
                        report = report,
                        isSubmitting = state.isSubmitting,
                        now = now,
                        onOpenDetails = { selectedReport = report },
                        onStatusChanged = reportViewModel::updateStatus,
                    )
                }
            }
        }
    }

    selectedReport?.let { report ->
        ReportDetailSheet(
            report = report,
            onDismiss = { selectedReport = null },
        )
    }
}

private enum class ReportFilter(
    val label: String,
) {
    ALL("Todos"),
    IN_REVIEW("En revisión"),
    ADDRESSED("Atendido"),
}

private fun ReportFilter.matches(status: String): Boolean = when (this) {
    ReportFilter.ALL -> true
    ReportFilter.IN_REVIEW -> status == "IN_REVIEW"
    ReportFilter.ADDRESSED -> status == "ADDRESSED" || status == "CLOSED"
}

@Composable
private fun ReportFilterChip(
    filter: ReportFilter,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val strings = LocalAppStrings.current
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Text(
            text = strings.t(filter.label),
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 9.dp),
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HrReportCard(
    report: ReportResponse,
    isSubmitting: Boolean,
    now: Instant,
    onOpenDetails: () -> Unit,
    onStatusChanged: (Long, String) -> Unit,
) {
    val strings = LocalAppStrings.current
    var expanded by remember(report.id) { mutableStateOf(false) }
    val priorityStyle = report.priority.toReportPriorityStyle()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (report.status == "NEW") 1.5.dp else 1.dp,
            color = if (report.status == "NEW") {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
        ),
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        modifier = Modifier.size(8.dp),
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = priorityStyle.dotColor,
                    ) {}
                    Text(
                        text = report.referenceCode(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                ReportPriorityBadge(priorityStyle)
            }
            Text(
                text = report.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (report.anonymous) Icons.Default.VisibilityOff else Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = if (report.anonymous) strings.t("Anónimo") else {
                        report.reporterDisplayName?.takeIf(String::isNotBlank) ?: strings.t("Identificado")
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = strings.reportStatus(report.status),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = report.createdAt.toRelativeReportTime(now, strings),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Box {
                    OutlinedButton(
                        onClick = { expanded = !expanded },
                        enabled = !isSubmitting,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary,
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    ) {
                        Text(strings.t("Cambiar estado"))
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        listOf("NEW", "IN_REVIEW", "ADDRESSED", "CLOSED").forEach { status ->
                            DropdownMenuItem(
                                text = { Text(strings.reportStatus(status)) },
                                onClick = {
                                    expanded = false
                                    if (!isSubmitting) onStatusChanged(report.id, status)
                                },
                            )
                        }
                    }
                }
            }
            OutlinedButton(
                onClick = onOpenDetails,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 10.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(strings.t("Ver detalle"), modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportDetailSheet(
    report: ReportResponse,
    onDismiss: () -> Unit,
) {
    val strings = LocalAppStrings.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, top = 4.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = strings.t("Detalle del reporte"),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = report.referenceCode(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = strings.t("Cerrar detalle"),
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReportPriorityBadge(report.priority.toReportPriorityStyle())
                ReportStatusBadge(report.status)
            }
            ReportDetailField("Título", report.title)
            ReportDetailField("Categoría o puesto", strings.t(report.category))
            ReportDetailField("Descripción", report.description)
            ReportDetailField(
                label = "Identidad del reporte",
                value = if (report.anonymous) {
                    strings.t("Anónimo")
                } else {
                    report.reporterDisplayName?.takeIf(String::isNotBlank) ?: strings.t("Identificado")
                },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 11.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
            ) {
                Text(strings.t("Cerrar detalle"))
            }
        }
    }
}

@Composable
private fun ReportDetailField(
    label: String,
    value: String,
) {
    val strings = LocalAppStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(
            text = strings.t(label),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Text(
                text = value,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun ReportStatusBadge(status: String) {
    val strings = LocalAppStrings.current
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Text(
            text = strings.reportStatus(status),
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
        )
    }
}

private data class ReportPriorityStyle(
    val label: String,
    val dotColor: Color,
    val containerColor: Color,
    val contentColor: Color,
)

@Composable
private fun ReportPriorityBadge(style: ReportPriorityStyle) {
    val strings = LocalAppStrings.current
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = style.containerColor,
    ) {
        Text(
            text = strings.t(style.label),
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            color = style.contentColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
        )
    }
}

private fun String.toReportPriorityStyle(): ReportPriorityStyle = when (this) {
    "URGENT" -> ReportPriorityStyle("Crítica", AdminCoral, AdminCoral.copy(alpha = 0.28f), AdminDark)
    "HIGH" -> ReportPriorityStyle("Alta", AdminBlue, AdminBlue.copy(alpha = 0.85f), AdminDark)
    "NORMAL" -> ReportPriorityStyle("Media", AdminSage, Color(0xFFE8D9FA), AdminDark)
    "LOW" -> ReportPriorityStyle("Baja", AdminSage, AdminSage.copy(alpha = 0.45f), AdminDark)
    else -> ReportPriorityStyle(this, AdminGold, AdminGold.copy(alpha = 0.35f), AdminDark)
}

private fun ReportResponse.referenceCode(): String =
    "REP-${id.toString().padStart(3, '0')}"

private fun String?.toRelativeReportTime(now: Instant, strings: com.experimentos.mobile.shared.presentation.AppStrings): String {
    if (isNullOrBlank()) return strings.t("Fecha no disponible")
    val createdAt = runCatching { Instant.parse(this) }.getOrNull()
        ?: return strings.t("Fecha no disponible")
    val elapsedSeconds = Duration.between(createdAt, now).seconds.coerceAtLeast(0)
    val elapsedMinutes = elapsedSeconds / 60
    val elapsedHours = elapsedMinutes / 60
    val elapsedDays = elapsedHours / 24

    if (strings.isEnglish) {
        return when {
            elapsedMinutes < 1 -> strings.t("Ahora")
            elapsedMinutes < 60 -> "$elapsedMinutes ${if (elapsedMinutes == 1L) "minute" else "minutes"} ago"
            elapsedHours < 24 -> "$elapsedHours ${if (elapsedHours == 1L) "hour" else "hours"} ago"
            elapsedDays == 1L -> strings.t("Ayer")
            elapsedDays < 7 -> "$elapsedDays days ago"
            else -> createdAt.atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH))
        }
    }

    return when {
        elapsedMinutes < 1 -> "Ahora"
        elapsedMinutes < 60 -> "Hace $elapsedMinutes ${if (elapsedMinutes == 1L) "minuto" else "minutos"}"
        elapsedHours < 24 -> "Hace $elapsedHours ${if (elapsedHours == 1L) "hora" else "horas"}"
        elapsedDays == 1L -> "Ayer"
        elapsedDays < 7 -> "Hace $elapsedDays días"
        else -> createdAt.atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("es-PE")))
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
        Text(
            text = strings.translateMessage(message),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private fun String.toDisplayStatus(): String = when (this) {
    "NEW" -> "Nuevo"
    "IN_REVIEW" -> "En revisión"
    "ADDRESSED" -> "Atendido"
    "CLOSED" -> "Cerrado"
    else -> this
}
