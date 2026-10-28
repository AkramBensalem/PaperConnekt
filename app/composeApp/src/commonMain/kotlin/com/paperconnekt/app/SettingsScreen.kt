package com.paperconnekt.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.paperconnekt.domain.model.ThemePreference
import com.paperconnekt.secrets.SecretKey
import com.paperconnekt.secrets.SecretStore
import com.paperconnekt.storage.LocalResearchStore
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    store: LocalResearchStore,
    secretStore: SecretStore,
    paddingValues: PaddingValues,
) {
    val settings by store.settings.collectAsState()
    val scope = rememberCoroutineScope()
    var openAiKey by remember { mutableStateOf("") }
    var mistralKey by remember { mutableStateOf("") }
    var zoteroKey by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        openAiKey = secretStore.read(SecretKey.OPENAI).orEmpty()
        mistralKey = secretStore.read(SecretKey.MISTRAL).orEmpty()
        zoteroKey = secretStore.read(SecretKey.ZOTERO).orEmpty()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Settings")
        Text("Optional integrations stay local. No account or Supabase project is required.")

        Text("Workspace")
        Text("Workspace: ${settings.workspaceName}")
        Row {
            Text("Enable optional Zotero")
            Switch(
                checked = settings.zoteroEnabled,
                onCheckedChange = { enabled ->
                    scope.launch {
                        store.updateSettings { it.copy(zoteroEnabled = enabled) }
                    }
                },
            )
        }

        Text("Theme")
        ThemePreference.values().forEach { theme ->
            Row {
                RadioButton(
                    selected = settings.selectedTheme == theme,
                    onClick = {
                        scope.launch {
                            store.updateSettings { it.copy(selectedTheme = theme) }
                        }
                    },
                )
                Text(theme.name.lowercase().replaceFirstChar { it.uppercase() })
            }
        }

        Text("Provider keys")
        SecretField(
            label = "OpenAI API key",
            value = openAiKey,
            onValueChange = { openAiKey = it },
            onSave = {
                scope.launch {
                    secretStore.write(SecretKey.OPENAI, openAiKey)
                    statusMessage = "OpenAI key saved locally."
                }
            },
            onClear = {
                scope.launch {
                    secretStore.delete(SecretKey.OPENAI)
                    openAiKey = ""
                    statusMessage = "OpenAI key removed."
                }
            },
        )
        SecretField(
            label = "Mistral API key",
            value = mistralKey,
            onValueChange = { mistralKey = it },
            onSave = {
                scope.launch {
                    secretStore.write(SecretKey.MISTRAL, mistralKey)
                    statusMessage = "Mistral key saved locally."
                }
            },
            onClear = {
                scope.launch {
                    secretStore.delete(SecretKey.MISTRAL)
                    mistralKey = ""
                    statusMessage = "Mistral key removed."
                }
            },
        )
        SecretField(
            label = "Zotero API key",
            value = zoteroKey,
            onValueChange = { zoteroKey = it },
            onSave = {
                scope.launch {
                    secretStore.write(SecretKey.ZOTERO, zoteroKey)
                    statusMessage = "Zotero key saved locally."
                }
            },
            onClear = {
                scope.launch {
                    secretStore.delete(SecretKey.ZOTERO)
                    zoteroKey = ""
                    statusMessage = "Zotero key removed."
                }
            },
        )
        statusMessage?.let { Text(it) }
    }
}

@Composable
private fun SecretField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                enabled = value.isNotBlank(),
                onClick = onSave,
            ) {
                Text("Save")
            }
            TextButton(
                enabled = value.isNotEmpty(),
                onClick = onClear,
            ) {
                Text("Remove")
            }
        }
    }
}
