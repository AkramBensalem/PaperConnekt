package com.paperconnekt.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import com.akram.bensalem.paper.connekt.feature.mcp.LocalMcpHost
import com.akram.bensalem.paper.connekt.feature.mcp.McpHost
import com.akram.bensalem.paper.connekt.feature.mcp.McpHostConfig
import com.paperconnekt.domain.navigation.AppDestination
import com.paperconnekt.secrets.SecretStore
import com.paperconnekt.storage.PersistentResearchStore
import com.paperconnekt.storage.WorkspaceStorage
import com.paperconnekt.app.theme.PaperConnektTheme
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException

@Composable
fun PaperConnektApp(
    secretStore: SecretStore,
    workspaceStorage: WorkspaceStorage,
    pdfAttachments: PdfAttachmentUi,
    initialDestination: AppDestination? = null,
    requestedDestination: AppDestination? = null,
    onDestinationChanged: (AppDestination) -> Unit = {},
    menuState: AppMenuState = remember { AppMenuState() },
    showMobileMenu: Boolean = false,
    menuDialogHost: AppMenuDialogHost = MaterialAppMenuDialogHost,
    mcpHost: McpHost? = null,
) {
    val store = rememberResearchStore(workspaceStorage)
    var mobileDrawerOpen by remember { mutableStateOf(false) }
    var currentDestination by remember { mutableStateOf<AppDestination?>(null) }
    var focusedWorkspaceOpen by remember { mutableStateOf(false) }
    var initializationError by remember(store) { mutableStateOf<String?>(null) }
    var attempt by remember(store) { mutableStateOf(0) }
    LaunchedEffect(store, attempt) {
        initializationError = null
        try {
            store.initialize()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Napier.e("Could not load the saved workspace", error)
            initializationError = error.message ?: "Could not load the saved workspace."
        }
    }
    val initialized by store.isInitialized.collectAsState()
    val settings by store.settings.collectAsState()
    SideEffect {
        menuState.workspaceReady = initialized && settings.onboardingCompleted
    }
    if (mcpHost != null) {
        val mcpConfig = McpHostConfig(
            enabled = initialized && settings.mcpServerEnabled,
            port = settings.mcpServerPort,
            allowIdeaWrites = settings.mcpAllowIdeaWrites,
        )
        LaunchedEffect(mcpHost, mcpConfig) { mcpHost.apply(mcpConfig, store, secretStore) }
    }
    val startDestination = if (settings.onboardingCompleted) {
        AppDestination.DASHBOARD
    } else {
        AppDestination.ONBOARDING
    }
    val effectiveStartDestination = if (settings.onboardingCompleted) {
        initialDestination ?: startDestination
    } else {
        startDestination
    }
    CompositionLocalProvider(LocalPdfAttachments provides pdfAttachments, LocalMcpHost provides mcpHost) {
        PaperConnektTheme(preference = settings.selectedTheme) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = androidx.compose.material3.MaterialTheme.colorScheme.background,
            ) {
                Column(Modifier.fillMaxSize()) {
                    if (showMobileMenu) {
                        BoxWithConstraints(
                            modifier = Modifier.weight(1f).fillMaxSize(),
                        ) {
                            val appScale by animateFloatAsState(
                                targetValue = if (mobileDrawerOpen) 0.5f else 1f,
                            )
                            val drawerWidth = (maxWidth * 0.5f).coerceIn(176.dp, 280.dp)
                            val shrunkenAppMargin = if (mobileDrawerOpen) 12.dp else 0.dp

                            if (mobileDrawerOpen) {
                                MobileAppDrawer(
                                    state = menuState,
                                    drawerWidth = drawerWidth,
                                    onDismiss = { mobileDrawerOpen = false },
                                )
                            }

                            Surface(
                                modifier = Modifier
                                    .padding(
                                        end = shrunkenAppMargin,
                                        bottom = shrunkenAppMargin,
                                    )
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        transformOrigin = TransformOrigin(1f, 1f)
                                        scaleX = appScale
                                        scaleY = appScale
                                        shadowElevation = if (mobileDrawerOpen) 24.dp.toPx() else 0f
                                        shape = if (mobileDrawerOpen) {
                                            RoundedCornerShape(28.dp)
                                        } else {
                                            RectangleShape
                                        }
                                        clip = mobileDrawerOpen
                                    },
                                shape = if (mobileDrawerOpen) {
                                    RoundedCornerShape(28.dp)
                                } else {
                                    RectangleShape
                                },
                                tonalElevation = if (mobileDrawerOpen) 8.dp else 0.dp,
                            ) {
                                Box(Modifier.fillMaxSize()) {
                                    Column(Modifier.fillMaxSize()) {
                                        if (!mobileDrawerOpen &&
                                            currentDestination !in setOf(
                                                AppDestination.GRAPH,
                                                AppDestination.GRAPH_CANVAS,
                                            ) &&
                                            !focusedWorkspaceOpen
                                        ) {
                                            MobileAppMenu(
                                                drawerOpen = false,
                                                onDrawerOpenChange = { mobileDrawerOpen = it },
                                            )
                                        }
                                        Box(Modifier.weight(1f).fillMaxSize()) {
                                            AppContent(
                                                initialized = initialized,
                                                initializationError = initializationError,
                                                attempt = { attempt++ },
                                                store = store,
                                                secretStore = secretStore,
                                                startDestination = effectiveStartDestination,
                                                requestedDestination = requestedDestination,
                                                menuState = menuState,
                                                hideMobileNavigation = mobileDrawerOpen,
                                                onDestinationChanged = {
                                                    currentDestination = it
                                                    onDestinationChanged(it)
                                                },
                                                onFocusedWorkspaceChanged = { focusedWorkspaceOpen = it },
                                            )
                                        }
                                    }
                                }
                            }
                            if (mobileDrawerOpen) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(
                                            end = shrunkenAppMargin,
                                            bottom = shrunkenAppMargin,
                                        )
                                        .fillMaxWidth(appScale)
                                        .fillMaxHeight(appScale)
                                        .border(
                                            width = 2.dp,
                                            color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                            shape = RoundedCornerShape(14.dp),
                                        )
                                        .clickable(onClick = { mobileDrawerOpen = false }),
                                    )
                            }
                        }
                    } else {
                        Box(Modifier.weight(1f).fillMaxSize()) {
                            AppContent(
                                initialized = initialized,
                                initializationError = initializationError,
                                attempt = { attempt++ },
                                store = store,
                                secretStore = secretStore,
                                startDestination = effectiveStartDestination,
                                requestedDestination = requestedDestination,
                                menuState = menuState,
                                onDestinationChanged = onDestinationChanged,
                            )
                        }
                    }
                }
                AppMenuDialogs(menuState, menuDialogHost)
            }
        }
    }
}

@Composable
private fun AppContent(
    initialized: Boolean,
    initializationError: String?,
    attempt: () -> Unit,
    store: PersistentResearchStore,
    secretStore: SecretStore,
    startDestination: AppDestination,
    requestedDestination: AppDestination? = null,
    menuState: AppMenuState,
    hideMobileNavigation: Boolean = false,
    onDestinationChanged: (AppDestination) -> Unit = {},
    onFocusedWorkspaceChanged: (Boolean) -> Unit = {},
) {
    if (initialized) {
        AppShell(
            store = store,
            secretStore = secretStore,
            startDestination = startDestination,
            requestedDestination = requestedDestination,
            menuState = menuState,
            hideMobileNavigation = hideMobileNavigation,
            onDestinationChanged = onDestinationChanged,
            onFocusedWorkspaceChanged = onFocusedWorkspaceChanged,
        )
    } else {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (initializationError == null) {
                CircularProgressIndicator()
            } else {
                Text(initializationError)
                Button(onClick = attempt) { Text("Retry") }
            }
        }
    }
}

@Composable
private fun rememberResearchStore(storage: WorkspaceStorage): PersistentResearchStore {
    return remember(storage) { PersistentResearchStore(storage) }
}
