package com.experimentos.mobile.shared.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.experimentos.mobile.authentication.presentation.LoginScreen
import com.experimentos.mobile.authentication.presentation.RegisterScreen
import com.experimentos.mobile.ai.presentation.AiScreen
import com.experimentos.mobile.humanresources.presentation.HrContentScreen
import com.experimentos.mobile.humanresources.presentation.HrHomeScreen
import com.experimentos.mobile.profile.presentation.ProfileScreen
import com.experimentos.mobile.report.presentation.CreateReportScreen
import com.experimentos.mobile.report.presentation.HrReportsScreen
import com.experimentos.mobile.shared.data.AppContainer
import com.experimentos.mobile.shared.data.Session
import com.experimentos.mobile.shared.data.SessionStore
import com.experimentos.mobile.mood.presentation.HomeScreen
import com.experimentos.mobile.shared.presentation.LoadingState
import com.experimentos.mobile.shared.presentation.LocalAppStrings
import com.experimentos.mobile.shared.presentation.SafeSpaceSnackbarHost
import com.experimentos.mobile.survey.presentation.SurveysScreen

private enum class EmployeeScreen(val label: String) {
    HOME("Inicio"),
    SURVEYS("Encuestas"),
    AI("Chat AI"),
    PROFILE("Perfil"),
}

private enum class HrScreen(val label: String) {
    HOME("Inicio"),
    CONTENT("Gestión"),
    REPORTS("Reportes"),
    PROFILE("Perfil"),
}

@Composable
fun AppNavHost(
    container: AppContainer,
    session: Session?,
) {
    val coroutineScope = rememberCoroutineScope()
    val sessionStore = container.sessionStore
    var showRegister by remember { mutableStateOf(false) }

    if (session == null) {
        if (showRegister) {
            RegisterScreen(
                api = container.authApi,
                sessionStore = sessionStore,
                onBackToLogin = { showRegister = false },
            )
        } else {
            LoginScreen(
                api = container.authApi,
                sessionStore = sessionStore,
                onRegister = { showRegister = true },
            )
        }
        return
    }

    // SYSTEM_ADMIN is intentionally not given a mobile destination.
    if (session.role != "EMPLOYEE" && session.role != "HR_MEMBER") {
        LaunchedEffect(session.role) { sessionStore.clear() }
        LoadingState(Modifier.padding(24.dp))
        return
    }

    if (session.role == "HR_MEMBER") {
        HrNavigation(container, session, sessionStore)
    } else {
        EmployeeNavigation(container, session, sessionStore)
    }
}

@Composable
private fun EmployeeNavigation(
    container: AppContainer,
    session: Session,
    sessionStore: SessionStore,
) {
    val coroutineScope = rememberCoroutineScope()
    val strings = LocalAppStrings.current
    var selectedScreen by rememberSaveable(session.role) { mutableStateOf(EmployeeScreen.HOME) }
    var showReportScreen by rememberSaveable(session.role) { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val screens = EmployeeScreen.entries

    fun showSnackbar(message: String) {
        coroutineScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short,
            )
        }
    }

    Scaffold(
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background,
        snackbarHost = {
            SafeSpaceSnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
            ) {
                screens.forEach { screen ->
                    val label = strings.t(screen.label)
                    NavigationBarItem(
                        selected = selectedScreen == screen,
                        onClick = {
                            selectedScreen = screen
                            showReportScreen = false
                        },
                        icon = { Icon(screen.icon(), contentDescription = label) },
                        label = { Text(label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer,
                            indicatorColor = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer,
                        ),
                    )
                }
            }
        },
    ) { paddingValues ->
        if (showReportScreen) {
            CreateReportScreen(
                reportApi = container.reportApi,
                onBack = { showReportScreen = false },
                sessionKey = session.username,
                modifier = Modifier.padding(paddingValues),
            )
        } else {
            when (selectedScreen) {
                EmployeeScreen.HOME -> HomeScreen(
                    username = session.username,
                    displayName = session.displayName,
                    moodApi = container.moodApi,
                    modifier = Modifier.padding(paddingValues),
                )
                EmployeeScreen.SURVEYS -> SurveysScreen(
                    surveyApi = container.surveyApi,
                    commentApi = container.commentApi,
                    activityApi = container.activityApi,
                    onOpenReport = { showReportScreen = true },
                    sessionKey = session.username,
                    modifier = Modifier.padding(paddingValues),
                )
                EmployeeScreen.AI -> AiScreen(
                    aiApi = container.aiApi,
                    sessionKey = session.username,
                    modifier = Modifier.padding(paddingValues),
                )
                EmployeeScreen.PROFILE -> ProfileScreen(
                    profileApi = container.profileApi,
                    appearanceStore = container.appearanceStore,
                    profilePhotoStore = container.profilePhotoStore,
                    session = session,
                    sessionStore = sessionStore,
                    onLogout = { coroutineScope.launch { sessionStore.clear() } },
                    onShowSnackbar = { message -> showSnackbar(message) },
                    modifier = Modifier.padding(paddingValues),
                )
            }
        }
    }
}

