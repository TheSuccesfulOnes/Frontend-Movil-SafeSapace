package com.experimentos.mobile.profile.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.experimentos.mobile.profile.data.ProfileResponse
import com.experimentos.mobile.shared.presentation.InlineError
import com.experimentos.mobile.shared.presentation.LocalAppStrings

enum class ProfileField {
    DISPLAY_NAME,
    USERNAME,
    EMAIL,
}

@Composable
fun ProfileSettingsScreen(
    profile: ProfileResponse?,
    isSaving: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onEditDisplayName: () -> Unit,
    onEditUsername: () -> Unit,
    onEditEmail: () -> Unit,
    onUpdatePreferences: (String, String) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalAppStrings.current
    var showLanguageDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = strings.t("Volver"),
                        )
                    }
                    Text(strings.t("Configuración"), style = MaterialTheme.typography.titleMedium)
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                SettingsSection(title = strings.t("CUENTA")) {
                    SettingsRow(
                        icon = Icons.Default.Person,
                        title = strings.t("Cambiar nombre"),
                        detail = profile?.displayName.orEmpty(),
                        onClick = onEditDisplayName,
                    )
                    SettingsDivider()
                    SettingsRow(
                        icon = Icons.Default.Person,
                        title = strings.t("Cambiar nombre de usuario"),
                        detail = "@${profile?.username.orEmpty()}",
                        onClick = onEditUsername,
                    )
                    SettingsDivider()
                    SettingsRow(
                        icon = Icons.Default.Email,
                        title = strings.t("Cambiar correo"),
                        detail = profile?.email.orEmpty(),
                        onClick = onEditEmail,
                    )
                    SettingsDivider()
                    SettingsRow(
                        icon = Icons.Default.Lock,
                        title = strings.t("Cambiar contraseña"),
                        detail = strings.t("Disponible próximamente"),
                        onClick = {},
                        enabled = false,
                    )
                }
            }
            item {
                SettingsSection(title = strings.t("PREFERENCIAS")) {
                    SettingsRow(
                        icon = Icons.Default.Language,
                        title = strings.t("Seleccionar idioma"),
                        detail = strings.languageLabel(),
                        onClick = { showLanguageDialog = true },
                    )
                    SettingsDivider()
                    SettingsRow(
                        icon = Icons.Default.DarkMode,
                        title = strings.t("Tema oscuro"),
                        detail = if (profile?.theme == "DARK") {
                            strings.t("Activado")
                        } else {
                            strings.t("Cambiar entre claro y oscuro")
                        },
                        onClick = {
                            onUpdatePreferences(
                                strings.language.code,
                                if (profile?.theme == "DARK") "LIGHT" else "DARK",
                            )
                        },
                        trailing = {
                            Switch(
                                checked = profile?.theme == "DARK",
                                onCheckedChange = { enabled ->
                                    onUpdatePreferences(
                                        strings.language.code,
                                        if (enabled) "DARK" else "LIGHT",
                                    )
                                },
                                enabled = !isSaving,
                            )
                        },
                        showChevron = false,
                    )
                }
            }
            item {
                SettingsSection(
                    title = strings.t("ZONA DE PELIGRO"),
                    titleColor = MaterialTheme.colorScheme.error,
                ) {
                    SettingsRow(
                        icon = Icons.AutoMirrored.Filled.Logout,
                        title = strings.t("Cerrar sesión"),
                        detail = strings.t("Finalizar esta sesión en el dispositivo"),
                        onClick = onLogout,
                        tint = MaterialTheme.colorScheme.error,
                        showChevron = false,
                    )
                }
            }
            errorMessage?.let { item { InlineError(it) } }
            if (isSaving) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator(strokeWidth = 2.dp)
                    }
                }
            }
        }
    }

    if (showLanguageDialog) {
        LanguageDialog(
            selectedLanguage = strings.language.code,
            onDismiss = { showLanguageDialog = false },
            onLanguageSelected = { language ->
                showLanguageDialog = false
                onUpdatePreferences(language, profile?.theme ?: "LIGHT")
            },
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    titleColor: Color = MaterialTheme.colorScheme.primary,
    content: @Composable () -> Unit,
) {
    val strings = LocalAppStrings.current
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            Text(
                text = strings.t(title),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                color = titleColor,
                style = MaterialTheme.typography.labelMedium,
            )
            content()
        }
    }
}

