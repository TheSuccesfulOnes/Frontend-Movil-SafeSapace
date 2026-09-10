package com.experimentos.mobile.shared.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.experimentos.mobile.R
import com.experimentos.mobile.shared.designsystem.AdminCoral
import com.experimentos.mobile.shared.designsystem.AdminDark

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    action: (@Composable () -> Unit)? = null,
) {
    val strings = LocalAppStrings.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let {
            Surface(
                modifier = Modifier.size(46.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(it, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(strings.t(title), style = MaterialTheme.typography.headlineMedium)
            Text(strings.t(subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        action?.invoke()
    }
}

/** Displays the signed-in user's compact welcome row on landing screens. */
@Composable
fun WelcomeRow(name: String) {
    val strings = LocalAppStrings.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        InitialAvatar(name = name, size = 40.dp)
        Text(
            text = if (strings.isEnglish) "Welcome, $name" else "Bienvenido, $name",
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

@Composable
fun BrandEmblem(
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
) {
    val strings = LocalAppStrings.current
    Surface(
        modifier = modifier.size(size),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 5.dp,
    ) {
        Image(
            painter = painterResource(id = R.drawable.safespace_logo),
            contentDescription = strings.t("SafeSpace"),
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            contentScale = ContentScale.Fit,
        )
    }
}

/**
 * Compact circular brand mark for authenticated top bars.
 */
@Composable
fun BrandCircleIcon(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
) {
    val strings = LocalAppStrings.current
    Surface(
        modifier = modifier.size(size),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
    ) {
        Image(
            painter = painterResource(id = R.drawable.safespace_logo),
            contentDescription = strings.t("Logo de SafeSpace"),
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp),
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
fun InitialAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
) {
    Surface(
        modifier = modifier.size(size),
        shape = CircleShape,
        color = AdminCoral,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = name.trim().take(1).uppercase().ifBlank { "U" },
                color = AdminDark,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun SoftTag(text: String, modifier: Modifier = Modifier) {
    val strings = LocalAppStrings.current
    Surface(
        modifier = modifier,
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
fun InlineError(message: String, modifier: Modifier = Modifier) {
    val strings = LocalAppStrings.current
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Text(
            text = strings.translateMessage(message),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
