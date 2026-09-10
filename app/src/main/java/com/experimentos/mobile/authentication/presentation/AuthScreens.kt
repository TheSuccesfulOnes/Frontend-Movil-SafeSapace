package com.experimentos.mobile.authentication.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.experimentos.mobile.authentication.data.AuthApi
import com.experimentos.mobile.authentication.domain.DefaultAuthRepository
import com.experimentos.mobile.shared.data.SessionStore
import com.experimentos.mobile.shared.presentation.BrandEmblem
import com.experimentos.mobile.shared.presentation.InlineError
import com.experimentos.mobile.shared.presentation.LocalAppStrings

@Composable
fun LoginScreen(
    api: AuthApi,
    sessionStore: SessionStore,
    onRegister: () -> Unit,
    onPasswordRecovery: () -> Unit,
) {
    val strings = LocalAppStrings.current
    val viewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(DefaultAuthRepository(api), sessionStore),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    var identifier by rememberSaveable { mutableStateOf("") }
    // Passwords are intentionally not placed in saved instance state.
    var password by remember { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }

    AuthLayout(
        title = strings.t("Bienvenido de nuevo"),
        subtitle = strings.t("Vuelve a tu espacio seguro para cuidar de ti."),
    ) {
        AuthTextField(
            value = identifier,
            onValueChange = { identifier = it },
            label = strings.t("Usuario o correo"),
            placeholder = strings.t("ej. carlos o carlos@empresa.com"),
            icon = Icons.Default.Person,
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Next,
        )
        Spacer(Modifier.height(14.dp))
        AuthTextField(
            value = password,
            onValueChange = { password = it },
            label = strings.t("Contraseña"),
            placeholder = strings.t("Escribe tu contraseña"),
            icon = Icons.Default.Lock,
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
            isPassword = true,
            passwordVisible = showPassword,
            onPasswordVisibilityChange = { showPassword = !showPassword },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onPasswordRecovery) {
                Text(strings.t("¿Olvidaste tu contraseña?"))
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { viewModel.login(identifier, password) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            enabled = !state.isLoading,
            shape = MaterialTheme.shapes.small,
        ) {
            Text(strings.t(if (state.isLoading) "Validando…" else "Iniciar sesión"))
        }
        state.errorMessage?.let { InlineError(it) }
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(strings.t("¿No tienes cuenta?"), color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = onRegister) { Text(strings.t("Regístrate")) }
        }
    }

}

@Composable
fun PasswordRecoveryScreen(
    api: AuthApi,
    onBackToLogin: () -> Unit,
) {
    val strings = LocalAppStrings.current
    val viewModel: PasswordRecoveryViewModel = viewModel(
        factory = PasswordRecoveryViewModelFactory.forApi(api),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    var identifier by rememberSaveable { mutableStateOf("") }
    var token by rememberSaveable { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var showNewPassword by rememberSaveable { mutableStateOf(false) }
    var showConfirmation by rememberSaveable { mutableStateOf(false) }
    var showResetForm by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.completed) {
        if (state.completed) {
            showResetForm = false
            newPassword = ""
            confirmation = ""
        }
    }

    AuthLayout(
        title = strings.t("Recuperar acceso"),
        subtitle = strings.t("Te ayudaremos a volver a tu espacio seguro."),
    ) {
        if (state.completed) {
            RecoverySuccessCard(onBackToLogin = onBackToLogin)
        } else if (!showResetForm) {
            Text(
                strings.t("Escribe tu usuario o correo y te enviaremos un enlace de recuperación."),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(18.dp))
            AuthTextField(
                value = identifier,
                onValueChange = { identifier = it },
                label = strings.t("Usuario o correo"),
                placeholder = strings.t("ej. carlos o carlos@empresa.com"),
                icon = Icons.Default.Person,
                imeAction = ImeAction.Done,
            )
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = { viewModel.requestRecovery(identifier) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !state.isLoading,
                shape = MaterialTheme.shapes.small,
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.size(8.dp))
                }
                Text(strings.t(if (state.isLoading) "Enviando…" else "Enviar enlace"))
            }
            if (state.requestSent) {
                Spacer(Modifier.height(14.dp))
                InlineSuccess(
                    strings.t("Si la cuenta existe, recibirás instrucciones para recuperar el acceso."),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { showResetForm = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(strings.t("Ya tengo el enlace"))
                }
            }
        } else {
            Text(
                strings.t("Pega el token que recibiste para crear una nueva contraseña."),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(18.dp))
            AuthTextField(
                value = token,
                onValueChange = { token = it },
                label = strings.t("Token de recuperación"),
                placeholder = strings.t("Pega aquí tu token"),
                icon = Icons.Default.Lock,
                imeAction = ImeAction.Next,
            )
            Spacer(Modifier.height(14.dp))
            AuthTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = strings.t("Nueva contraseña"),
                placeholder = strings.t("Mínimo 8 caracteres"),
                icon = Icons.Default.Lock,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next,
                isPassword = true,
                passwordVisible = showNewPassword,
                onPasswordVisibilityChange = { showNewPassword = !showNewPassword },
            )
            Spacer(Modifier.height(14.dp))
            AuthTextField(
                value = confirmation,
                onValueChange = { confirmation = it },
                label = strings.t("Confirmar contraseña"),
                placeholder = strings.t("Repite tu contraseña"),
                icon = Icons.Default.Lock,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
                isPassword = true,
                passwordVisible = showConfirmation,
                onPasswordVisibilityChange = { showConfirmation = !showConfirmation },
            )
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = { viewModel.confirmRecovery(token, newPassword, confirmation) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !state.isLoading,
                shape = MaterialTheme.shapes.small,
            ) {
                Text(strings.t(if (state.isLoading) "Guardando…" else "Cambiar contraseña"))
            }
        }
        state.errorMessage?.let { InlineError(it) }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onBackToLogin,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            enabled = !state.isLoading,
            shape = MaterialTheme.shapes.small,
        ) {
            Text(strings.t("Volver al inicio de sesión"))
        }
    }
}

