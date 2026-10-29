package com.paperconnekt.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.paperconnekt.domain.navigation.AppDestination
import com.paperconnekt.secrets.SecretStore
import com.paperconnekt.storage.PersistentResearchStore
import com.paperconnekt.storage.WorkspaceStorage
import com.paperconnekt.app.theme.PaperConnektTheme

@Composable
fun PaperConnektApp(
    secretStore: SecretStore,
    workspaceStorage: WorkspaceStorage,
) {
    val store = rememberResearchStore(workspaceStorage)
    LaunchedEffect(store) {
        store.initialize()
    }
    val initialized by store.isInitialized.collectAsState()
    val settings by store.settings.collectAsState()
    val startDestination = if (settings.onboardingCompleted) {
        AppDestination.DASHBOARD
    } else {
        AppDestination.ONBOARDING
    }

    PaperConnektTheme(preference = settings.selectedTheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = androidx.compose.material3.MaterialTheme.colorScheme.background,
        ) {
            if (initialized) {
                AppShell(
                    store = store,
                    secretStore = secretStore,
                    startDestination = startDestination,
                )
            }
        }
    }
}

@Composable
private fun rememberResearchStore(storage: WorkspaceStorage): PersistentResearchStore {
    return remember(storage) { PersistentResearchStore(storage) }
}
