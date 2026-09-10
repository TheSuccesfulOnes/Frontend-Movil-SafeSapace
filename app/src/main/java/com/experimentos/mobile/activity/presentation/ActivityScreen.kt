package com.experimentos.mobile.activity.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.experimentos.mobile.activity.data.ActivityApi
import com.experimentos.mobile.activity.data.ActivityResponse
import com.experimentos.mobile.shared.presentation.EmptyState
import com.experimentos.mobile.shared.presentation.ErrorState
import com.experimentos.mobile.shared.presentation.LoadingState
import com.experimentos.mobile.shared.presentation.LocalAppStrings
import com.experimentos.mobile.shared.presentation.ScreenHeader
import com.experimentos.mobile.shared.presentation.SoftTag

@Composable
fun ActivitiesScreen(activityApi: ActivityApi, modifier: Modifier = Modifier) {
    val strings = LocalAppStrings.current
    val activityViewModel: ActivityViewModel = viewModel(factory = ActivityViewModelFactory(activityApi))
    val state by activityViewModel.state.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            ScreenHeader(
                title = "Actividades semanales",
                subtitle = "Elige una opción y consulta cómo avanza la votación.",
                icon = Icons.Default.CalendarMonth,
            )
        }
        state.message?.let {
            item {
                Text(
                    text = strings.translateMessage(it),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        state.errorMessage?.let { item { ErrorState(it, onRetry = activityViewModel::load) } }
        if (state.isLoading) {
            item { LoadingState() }
        } else if (state.activities.isEmpty()) {
            item { EmptyState("No hay actividades abiertas por ahora.") }
        } else {
            items(state.activities, key = { it.id }) { activity ->
                ActivityCard(
                    activity = activity,
                    selectedOptionId = state.selectedOptions[activity.id],
                    pendingOptionId = state.pendingOptions[activity.id],
                    isSubmitting = state.isSubmitting,
                    onSelect = activityViewModel::choose,
                    onVote = { activityViewModel.vote(activity.id) },
                )
            }
        }
    }
}

@Composable
fun ActivityCard(
    activity: ActivityResponse,
    selectedOptionId: Long?,
    pendingOptionId: Long? = null,
    isSubmitting: Boolean,
    onSelect: (Long, Long) -> Unit,
    onVote: () -> Unit = {},
) {
    val strings = LocalAppStrings.current
    val displayedOptionId = pendingOptionId ?: selectedOptionId
    val canVote = displayedOptionId != null && displayedOptionId != selectedOptionId

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SoftTag("VOTACIÓN ABIERTA")
            Text(activity.title, style = MaterialTheme.typography.titleLarge)
            activity.description?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                activity.options.forEach { option ->
                    ActivityOptionRow(
                        option = option,
                        selected = displayedOptionId == option.id,
                        enabled = !isSubmitting,
                        onClick = { onSelect(activity.id, option.id) },
                    )
                }
            }
            Button(
                onClick = onVote,
                enabled = canVote && !isSubmitting,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(strings.t(if (selectedOptionId == null) "Votar" else "Cambiar voto"))
            }
        }
    }
}

@Composable
private fun ActivityOptionRow(
    option: com.experimentos.mobile.activity.data.ActivityOption,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val strings = LocalAppStrings.current
    val percentage = (option.percentage / 100.0).coerceIn(0.0, 1.0).toFloat()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            ),
    ) {
        Box(
                modifier = Modifier
                    .fillMaxWidth(percentage)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)),
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                RadioButton(selected = selected, onClick = null)
                Column {
                    Text(strings.t(option.label), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "${option.votes} ${strings.t("votos")}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            Text(
                text = "${"%.0f".format(option.percentage)}%",
                modifier = Modifier.padding(end = 12.dp),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
