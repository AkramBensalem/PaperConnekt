package com.paperconnekt.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.paperconnekt.domain.navigation.AppDestination
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

    if (destination == AppDestination.ONBOARDING) {
        OnboardingScreen(
            store = store,
            onFinished = { destination = AppDestination.DASHBOARD },
        )
        return
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                listOf(
                    AppDestination.DASHBOARD to (NavigationGlyph.DASHBOARD to "Dashboard"),
                    AppDestination.ASSISTANT to (NavigationGlyph.ASSISTANT to "Assistant"),
                    AppDestination.GRAPH to (NavigationGlyph.GRAPH to "Graph"),
                    AppDestination.LIBRARY to (NavigationGlyph.LIBRARY to "Library"),
                    AppDestination.SETTINGS to (NavigationGlyph.SETTINGS to "Settings"),
                ).forEach { (item, label) ->
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
        Column(modifier = Modifier.fillMaxSize()) {
            DestinationScreen(
                destination = destination,
                store = store,
                secretStore = secretStore,
                paddingValues = paddingValues,
            )
        }
    }
}
