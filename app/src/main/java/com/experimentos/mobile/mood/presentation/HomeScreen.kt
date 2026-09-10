package com.experimentos.mobile.mood.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.SentimentVeryDissatisfied
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.experimentos.mobile.mood.data.Mood
import com.experimentos.mobile.mood.data.MoodApi
import com.experimentos.mobile.shared.designsystem.AdminBlue
import com.experimentos.mobile.shared.designsystem.AdminDark
import com.experimentos.mobile.shared.designsystem.AdminSage
import com.experimentos.mobile.shared.presentation.BrandCircleIcon
import com.experimentos.mobile.shared.presentation.ErrorState
import com.experimentos.mobile.shared.presentation.LocalAppStrings
import com.experimentos.mobile.shared.presentation.SafeSpaceTopBar
import com.experimentos.mobile.shared.presentation.WelcomeRow

@Composable
fun HomeScreen(
    username: String,
    displayName: String,
    moodApi: MoodApi,
    modifier: Modifier = Modifier,
) {
    val strings = LocalAppStrings.current
    val homeViewModel: HomeViewModel = viewModel(
        key = "home-$username",
        factory = HomeViewModelFactory(moodApi),
    )
    val state by homeViewModel.state.collectAsStateWithLifecycle()
    val visibleName = displayName.ifBlank { username }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SafeSpaceTopBar(
                title = "SafeSpace",
                leadingContent = { BrandCircleIcon() },
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 18.dp, end = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                WelcomeRow(name = visibleName)
            }
            item {
                if (state.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(48.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp,
                        )
                    }
                } else {
                    MoodCard(state = state, onMoodSelected = homeViewModel::submitMood)
                }
            }
            item {
                Text(
                    text = strings.t("Próximamente"),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            items(comingSoonFeatures()) { feature ->
                ComingSoonCard(feature)
            }
        }
    }
}

@Composable
private fun MoodCard(state: HomeUiState, onMoodSelected: (Mood) -> Unit) {
    val strings = LocalAppStrings.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = strings.t("¿Cómo te sientes hoy?"),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Top,
            ) {
                moodOptions().forEach { option ->
                    MoodOption(
                        option = option,
                        selected = state.selectedMood == option.mood,
                        enabled = state.selectedMood == null && !state.isSubmitting,
                        onClick = { onMoodSelected(option.mood) },
                    )
                }
            }
            state.selectedMood?.let { selectedMood ->
                Text(
                    text = "${strings.t("Registrado:")} ${strings.moodLabel(selectedMood)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = strings.t("Podrás responder nuevamente mañana."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            state.errorMessage?.let { ErrorState(message = it) }
        }
    }
}

@Composable
private fun MoodOption(
    option: MoodOption,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val strings = LocalAppStrings.current
    Column(
        modifier = Modifier.width(58.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.size(48.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = option.color,
                contentColor = AdminDark,
                disabledContainerColor = option.color,
                disabledContentColor = AdminDark,
            ),
        ) {
            Icon(
                imageVector = option.icon,
                contentDescription = strings.t(option.label),
                modifier = Modifier.size(24.dp),
            )
        }
        Text(
            text = strings.t(option.label),
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (selected) {
            Surface(
                modifier = Modifier.size(5.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
            ) {}
        }
    }
}

@Composable
private fun ComingSoonCard(feature: ComingSoonFeature) {
    val strings = LocalAppStrings.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(12.dp),
                color = feature.color,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = feature.icon,
                        contentDescription = null,
                        tint = AdminDark,
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(strings.t(feature.title), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = strings.t(feature.description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private data class MoodOption(
    val mood: Mood,
    val label: String,
    val icon: ImageVector,
    val color: Color,
)

private fun moodOptions() = listOf(
    MoodOption(Mood.VERY_BAD, "Muy mal", Icons.Default.SentimentVeryDissatisfied, Color(0xFFD86B73)),
    MoodOption(Mood.BAD, "Mal", Icons.Default.SentimentDissatisfied, Color(0xFFF3C58E)),
    MoodOption(Mood.GOOD, "Bien", Icons.Default.SentimentSatisfied, AdminSage),
    MoodOption(Mood.VERY_GOOD, "Muy bien", Icons.Default.SentimentVerySatisfied, Color(0xFF78A9DC)),
)

private data class ComingSoonFeature(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
)

private fun comingSoonFeatures() = listOf(
    ComingSoonFeature(
        title = "Métricas de Bienestar",
        description = "Visualiza tu progreso a lo largo del tiempo con gráficos detallados.",
        icon = Icons.AutoMirrored.Filled.ShowChart,
        color = AdminBlue,
    ),
    ComingSoonFeature(
        title = "Recursos Guiados",
        description = "Ejercicios de respiración y mindfulness para tu día a día.",
        icon = Icons.Default.SelfImprovement,
        color = AdminSage,
    ),
)
