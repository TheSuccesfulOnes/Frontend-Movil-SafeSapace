package com.experimentos.mobile.shared.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 3.dp,
        )
    }
}

@Composable
fun ErrorState(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    val strings = LocalAppStrings.current
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(strings.translateMessage(message), color = MaterialTheme.colorScheme.onErrorContainer)
            onRetry?.let {
                OutlinedButton(onClick = it) {
                    Text(strings.t("Reintentar"))
                }
            }
        }
    }
}

@Composable
fun EmptyState(message: String, modifier: Modifier = Modifier) {
    val strings = LocalAppStrings.current
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Text(
            text = strings.translateMessage(message),
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Displays transient success feedback without permanently occupying screen space. */
@Composable
fun SafeSpaceSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier,
    ) { snackbarData ->
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.82f),
                    ),
                    shape = MaterialTheme.shapes.medium,
                ),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 44.dp)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = snackbarData.visuals.message,
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val strings = LocalAppStrings.current
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(strings.t(title), style = MaterialTheme.typography.titleLarge)
            content()
        }
    }
}

fun Throwable.toUserMessage(defaultMessage: String): String = when (this) {
    is java.io.IOException -> "No se pudo conectar con el servidor."
    is retrofit2.HttpException -> when (code()) {
        400, 409 -> duplicateFieldMessage() ?: if (code() == 400) {
            "Revisa los datos ingresados."
        } else {
            "La operación entra en conflicto con un registro existente."
        }
        401 -> "Tu sesión ya no es válida. Inicia sesión nuevamente."
        403 -> "No tienes permisos para realizar esta acción."
        404 -> "No se encontró la información solicitada."
        503 -> "El asistente no está disponible en este momento. Intenta nuevamente."
        else -> defaultMessage
    }
    else -> defaultMessage
}

/** Maps known API validation messages to safe, actionable text for the user. */
private fun retrofit2.HttpException.duplicateFieldMessage(): String? {
    val body = response()?.errorBody()?.string().orEmpty().lowercase()
    val apiMessage = Regex("\\\"message\\\"\\s*:\\s*\\\"([^\\\"]+)").find(body)?.groupValues?.getOrNull(1)
        ?: body
    return when {
        apiMessage.contains("username") -> "El nombre de usuario ya está utilizado."
        apiMessage.contains("email") -> "El correo electrónico ya está utilizado."
        apiMessage.contains("already been answered") -> "Ya respondiste esta encuesta."
        apiMessage.contains("survey is not open") -> "Esta encuesta ya no está disponible."
        else -> null
    }
}
