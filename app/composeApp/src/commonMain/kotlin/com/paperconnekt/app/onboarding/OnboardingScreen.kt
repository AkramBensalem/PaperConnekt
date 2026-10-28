package com.paperconnekt.app.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.paperconnekt.storage.LocalResearchStore
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    store: LocalResearchStore,
    onFinished: () -> Unit,
) {
    var step by remember { mutableStateOf(OnboardingStep.WELCOME) }
    var workspaceName by remember { mutableStateOf("My research") }
    var zoteroEnabled by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun finish() {
        scope.launch {
            store.updateSettings {
                it.copy(
                    onboardingCompleted = true,
                    workspaceName = workspaceName.trim().ifEmpty { "My research" },
                    zoteroEnabled = zoteroEnabled,
                )
            }
            onFinished()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        when (step) {
            OnboardingStep.WELCOME -> WelcomeStep()
            OnboardingStep.WORKSPACE -> WorkspaceStep(
                workspaceName = workspaceName,
                onWorkspaceNameChanged = { workspaceName = it },
            )
            OnboardingStep.OPTIONAL_TOOLS -> OptionalToolsStep(
                zoteroEnabled = zoteroEnabled,
                onZoteroEnabledChanged = { zoteroEnabled = it },
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (step != OnboardingStep.WELCOME) {
                OutlinedButton(
                    onClick = {
                        step = when (step) {
                            OnboardingStep.WORKSPACE -> OnboardingStep.WELCOME
                            OnboardingStep.OPTIONAL_TOOLS -> OnboardingStep.WORKSPACE
                            OnboardingStep.WELCOME -> OnboardingStep.WELCOME
                        }
                    },
                ) {
                    Text("Back")
                }
            }

            Button(
                onClick = {
                    when (step) {
                        OnboardingStep.WELCOME -> step = OnboardingStep.WORKSPACE
                        OnboardingStep.WORKSPACE -> step = OnboardingStep.OPTIONAL_TOOLS
                        OnboardingStep.OPTIONAL_TOOLS -> finish()
                    }
                },
            ) {
                Text(if (step == OnboardingStep.OPTIONAL_TOOLS) "Create workspace" else "Continue")
            }
        }
    }
}

@Composable
private fun WelcomeStep() {
    Text("Welcome to PaperConnekt")
    Text("A private, local-first workspace for discovering papers, building graphs, and thinking with AI.")
}

@Composable
private fun WorkspaceStep(
    workspaceName: String,
    onWorkspaceNameChanged: (String) -> Unit,
) {
    Text("Name your workspace")
    Text("Your research stays on this device and can be organized into one or more local workspaces.")
    OutlinedTextField(
        value = workspaceName,
        onValueChange = onWorkspaceNameChanged,
        label = { Text("Workspace name") },
        singleLine = true,
    )
}

@Composable
private fun OptionalToolsStep(
    zoteroEnabled: Boolean,
    onZoteroEnabledChanged: (Boolean) -> Unit,
) {
    Text("Optional tools")
    Text("PaperConnekt works without remote services. Enable Zotero only if you want to connect it later.")
    OutlinedButton(onClick = { onZoteroEnabledChanged(!zoteroEnabled) }) {
        Text(if (zoteroEnabled) "Zotero enabled" else "Keep Zotero optional")
    }
}

