package com.paperconnekt.app

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.paperconnekt.domain.navigation.AppDestination
import com.paperconnekt.domain.model.Paper
import com.paperconnekt.secrets.SecretStore
import com.paperconnekt.storage.LocalResearchStore
import com.paperconnekt.app.onboarding.OnboardingScreen
import com.paperconnekt.domain.model.ResearchGraph
import com.paperconnekt.domain.model.ThemePreference
import com.paperconnekt.app.resources.Res
import com.paperconnekt.app.resources.paperconnekt_brand
import org.jetbrains.compose.resources.painterResource
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import io.github.aakira.napier.Napier
import kotlinx.datetime.Clock
import kotlin.random.Random

@Composable
fun AppShell(
    store: LocalResearchStore,
    secretStore: SecretStore,
    startDestination: AppDestination,
    menuState: AppMenuState,
) {
    val pdfNavigation = remember { PdfNavigationGuard() }
    CompositionLocalProvider(LocalPdfNavigationGuard provides pdfNavigation) {
        AppShellContent(store, secretStore, startDestination, pdfNavigation, menuState)
    }
}

@Composable
private fun AppShellContent(
    store: LocalResearchStore,
    secretStore: SecretStore,
    startDestination: AppDestination,
    pdfNavigation: PdfNavigationGuard,
    menuState: AppMenuState,
) {
    var destination by remember { mutableStateOf(startDestination) }
    var requestedPaper by remember { mutableStateOf<Paper?>(null) }
    var requestedGraphId by remember { mutableStateOf<String?>(null) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var showAddPaper by remember { mutableStateOf(false) }

    SideEffect {
        menuState.onWorkspaceAction = { action ->
            when (action) {
                AppMenuAction.QUIT -> pdfNavigation.navigate { menuState.exit() }
                AppMenuAction.ADD_PAPER -> pdfNavigation.navigate { showAddPaper = true }
                AppMenuAction.THEME_SYSTEM, AppMenuAction.THEME_LIGHT, AppMenuAction.THEME_DARK -> {
                    val theme = when (action) {
                        AppMenuAction.THEME_LIGHT -> ThemePreference.LIGHT
                        AppMenuAction.THEME_DARK -> ThemePreference.DARK
                        else -> ThemePreference.SYSTEM
                    }
                    scope.launch {
                        try {
                            store.updateSettings { it.copy(selectedTheme = theme) }
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (error: Exception) {
                            Napier.e("Could not save appearance", error)
                            menuState.reportError("Could not save your appearance preference. Please try again.")
                        }
                    }
                }
                else -> {
                    val target = when (action) {
                        AppMenuAction.DASHBOARD -> AppDestination.DASHBOARD
                        AppMenuAction.LIBRARY -> AppDestination.LIBRARY
                        AppMenuAction.GRAPH -> AppDestination.GRAPH
                        AppMenuAction.ASSISTANT -> AppDestination.ASSISTANT
                        AppMenuAction.SETTINGS -> AppDestination.SETTINGS
                        else -> error("Unsupported workspace action: $action")
                    }
                    if (destination != target) pdfNavigation.navigate { destination = target }
                }
            }
        }
    }
    DisposableEffect(menuState) {
        onDispose { menuState.onWorkspaceAction = null }
    }

    if (showAddPaper) {
        AddPaperDialog(
            onDismiss = { showAddPaper = false },
            onSave = { paper ->
                store.savePaper(paper)
                showAddPaper = false
            },
        )
    }

    fun openPaperGraph(paper: Paper) {
        scope.launch {
            val graph = ResearchGraph(
                id = "graph-${Random.nextLong()}",
                name = paper.title,
                paperIds = listOf(paper.id),
                seedPaperIds = listOf(paper.id),
                updatedAt = Clock.System.now(),
                discoveredPapers = listOf(paper),
            )
            store.saveGraph(graph)
            requestedGraphId = graph.id
            destination = AppDestination.GRAPH_CANVAS
        }
    }

    if (destination == AppDestination.ONBOARDING) {
        OnboardingScreen(
            store = store,
            onFinished = { destination = AppDestination.DASHBOARD },
        )
        return
    }

    val navigationItems = listOf(
        AppDestination.DASHBOARD to (NavigationGlyph.DASHBOARD to "Dashboard"),
        AppDestination.ASSISTANT to (NavigationGlyph.ASSISTANT to "Assistant"),
        AppDestination.GRAPH to (NavigationGlyph.GRAPH to "Graph"),
        AppDestination.LIBRARY to (NavigationGlyph.LIBRARY to "Library"),
        AppDestination.SETTINGS to (NavigationGlyph.SETTINGS to "Settings"),
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        if (destination == AppDestination.GRAPH_CANVAS) {
            // The canvas is a focused workspace. Keep it out of the application
            // navigation rail so it owns the complete available page.
            DestinationScreen(
                destination = destination,
                store = store,
                secretStore = secretStore,
                paddingValues = androidx.compose.foundation.layout.PaddingValues(0.dp),
                onNavigate = { destination = it },
                onPaperSelected = {
                    requestedPaper = it
                    destination = AppDestination.ASSISTANT
                },
                requestedPaper = requestedPaper,
                onPaperRequestConsumed = { requestedPaper = null },
                onGraphSelected = { requestedGraphId = it },
                requestedGraphId = requestedGraphId,
                onOpenGraph = ::openPaperGraph,
            )
        } else if (maxWidth >= 840.dp) {
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    modifier = Modifier.border(
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ),
                    containerColor = MaterialTheme.colorScheme.surface,
                    header = {
                        Column(
                            modifier = Modifier.padding(vertical = 20.dp),
                            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                        ) {
                            Image(
                                painter = painterResource(Res.drawable.paperconnekt_brand),
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                            )
                        }
                    },
                ) {
                    navigationItems.forEach { (item, label) ->
                        NavigationRailItem(
                            selected = destination == item,
                            onClick = { if (destination != item) pdfNavigation.navigate { destination = item } },
                            icon = {
                                NavigationIcon(
                                    glyph = label.first,
                                    contentDescription = label.second,
                                )
                            },
                            label = { Text(label.second) },
                            alwaysShowLabel = true,
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                ) {
                    DestinationScreen(
                        destination = destination,
                        store = store,
                        secretStore = secretStore,
                        paddingValues = androidx.compose.foundation.layout.PaddingValues(0.dp),
                        onNavigate = { destination = it },
                        onPaperSelected = {
                            requestedPaper = it
                            destination = AppDestination.ASSISTANT
                        },
                        requestedPaper = requestedPaper,
                        onPaperRequestConsumed = { requestedPaper = null },
                        onGraphSelected = { requestedGraphId = it },
                        requestedGraphId = requestedGraphId,
                        onOpenGraph = ::openPaperGraph,
                    )
                }
            }
        } else {
            Scaffold(
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        navigationItems.forEach { (item, label) ->
                            NavigationBarItem(
                                selected = destination == item,
                                onClick = { if (destination != item) pdfNavigation.navigate { destination = item } },
                                icon = {
                                    NavigationIcon(
                                        glyph = label.first,
                                        contentDescription = label.second,
                                    )
                                },
                                label = { Text(label.second) },
                            )
                        }
                    }
                },
            ) { paddingValues ->
                DestinationScreen(
                    destination = destination,
                    store = store,
                    secretStore = secretStore,
                    paddingValues = paddingValues,
                    onNavigate = { destination = it },
                    onPaperSelected = {
                        requestedPaper = it
                        destination = AppDestination.ASSISTANT
                    },
                    requestedPaper = requestedPaper,
                    onPaperRequestConsumed = { requestedPaper = null },
                    onGraphSelected = { requestedGraphId = it },
                    requestedGraphId = requestedGraphId,
                    onOpenGraph = ::openPaperGraph,
                )
            }
        }
    }
}
