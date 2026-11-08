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
import com.paperconnekt.app.resources.Res
import org.jetbrains.compose.resources.stringResource
import kotlinx.coroutines.launch
import com.paperconnekt.app.resources.back
import com.paperconnekt.app.resources.continue_action
import com.paperconnekt.app.resources.create_workspace
import com.paperconnekt.app.resources.keep_zotero_optional
import com.paperconnekt.app.resources.name_your_workspace
import com.paperconnekt.app.resources.onboarding_description
import com.paperconnekt.app.resources.onboarding_workspace_description
import com.paperconnekt.app.resources.optional_tools
import com.paperconnekt.app.resources.optional_tools_description
import com.paperconnekt.app.resources.welcome_to_paperconnekt
import com.paperconnekt.app.resources.workspace_name
import com.paperconnekt.app.resources.zotero_enabled

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
                    Text(stringResource(Res.string.back))
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
                Text(
                    stringResource(
                        if (step == OnboardingStep.OPTIONAL_TOOLS) {
                            Res.string.create_workspace
                        } else {
                            Res.string.continue_action
                        },
                    ),
                )
            }
        }
    }
}

@Composable
private fun WelcomeStep() {
    Text(stringResource(Res.string.welcome_to_paperconnekt))
    Text(stringResource(Res.string.onboarding_description))
}

@Composable
private fun WorkspaceStep(
    workspaceName: String,
    onWorkspaceNameChanged: (String) -> Unit,
) {
    Text(stringResource(Res.string.name_your_workspace))
    Text(stringResource(Res.string.onboarding_workspace_description))
    OutlinedTextField(
        value = workspaceName,
        onValueChange = onWorkspaceNameChanged,
        label = { Text(stringResource(Res.string.workspace_name)) },
        singleLine = true,
    )
}

@Composable
private fun OptionalToolsStep(
    zoteroEnabled: Boolean,
    onZoteroEnabledChanged: (Boolean) -> Unit,
) {
    Text(stringResource(Res.string.optional_tools))
    Text(stringResource(Res.string.optional_tools_description))
    OutlinedButton(onClick = { onZoteroEnabledChanged(!zoteroEnabled) }) {
        Text(
            stringResource(
                if (zoteroEnabled) Res.string.zotero_enabled else Res.string.keep_zotero_optional,
            ),
        )
    }
}
