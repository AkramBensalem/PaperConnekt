package com.paperconnekt.app

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import com.paperconnekt.domain.navigation.AppDestination
import com.paperconnekt.storage.LocalResearchStore

@Composable
fun DestinationScreen(
    destination: AppDestination,
    store: LocalResearchStore,
    paddingValues: PaddingValues,
) {
    when (destination) {
        AppDestination.DASHBOARD -> DashboardScreen(store, paddingValues)
        AppDestination.ASSISTANT -> AssistantScreen(store, paddingValues)
        AppDestination.GRAPH -> GraphScreen(paddingValues)
        AppDestination.LIBRARY -> LibraryScreen(paddingValues)
        AppDestination.SETTINGS -> SettingsScreen(store, paddingValues)
        AppDestination.ONBOARDING -> Unit
    }
}