@Composable
private fun HrNavigation(
    container: AppContainer,
    session: Session,
    sessionStore: SessionStore,
) {
    val coroutineScope = rememberCoroutineScope()
    val strings = LocalAppStrings.current
    var selectedScreen by rememberSaveable(session.role) { mutableStateOf(HrScreen.HOME) }
    val snackbarHostState = remember { SnackbarHostState() }
    val screens = HrScreen.entries

    fun showSnackbar(message: String) {
        coroutineScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short,
            )
        }
    }

    Scaffold(
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background,
        snackbarHost = {
            SafeSpaceSnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
            ) {
                screens.forEach { screen ->
                    val label = strings.t(screen.label)
                    NavigationBarItem(
                        selected = selectedScreen == screen,
                        onClick = { selectedScreen = screen },
                        icon = {
                            Icon(
                                imageVector = screen.icon(),
                                contentDescription = label,
                            )
                        },
                        label = { Text(label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer,
                            indicatorColor = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer,
                        ),
                    )
                }
            }
        },
    ) { paddingValues ->
        when (selectedScreen) {
            HrScreen.HOME -> HrHomeScreen(
                moodApi = container.moodApi,
                username = session.username,
                displayName = session.displayName,
                sessionKey = session.username,
                modifier = Modifier.padding(paddingValues),
            )
            HrScreen.CONTENT -> HrContentScreen(
                surveyApi = container.surveyApi,
                activityApi = container.activityApi,
                commentApi = container.commentApi,
                sessionKey = session.username,
                modifier = Modifier.padding(paddingValues),
            )
            HrScreen.REPORTS -> HrReportsScreen(
                reportApi = container.reportApi,
                sessionKey = session.username,
                modifier = Modifier.padding(paddingValues),
            )
            HrScreen.PROFILE -> ProfileScreen(
                profileApi = container.profileApi,
                appearanceStore = container.appearanceStore,
                profilePhotoStore = container.profilePhotoStore,
                session = session,
                sessionStore = sessionStore,
                onLogout = { coroutineScope.launch { sessionStore.clear() } },
                onShowSnackbar = { message -> showSnackbar(message) },
                modifier = Modifier.padding(paddingValues),
            )
        }
    }
}

private fun EmployeeScreen.icon() = when (this) {
    EmployeeScreen.HOME -> Icons.Default.Home
    EmployeeScreen.SURVEYS -> Icons.Default.Poll
    EmployeeScreen.AI -> Icons.AutoMirrored.Filled.Chat
    EmployeeScreen.PROFILE -> Icons.Default.Person
}

private fun HrScreen.icon() = when (this) {
    HrScreen.HOME -> Icons.Default.Home
    HrScreen.CONTENT -> Icons.AutoMirrored.Filled.Assignment
    HrScreen.REPORTS -> Icons.Default.Poll
    HrScreen.PROFILE -> Icons.Default.Person
}
