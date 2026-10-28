package com.paperconnekt.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
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
                    AppDestination.DASHBOARD to (Icons.Default.Dashboard to "Dashboard"),
                    AppDestination.ASSISTANT to (Icons.Default.AutoAwesome to "Assistant"),
                    AppDestination.GRAPH to (Icons.Default.Hub to "Graph"),
                    AppDestination.LIBRARY to (Icons.Default.Folder to "Library"),
                    AppDestination.SETTINGS to (Icons.Default.Settings to "Settings"),
                ).forEach { (item, label) ->
                    NavigationBarItem(
                        selected = destination == item,
                        onClick = { destination = item },
                        icon = { Icon(label.first, contentDescription = label.second) },
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
