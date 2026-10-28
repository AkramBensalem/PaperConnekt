package com.paperconnekt.app

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.paperconnekt.domain.navigation.AppDestination
import com.paperconnekt.domain.model.Paper
import com.paperconnekt.secrets.SecretStore
import com.paperconnekt.storage.LocalResearchStore
import com.paperconnekt.app.onboarding.OnboardingScreen

@Composable
fun AppShell(
    store: LocalResearchStore,
    secretStore: SecretStore,
    startDestination: AppDestination,
) {
    var destination by remember { mutableStateOf(startDestination) }
    var requestedPaper by remember { mutableStateOf<Paper?>(null) }

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
        if (maxWidth >= 840.dp) {
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    header = {
                        Column(
                            modifier = Modifier.padding(vertical = 20.dp),
                            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = "P",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = "PaperConnekt",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                ) {
                    navigationItems.forEach { (item, label) ->
                        NavigationRailItem(
                            selected = destination == item,
                            onClick = { destination = item },
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
                                onClick = { destination = item },
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
                )
            }
        }
    }
}
