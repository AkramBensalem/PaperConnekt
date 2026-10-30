package com.paperconnekt.app

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import com.paperconnekt.domain.model.Paper
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
    onPaperSelected: (Paper) -> Unit = {},
    requestedPaper: Paper? = null,
    onPaperRequestConsumed: () -> Unit = {},
    onGraphSelected: (String) -> Unit = {},
    requestedGraphId: String? = null,
    onOpenGraph: (Paper) -> Unit = {},
) {
    when (destination) {
        AppDestination.DASHBOARD -> DashboardScreen(
            store = store,
            secretStore = secretStore,
            paddingValues = paddingValues,
            onOpenAssistant = { onNavigate(AppDestination.ASSISTANT) },
            onOpenLibrary = { onNavigate(AppDestination.LIBRARY) },
            onOpenGraph = onOpenGraph,
        )
        AppDestination.ASSISTANT -> AssistantScreen(
            store = store,
            paddingValues = paddingValues,
            initialPaper = requestedPaper,
            onInitialPaperConsumed = onPaperRequestConsumed,
            secretStore = secretStore,
        )
        AppDestination.GRAPH -> GraphScreen(
            store = store,
            paddingValues = paddingValues,
            onOpenCanvas = { graphId ->
                onGraphSelected(graphId)
                onNavigate(AppDestination.GRAPH_CANVAS)
            },
        )
        AppDestination.GRAPH_CANVAS -> GraphCanvasScreen(
            store = store,
            secretStore = secretStore,
            paddingValues = paddingValues,
            graphId = requestedGraphId,
            onBack = { onNavigate(AppDestination.GRAPH) },
        )
        AppDestination.LIBRARY -> LibraryScreen(
            store = store,
            secretStore = secretStore,
            paddingValues = paddingValues,
            onOpenPaper = {
                onPaperSelected(it)
            },
            onOpenGraph = onOpenGraph,
        )
        AppDestination.SETTINGS -> SettingsScreen(store, secretStore, paddingValues)
        AppDestination.ONBOARDING -> Unit
    }
}
