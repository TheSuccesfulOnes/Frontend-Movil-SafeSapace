package com.experimentos.mobile.shared.presentation

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Shared top bar for authenticated mobile screens.
 *
 * Keeping the top bar in one place prevents role-specific screens from drifting
 * away from the SafeSpace visual system.
 */
@Composable
fun SafeSpaceTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: ImageVector? = null,
    navigationContentDescription: String = "Volver",
    onNavigationClick: (() -> Unit)? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
) {
    val strings = LocalAppStrings.current
    Surface(
        modifier = modifier.fillMaxWidth(),
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
            if (navigationIcon != null && onNavigationClick != null) {
                IconButton(onClick = onNavigationClick) {
                    Icon(
                        imageVector = navigationIcon,
                        contentDescription = strings.t(navigationContentDescription),
                    )
                }
            }
            leadingContent?.let {
                it()
                Spacer(modifier = Modifier.width(10.dp))
            }
            Text(
                text = strings.t(title),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
            )
            action?.invoke()
        }
    }
}

/**
 * Icon action with a consistent accessibility contract for top bars.
 */
@Composable
fun SafeSpaceTopBarAction(
    contentDescription: String,
    onClick: () -> Unit,
    icon: ImageVector,
) {
    val strings = LocalAppStrings.current
    IconButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = strings.t(contentDescription),
        )
    }
}
