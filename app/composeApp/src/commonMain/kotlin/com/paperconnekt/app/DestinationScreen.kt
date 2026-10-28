package com.paperconnekt.app

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import com.paperconnekt.domain.navigation.AppDestination
import com.paperconnekt.secrets.SecretStore
import com.paperconnekt.storage.LocalResearchStore

@Composable
fun DestinationScreen(
    destination: AppDestination,
    store: LocalResearchStore,
    secretStore: SecretStore,
    paddingValues: PaddingValues,
    onNavigate: (AppDestination) -> Unit = {},
) {
    when (destination) {
        AppDestination.DASHBOARD -> DashboardScreen(
            store = store,
            paddingValues = paddingValues,
            onOpenAssistant = { onNavigate(AppDestination.ASSISTANT) },
            onOpenLibrary = { onNavigate(AppDestination.LIBRARY) },
        )
        AppDestination.ASSISTANT -> AssistantScreen(store, paddingValues)
        AppDestination.GRAPH -> GraphScreen(store, paddingValues)
        AppDestination.LIBRARY -> LibraryScreen(store, paddingValues)
        AppDestination.SETTINGS -> SettingsScreen(store, secretStore, paddingValues)
        AppDestination.ONBOARDING -> Unit
    }
}
