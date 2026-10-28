package com.paperconnekt.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.paperconnekt.domain.navigation.AppDestination
import com.paperconnekt.storage.InMemoryResearchStore

@Composable
fun PaperConnektApp() {
    val store = rememberResearchStore()
    val settings by store.settings.collectAsState()
    val startDestination = if (settings.onboardingCompleted) {
        AppDestination.DASHBOARD
    } else {
        AppDestination.ONBOARDING
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            AppShell(
                store = store,
                startDestination = startDestination,
            )
        }
    }
}

@Composable
private fun rememberResearchStore(): InMemoryResearchStore {
    return androidx.compose.runtime.remember { InMemoryResearchStore() }
}

