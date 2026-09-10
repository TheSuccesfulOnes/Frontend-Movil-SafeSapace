package com.experimentos.mobile.profile.presentation

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.experimentos.mobile.profile.data.ProfileApi
import com.experimentos.mobile.profile.data.ProfilePhotoStore
import com.experimentos.mobile.profile.data.ProfileResponse
import com.experimentos.mobile.shared.data.AppearanceStore
import com.experimentos.mobile.shared.data.Session
import com.experimentos.mobile.shared.data.SessionStore
import com.experimentos.mobile.shared.presentation.ErrorState
import com.experimentos.mobile.shared.presentation.InitialAvatar
import com.experimentos.mobile.shared.presentation.LocalAppStrings
import com.experimentos.mobile.shared.presentation.LoadingState

@Composable
fun ProfileScreen(
    profileApi: ProfileApi,
    appearanceStore: AppearanceStore,
    profilePhotoStore: ProfilePhotoStore,
    session: Session,
    sessionStore: SessionStore,
    onLogout: () -> Unit,
    onShowSnackbar: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val profileViewModel: ProfileViewModel = viewModel(
        key = "profile-${session.role}-${session.username}",
        factory = ProfileViewModelFactory(profileApi, appearanceStore, sessionStore),
    )
    val state by profileViewModel.state.collectAsStateWithLifecycle()
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var editField by rememberSaveable { mutableStateOf<ProfileField?>(null) }
    var profilePhotoUri by rememberSaveable(session.username) {
        mutableStateOf(profilePhotoStore.getUri(session.username))
    }
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { selectedUri ->
        selectedUri?.let { uri ->
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            profilePhotoStore.saveUri(session.username, uri.toString())
            profilePhotoUri = uri.toString()
        }
    }

    LaunchedEffect(state.message, state.isSaving, strings.language) {
        if (state.message != null) {
            editField = null
        }
        if (!state.isSaving) {
            state.message
                ?.takeIf(String::isNotBlank)
                ?.let { feedback ->
                    onShowSnackbar(strings.translateMessage(feedback))
                    profileViewModel.clearFeedback()
                }
        }
    }

    if (state.isLoading) {
        LoadingState(modifier.fillMaxSize())
        return
    }

    if (showSettings) {
        ProfileSettingsScreen(
            profile = state.profile,
            isSaving = state.isSaving,
            errorMessage = state.errorMessage,
            onBack = { showSettings = false },
            onEditUsername = {
                profileViewModel.clearFeedback()
                editField = ProfileField.USERNAME
            },
            onEditDisplayName = {
                profileViewModel.clearFeedback()
                editField = ProfileField.DISPLAY_NAME
            },
            onEditEmail = {
                profileViewModel.clearFeedback()
                editField = ProfileField.EMAIL
            },
            onUpdatePreferences = profileViewModel::updatePreferences,
            onLogout = onLogout,
            modifier = modifier,
        )
    } else {
        ProfileOverview(
            profile = state.profile,
            session = session,
            photoUri = profilePhotoUri,
            onPhotoClick = {
                photoPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
            errorMessage = state.errorMessage,
            onOpenSettings = {
                profileViewModel.clearFeedback()
                showSettings = true
            },
            onRetry = profileViewModel::load,
            modifier = modifier,
        )
    }

    editField?.let { field ->
        AccountEditDialog(
            field = field,
            profile = state.profile,
            isSaving = state.isSaving,
            errorMessage = state.errorMessage,
            onDismiss = {
                if (!state.isSaving) {
                    editField = null
                    profileViewModel.clearFeedback()
                }
            },
            onSave = { username, email, displayName ->
                profileViewModel.updateAccount(username, email, displayName)
            },
        )
    }
}

@Composable
private fun ProfileOverview(
    profile: ProfileResponse?,
    session: Session,
    photoUri: String?,
    onPhotoClick: () -> Unit,
    errorMessage: String?,
    onOpenSettings: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalAppStrings.current
    val displayName = profile?.displayName ?: session.displayName
    val username = profile?.username ?: session.username
    val email = profile?.email.orEmpty()
    val role = profile?.role ?: session.role

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { ProfileTopBar(onSettingsClick = onOpenSettings) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ProfileAvatar(
                            name = displayName,
                            photoUri = photoUri,
                            onClick = onPhotoClick,
                        )
                        Text(
                            displayName,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            strings.t("Tu espacio personal de bienestar"),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
            item {
                ProfileDetailsCard(
                    username = username,
                    email = email,
                    role = strings.roleLabel(role),
                )
            }
            errorMessage?.let { item { ErrorState(it, onRetry = onRetry) } }
        }
    }
}

@Composable
private fun ProfileDetailsCard(username: String, email: String, role: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            ProfileDetailRow(
                icon = Icons.Default.Person,
                label = "Usuario",
                value = "@$username",
            )
            HorizontalDivider(modifier = Modifier.padding(start = 64.dp, end = 20.dp))
            ProfileDetailRow(
                icon = Icons.Default.Email,
                label = "Correo electrónico",
                value = email,
            )
            HorizontalDivider(modifier = Modifier.padding(start = 64.dp, end = 20.dp))
            ProfileDetailRow(
                icon = Icons.Default.Badge,
                label = "Rol",
                value = role,
            )
        }
    }
}

@Composable
private fun ProfileDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
) {
    val strings = LocalAppStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = MaterialTheme.shapes.small,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(9.dp),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                strings.t(label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                value.ifBlank { strings.t("No disponible") },
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun ProfileAvatar(
    name: String,
    photoUri: String?,
    onClick: () -> Unit,
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = null, photoUri) {
        value = photoUri?.let { uriString ->
            withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(Uri.parse(uriString))?.use {
                        BitmapFactory.decodeStream(it)
                    }
                }.getOrNull()
            }
        }
    }

    Box(contentAlignment = Alignment.BottomEnd) {
        Surface(
            modifier = Modifier
                .size(112.dp)
                .clip(CircleShape)
                .clickable(onClick = onClick),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            shadowElevation = 5.dp,
        ) {
            if (bitmap != null) {
                androidx.compose.foundation.Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = strings.t("Foto de perfil"),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                InitialAvatar(name, size = 112.dp)
            }
        }
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shadowElevation = 2.dp,
        ) {
            Icon(
                Icons.Default.AddAPhoto,
                contentDescription = strings.t("Cambiar foto de perfil"),
                modifier = Modifier.padding(8.dp),
            )
        }
    }
}

@Composable
private fun ProfileTopBar(onSettingsClick: () -> Unit) {
    val strings = LocalAppStrings.current
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(strings.t("Perfil"), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Default.Settings, contentDescription = strings.t("Abrir ajustes"))
            }
        }
    }
}
