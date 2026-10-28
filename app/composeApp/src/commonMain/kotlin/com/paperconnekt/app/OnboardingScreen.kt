package com.paperconnekt.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.paperconnekt.storage.LocalResearchStore
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun OnboardingScreen(
    store: LocalResearchStore,
    onFinished: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Welcome to PaperConnekt")
        Text("A local-first research workspace for exploring papers, ideas, and connections.")
        Button(
            onClick = {
                scope.launch {
                    store.updateSettings { it.copy(onboardingCompleted = true) }
                    onFinished()
                }
            },
        ) {
            Text("Start researching")
        }
    }
}

