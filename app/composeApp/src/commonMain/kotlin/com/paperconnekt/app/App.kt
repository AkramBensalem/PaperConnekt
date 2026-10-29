package com.paperconnekt.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
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
    menuState: AppMenuState = remember { AppMenuState() },
    showMobileMenu: Boolean = false,
    menuDialogHost: AppMenuDialogHost = MaterialAppMenuDialogHost,
) {
    val store = rememberResearchStore(workspaceStorage)
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
    val startDestination = if (settings.onboardingCompleted) {
        AppDestination.DASHBOARD
    } else {
        AppDestination.ONBOARDING
    }

    CompositionLocalProvider(LocalPdfAttachments provides pdfAttachments) {
        PaperConnektTheme(preference = settings.selectedTheme) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = androidx.compose.material3.MaterialTheme.colorScheme.background,
            ) {
                Column(Modifier.fillMaxSize()) {
                    if (showMobileMenu) MobileAppMenu(menuState)
                    Box(Modifier.weight(1f).fillMaxSize()) {
                        if (initialized) {
                            AppShell(
                                store = store,
                                secretStore = secretStore,
                                startDestination = startDestination,
                                menuState = menuState,
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
                                    Text(initializationError.orEmpty())
                                    Button(onClick = { attempt++ }) { Text("Retry") }
                                }
                            }
                        }
                    }
                }
                AppMenuDialogs(menuState, menuDialogHost)
            }
        }
    }
}

@Composable
private fun rememberResearchStore(storage: WorkspaceStorage): PersistentResearchStore {
    return remember(storage) { PersistentResearchStore(storage) }
}