@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    detail: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    showChevron: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
) {
    val strings = LocalAppStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (enabled) tint else tint.copy(alpha = 0.45f),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                strings.t(title),
                color = if (enabled) tint else tint.copy(alpha = 0.45f),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                detail,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = if (enabled) 1f else 0.55f,
                ),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        trailing?.invoke()
        if (showChevron) {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
            )
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(modifier = Modifier.padding(start = 58.dp, end = 16.dp))
}

@Composable
private fun LanguageDialog(
    selectedLanguage: String,
    onDismiss: () -> Unit,
    onLanguageSelected: (String) -> Unit,
) {
    val strings = LocalAppStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.t("Seleccionar idioma")) },
        text = {
            Column {
                LanguageOption(strings.t("Español"), "es", selectedLanguage, onLanguageSelected)
                LanguageOption("English", "en", selectedLanguage, onLanguageSelected)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(strings.t("Cerrar")) }
        },
    )
}

@Composable
private fun LanguageOption(
    label: String,
    value: String,
    selectedLanguage: String,
    onSelected: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelected(value) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selectedLanguage == value,
            onClick = { onSelected(value) },
        )
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun AccountEditDialog(
    field: ProfileField,
    profile: ProfileResponse?,
    isSaving: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit,
) {
    val strings = LocalAppStrings.current
    var value by remember(field, profile) {
        mutableStateOf(
            when (field) {
                ProfileField.DISPLAY_NAME -> profile?.displayName.orEmpty()
                ProfileField.USERNAME -> profile?.username.orEmpty()
                ProfileField.EMAIL -> profile?.email.orEmpty()
            },
        )
    }
    val isUsername = field == ProfileField.USERNAME
    val isEmail = field == ProfileField.EMAIL
    val isValid = when {
        isUsername -> value.trim().length in 3..50 && value.trim().matches(Regex("[A-Za-z0-9._-]+"))
        isEmail -> android.util.Patterns.EMAIL_ADDRESS.matcher(value.trim()).matches()
        else -> value.trim().length in 2..100
    }
    val titleKey = when (field) {
        ProfileField.DISPLAY_NAME -> "Cambiar nombre"
        ProfileField.USERNAME -> "Cambiar nombre de usuario"
        ProfileField.EMAIL -> "Cambiar correo"
    }
    val title = strings.t(titleKey)
    val label = strings.t(
        when (field) {
            ProfileField.DISPLAY_NAME -> "Nombre"
            ProfileField.USERNAME -> "Nombre de usuario"
            ProfileField.EMAIL -> "Correo electrónico"
        },
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(label) },
                    placeholder = {
                        Text(strings.t(when (field) {
                            ProfileField.DISPLAY_NAME -> "ej. Carlos Mendoza"
                            ProfileField.USERNAME -> "ej. carlos.mendoza"
                            ProfileField.EMAIL -> "ej. carlos@empresa.com"
                        }))
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = when {
                            isUsername -> KeyboardType.Ascii
                            isEmail -> KeyboardType.Email
                            else -> KeyboardType.Text
                        },
                    ),
                    isError = !isValid && value.isNotBlank(),
                    supportingText = {
                        Text(strings.t(when (field) {
                            ProfileField.DISPLAY_NAME -> "Usa entre 2 y 100 caracteres."
                            ProfileField.USERNAME -> "Usa entre 3 y 50 caracteres: letras, números, punto, guion o guion bajo."
                            ProfileField.EMAIL -> "Usa un correo válido para mantener tu cuenta segura."
                        }))
                    },
                    enabled = !isSaving,
                )
                errorMessage?.let { InlineError(it) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        if (isUsername) value else profile?.username.orEmpty(),
                        if (isEmail) value else profile?.email.orEmpty(),
                        if (field == ProfileField.DISPLAY_NAME) value else profile?.displayName.orEmpty(),
                    )
                },
                enabled = isValid && !isSaving,
            ) {
                Text(strings.t("Guardar"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) { Text(strings.t("Cancelar")) }
        },
    )
}
