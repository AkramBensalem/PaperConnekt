package com.paperconnekt.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.paperconnekt.storage.LocalResearchStore

@Composable
fun DashboardScreen(paddingValues: PaddingValues) {
    PlaceholderScreen("Research dashboard", "Search, save, and organize papers locally.", paddingValues)
}

@Composable
fun AssistantScreen(paddingValues: PaddingValues) {
    PlaceholderScreen("AI assistance", "Your paper conversations and PDF workspace will live here.", paddingValues)
}

@Composable
fun GraphScreen(paddingValues: PaddingValues) {
    PlaceholderScreen("Research graphs", "Explore saved connections between papers.", paddingValues)
}

@Composable
fun LibraryScreen(paddingValues: PaddingValues) {
    PlaceholderScreen("Library", "Organize papers in a local Zotero-style hierarchy.", paddingValues)
}

@Composable
fun SettingsScreen(
    store: LocalResearchStore,
    paddingValues: PaddingValues,
) {
    PlaceholderScreen("Settings", "Configure secure local AI and Zotero keys.", paddingValues)
}

@Composable
private fun PlaceholderScreen(
    title: String,
    description: String,
    paddingValues: PaddingValues,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title)
        Text(description)
    }
}