@Composable
private fun InlineSuccess(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(message, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
private fun RecoverySuccessCard(onBackToLogin: () -> Unit) {
    val strings = LocalAppStrings.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
                strings.t("Contraseña actualizada"),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                strings.t("Tu contraseña fue actualizada. Ya puedes iniciar sesión."),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Button(
                onClick = onBackToLogin,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
            ) {
                Text(strings.t("Volver al inicio de sesión"))
            }
        }
    }
}

@Composable
fun RegisterScreen(
    api: AuthApi,
    sessionStore: SessionStore,
    onBackToLogin: () -> Unit,
) {
    val strings = LocalAppStrings.current
    val viewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(DefaultAuthRepository(api), sessionStore),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    var username by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    // Passwords are intentionally not placed in saved instance state.
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var showConfirmation by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.registrationCompleted) {
        if (state.registrationCompleted) onBackToLogin()
    }

    AuthLayout(
        title = strings.t("Crea tu cuenta"),
        subtitle = strings.t("SafeSpace · Tu santuario digital."),
    ) {
        AuthTextField(
            value = username,
            onValueChange = { username = it },
            label = strings.t("Nombre de usuario"),
            placeholder = strings.t("ej. carlos.mendoza"),
            icon = Icons.Default.Person,
            imeAction = ImeAction.Next,
        )
        Spacer(Modifier.height(14.dp))
        AuthTextField(
            value = email,
            onValueChange = { email = it },
            label = strings.t("Correo electrónico"),
            placeholder = strings.t("ej. carlos@empresa.com"),
            icon = Icons.Default.Email,
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
        )
        Spacer(Modifier.height(14.dp))
        AuthTextField(
            value = password,
            onValueChange = { password = it },
            label = strings.t("Contraseña"),
            placeholder = strings.t("Mínimo 8 caracteres"),
            icon = Icons.Default.Lock,
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Next,
            isPassword = true,
            passwordVisible = showPassword,
            onPasswordVisibilityChange = { showPassword = !showPassword },
        )
        Spacer(Modifier.height(14.dp))
        AuthTextField(
            value = confirmation,
            onValueChange = { confirmation = it },
            label = strings.t("Confirmar contraseña"),
            placeholder = strings.t("Repite tu contraseña"),
            icon = Icons.Default.Lock,
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
            isPassword = true,
            passwordVisible = showConfirmation,
            onPasswordVisibilityChange = { showConfirmation = !showConfirmation },
        )
        Spacer(Modifier.height(22.dp))
        Button(
            onClick = { viewModel.register(username, email, password, confirmation) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            enabled = !state.isLoading,
            shape = MaterialTheme.shapes.small,
        ) {
            Text(strings.t(if (state.isLoading) "Creando cuenta…" else "Registrar cuenta"))
        }
        Text(
            strings.t("Al registrarte aceptas nuestras condiciones de uso y política de privacidad."),
            modifier = Modifier.padding(top = 12.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        state.errorMessage?.let { InlineError(it) }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(strings.t("¿Ya tienes una cuenta?"), color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = onBackToLogin) { Text(strings.t("Inicia sesión")) }
        }
    }
}

@Composable
private fun AuthLayout(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        BrandEmblem()
        Spacer(Modifier.height(18.dp))
        Text(
            "SAFESPACE",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(24.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                Text(title, style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(8.dp))
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(26.dp))
                content()
            }
        }
    }
}

@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Default,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onPasswordVisibilityChange: (() -> Unit)? = null,
) {
    val strings = LocalAppStrings.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        trailingIcon = if (isPassword && onPasswordVisibilityChange != null) {
            {
                IconButton(onClick = onPasswordVisibilityChange) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = strings.t(
                            if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña",
                        ),
                    )
                }
            }
        } else {
            null
        },
        visualTransformation = if (isPassword && !passwordVisible) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        singleLine = true,
        shape = MaterialTheme.shapes.small,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
            focusedContainerColor = MaterialTheme.colorScheme.surface,
        ),
    )
}
