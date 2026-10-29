package com.paperconnekt.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalUriHandler
import com.paperconnekt.domain.model.ThemePreference
import com.paperconnekt.secrets.SecretKey
import com.paperconnekt.secrets.SecretStore
import com.paperconnekt.storage.LocalResearchStore
import kotlinx.coroutines.launch
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import com.paperconnekt.app.resources.Res
import com.paperconnekt.app.resources.settings
import com.paperconnekt.app.resources.zotero

@Composable
fun SettingsScreen(
    store: LocalResearchStore,
    secretStore: SecretStore,
    paddingValues: PaddingValues,
) {
    val settings by store.settings.collectAsState()
    val papers by store.papers.collectAsState()
    val sessions by store.assistantSessions.collectAsState()
    val scope = rememberCoroutineScope()
    var workspaceName by remember { mutableStateOf(settings.workspaceName) }
    var zoteroUserId by remember { mutableStateOf(settings.zoteroUserId) }
    var zoteroLibraryId by remember { mutableStateOf(settings.zoteroLibraryId) }
    var openAlexDiscoveryEnabled by remember { mutableStateOf(settings.openAlexDiscoveryEnabled) }
    var openAiDraft by remember { mutableStateOf("") }
    var mistralDraft by remember { mutableStateOf("") }
    var zoteroDraft by remember { mutableStateOf("") }
    var openAlexDraft by remember { mutableStateOf("") }
    var openAiConfigured by remember { mutableStateOf(false) }
    var mistralConfigured by remember { mutableStateOf(false) }
    var zoteroConfigured by remember { mutableStateOf(false) }
    var openAlexConfigured by remember { mutableStateOf(false) }
    var isLoadingSecrets by remember { mutableStateOf(true) }
    var secretLoadFailed by remember { mutableStateOf(false) }
    var reloadSecretsToken by remember { mutableStateOf(0) }
    var feedback by remember { mutableStateOf<SettingsFeedback?>(null) }
    val uriHandler = LocalUriHandler.current
    val httpClient = remember { HttpClient() }

    LaunchedEffect(secretStore, reloadSecretsToken) {
        isLoadingSecrets = true
        secretLoadFailed = false
        try {
            openAiConfigured = secretStore.read(SecretKey.OPENAI)?.isNotBlank() == true
            mistralConfigured = secretStore.read(SecretKey.MISTRAL)?.isNotBlank() == true
            zoteroConfigured = secretStore.read(SecretKey.ZOTERO)?.isNotBlank() == true
            openAlexConfigured = secretStore.read(SecretKey.OPENALEX)?.isNotBlank() == true
        } catch (exception: kotlinx.coroutines.CancellationException) {
            throw exception
        } catch (_: Exception) {
            secretLoadFailed = true
            feedback = SettingsFeedback.Error("Could not read local provider settings.")
        } finally {
            isLoadingSecrets = false
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        val compact = maxWidth < 760.dp
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = if (compact) 16.dp else 32.dp,
                end = if (compact) 16.dp else 32.dp,
                top = 24.dp,
                bottom = 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                SettingsHeader(
                    workspaceName = settings.workspaceName,
                    configuredProviders = listOf(
                        openAiConfigured,
                        mistralConfigured,
                        zoteroConfigured,
                        openAlexConfigured,
                    ).count { it },
                )
            }
            feedback?.let { currentFeedback ->
                item {
                    SettingsFeedbackCard(
                        feedback = currentFeedback,
                        onDismiss = { feedback = null },
                        onRetry = {
                            feedback = null
                            reloadSecretsToken += 1
                        },
                    )
                }
            }
            item {
                SettingsSection(title = "Workspace") {
                    SettingsCard {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                "Your workspace stays on this device. Rename it to make the local research context yours.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            OutlinedTextField(
                                modifier = Modifier.fillMaxWidth(),
                                value = workspaceName,
                                onValueChange = { workspaceName = it },
                                label = { Text("Workspace name") },
                                singleLine = true,
                            )
                            Button(
                                enabled = workspaceName.trim().isNotEmpty() &&
                                    workspaceName.trim() != settings.workspaceName,
                                onClick = {
                                    scope.launch {
                                        store.updateSettings {
                                            it.copy(workspaceName = workspaceName.trim())
                                        }
                                        feedback = SettingsFeedback.Success("Workspace name saved locally.")
                                    }
                                },
                            ) {
                                Text("Save workspace")
                            }
                            Divider()
                            Text("Appearance", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Choose how PaperConnekt follows your device or stays light/dark.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            ThemePreference.values().forEach { preference ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .semantics {
                                            contentDescription = "${preference.label} appearance"
                                        },
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    RadioButton(
                                        selected = settings.selectedTheme == preference,
                                        onClick = {
                                            scope.launch {
                                                store.updateSettings {
                                                    it.copy(selectedTheme = preference)
                                                }
                                            }
                                        },
                                    )
                                    Column {
                                        Text(preference.label)
                                        Text(
                                            preference.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item {
                SettingsSection(title = "Assistant providers") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "Connect only the providers you want to use. Keys are stored locally through the Android Keystore or desktop preferences boundary; they are not synced or logged. A key is validated by its provider when a connected feature makes a request.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (isLoadingSecrets) {
                            SettingsCard {
                                Text("Checking local provider readiness…")
                            }
                        } else if (secretLoadFailed) {
                            SettingsCard {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Provider readiness is unavailable.")
                                    Text(
                                        "Your existing keys were not changed. Retry to check the local secret store again.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    TextButton(onClick = { reloadSecretsToken += 1 }) {
                                        Text("Retry")
                                    }
                                }
                            }
                        } else {
                            ProviderCredentialCard(
                                providerName = "OpenAI",
                                description = "Store an OpenAI API key locally for connected AI features.",
                                configured = openAiConfigured,
                                draft = openAiDraft,
                                onDraftChange = { openAiDraft = it },
                                onSave = {
                                    saveSecret(
                                        scope = scope,
                                        secretStore = secretStore,
                                        key = SecretKey.OPENAI,
                                        draft = openAiDraft,
                                        label = "OpenAI",
                                        onSuccess = {
                                            openAiConfigured = true
                                            openAiDraft = ""
                                            feedback = SettingsFeedback.Success("OpenAI key saved locally.")
                                        },
                                        onFailure = {
                                            feedback = SettingsFeedback.Error(it)
                                        },
                                    )
                                },
                                onClear = {
                                    deleteSecret(
                                        scope = scope,
                                        secretStore = secretStore,
                                        key = SecretKey.OPENAI,
                                        label = "OpenAI",
                                        onSuccess = {
                                            openAiConfigured = false
                                            feedback = SettingsFeedback.Success("OpenAI was cleared locally.")
                                        },
                                        onFailure = {
                                            feedback = SettingsFeedback.Error(it)
                                        },
                                        onValidate = {
                                            validateSecret(scope, secretStore, SecretKey.OPENAI, "OpenAI", httpClient) {
                                                feedback = SettingsFeedback.Success("OpenAI key is valid.")
                                            } { feedback = SettingsFeedback.Error(it) }
                                        },
                                        onOpenKeyPage = { uriHandler.openUri("https://platform.openai.com/api-keys") },
                                    )
                                },
                            )
                            ProviderCredentialCard(
                                providerName = "Mistral",
                                description = "Store a Mistral API key locally for OCR and connected AI features.",
                                configured = mistralConfigured,
                                draft = mistralDraft,
                                onDraftChange = { mistralDraft = it },
                                onSave = {
                                    saveSecret(
                                        scope = scope,
                                        secretStore = secretStore,
                                        key = SecretKey.MISTRAL,
                                        draft = mistralDraft,
                                        label = "Mistral",
                                        onSuccess = {
                                            mistralConfigured = true
                                            mistralDraft = ""
                                            feedback = SettingsFeedback.Success("Mistral key saved locally.")
                                        },
                                        onFailure = {
                                            feedback = SettingsFeedback.Error(it)
                                        },
                                    )
                                },
                                onClear = {
                                    deleteSecret(
                                        scope = scope,
                                        secretStore = secretStore,
                                        key = SecretKey.MISTRAL,
                                        label = "Mistral",
                                        onSuccess = {
                                            mistralConfigured = false
                                            feedback = SettingsFeedback.Success("Mistral was cleared locally.")
                                        },
                                        onFailure = {
                                            feedback = SettingsFeedback.Error(it)
                                        },
                                        onValidate = {
                                            validateSecret(scope, secretStore, SecretKey.MISTRAL, "Mistral", httpClient) {
                                                feedback = SettingsFeedback.Success("Mistral key is valid.")
                                            } { feedback = SettingsFeedback.Error(it) }
                                        },
                                        onOpenKeyPage = { uriHandler.openUri("https://console.mistral.ai/api-keys") },
                                    )
                                },
                            )
                        }
                    }
                }
            }
            item {
                SettingsSection(title = "Zotero library") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SettingsCard {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Enable optional Zotero", style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            "Use Zotero only when you choose to connect it. It is not required for local research.",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                    Switch(
                                        checked = settings.zoteroEnabled,
                                        onCheckedChange = { enabled ->
                                            scope.launch {
                                                store.updateSettings { it.copy(zoteroEnabled = enabled) }
                                            }
                                        },
                                        modifier = Modifier.semantics {
                                            contentDescription = "Enable optional Zotero"
                                        },
                                    )
                                }
                                if (settings.zoteroEnabled) {
                                    OutlinedTextField(
                                        modifier = Modifier.fillMaxWidth(),
                                        value = zoteroUserId,
                                        onValueChange = { zoteroUserId = it },
                                        label = { Text("Zotero user ID") },
                                        singleLine = true,
                                    )
                                    OutlinedTextField(
                                        modifier = Modifier.fillMaxWidth(),
                                        value = zoteroLibraryId,
                                        onValueChange = { zoteroLibraryId = it },
                                        label = { Text("Zotero library ID (optional)") },
                                        singleLine = true,
                                    )
                                    Button(
                                        enabled = zoteroUserId.trim() != settings.zoteroUserId ||
                                            zoteroLibraryId.trim() != settings.zoteroLibraryId,
                                        onClick = {
                                            scope.launch {
                                                store.updateSettings {
                                                    it.copy(
                                                        zoteroUserId = zoteroUserId.trim(),
                                                        zoteroLibraryId = zoteroLibraryId.trim(),
                                                    )
                                                }
                                                feedback = SettingsFeedback.Success("Zotero identifiers saved locally.")
                                            }
                                        },
                                    ) {
                                        Text("Save Zotero details")
                                    }
                                }
                            }
                        }
                        ProviderCredentialCard(
                            providerName = "Zotero",
                            description = "Store the optional Zotero API key on this device.",
                            configured = zoteroConfigured,
                            draft = zoteroDraft,
                            providerIcon = Res.drawable.zotero,
                            onDraftChange = { zoteroDraft = it },
                            onSave = {
                                saveSecret(
                                    scope = scope,
                                    secretStore = secretStore,
                                    key = SecretKey.ZOTERO,
                                    draft = zoteroDraft,
                                    label = "Zotero",
                                    onSuccess = {
                                        zoteroConfigured = true
                                        zoteroDraft = ""
                                        feedback = SettingsFeedback.Success("Zotero key saved locally.")
                                    },
                                    onFailure = {
                                        feedback = SettingsFeedback.Error(it)
                                    },
                                )
                            },
                            onClear = {
                                deleteSecret(
                                    scope = scope,
                                    secretStore = secretStore,
                                    key = SecretKey.ZOTERO,
                                    label = "Zotero",
                                    onSuccess = {
                                        zoteroConfigured = false
                                        feedback = SettingsFeedback.Success("Zotero was cleared locally.")
                                    },
                                    onFailure = {
                                        feedback = SettingsFeedback.Error(it)
                                    },
                                    onValidate = {
                                        validateSecret(scope, secretStore, SecretKey.ZOTERO, "Zotero", httpClient) {
                                            feedback = SettingsFeedback.Success("Zotero key is valid.")
                                        } { feedback = SettingsFeedback.Error(it) }
                                    },
                                    onOpenKeyPage = { uriHandler.openUri("https://www.zotero.org/settings/keys") },
                                )
                            },
                        )
                    }
                }
            }
            item {
                SettingsSection(title = "OpenAlex") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "OpenAlex works without a key at a lower rate limit. Add your personal key for higher limits; it is stored only on this device.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        ProviderCredentialCard(
                            providerName = "OpenAlex",
                            description = "Optional personal key for OpenAlex discovery and graph requests.",
                            configured = openAlexConfigured,
                            draft = openAlexDraft,
                            onDraftChange = { openAlexDraft = it },
                            onSave = {
                                saveSecret(scope, secretStore, SecretKey.OPENALEX, openAlexDraft, "OpenAlex",
                                    onSuccess = {
                                        openAlexConfigured = true
                                        openAlexDraft = ""
                                        feedback = SettingsFeedback.Success("OpenAlex key saved locally.")
                                    },
                                    onFailure = { feedback = SettingsFeedback.Error(it) })
                            },
                            onClear = {
                                deleteSecret(scope, secretStore, SecretKey.OPENALEX, "OpenAlex",
                                    onSuccess = {
                                        openAlexConfigured = false
                                        feedback = SettingsFeedback.Success("OpenAlex was cleared locally.")
                                    },
                                    onFailure = { feedback = SettingsFeedback.Error(it) })
                            },
                            onValidate = {
                                validateSecret(scope, secretStore, SecretKey.OPENALEX, "OpenAlex", httpClient) {
                                    feedback = SettingsFeedback.Success("OpenAlex key is valid.")
                                } { feedback = SettingsFeedback.Error(it) }
                            },
                            onOpenKeyPage = {
                                uriHandler.openUri(
                                    "https://docs.openalex.org/how-to-use-the-api/rate-limits-and-authentication",
                                )
                            },
                        )
                    }
                }
            }
            item {
                SettingsSection(title = "Discovery") {
                    SettingsCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Optional OpenAlex discovery", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "When enabled, dashboard searches may query OpenAlex. Local papers remain available offline.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = openAlexDiscoveryEnabled,
                                onCheckedChange = { enabled ->
                                    openAlexDiscoveryEnabled = enabled
                                    scope.launch {
                                        store.updateSettings {
                                            it.copy(openAlexDiscoveryEnabled = enabled)
                                        }
                                    }
                                },
                                modifier = Modifier.semantics {
                                    contentDescription = "Enable optional OpenAlex discovery"
                                },
                            )
                        }
                    }
                }
            }
            item {
                SettingsSection(title = "Readiness") {
                    SettingsCard {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Assistant usage", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${sessions.size} local conversation${if (sessions.size == 1) "" else "s"} · ${papers.size} saved paper${if (papers.size == 1) "" else "s"}",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                when {
                                    openAiConfigured || mistralConfigured ->
                                        "A provider key is saved locally. The provider confirms whether it is valid when a connected feature makes a request."
                                    else ->
                                        "Add an OpenAI or Mistral key to make provider-backed answers available."
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ReadinessChip("OpenAI", openAiConfigured)
                                ReadinessChip("Mistral", mistralConfigured)
                                ReadinessChip("Zotero", zoteroConfigured && settings.zoteroEnabled)
                                ReadinessChip("OpenAlex", openAlexConfigured)
                            }
                        }
                    }
                }
            }
        }
    }
}

private sealed interface SettingsFeedback {
    data class Success(val message: String) : SettingsFeedback
    data class Error(val message: String) : SettingsFeedback
}

@Composable
private fun SettingsHeader(
    workspaceName: String,
    configuredProviders: Int,
) {
    WorkspaceHeader(
        title = "Settings",
        subtitle = "Settings for $workspaceName. Your research, your preferences, your device.",
        icon = Res.drawable.settings,
        summaries = listOf(
            "Local-first workspace",
            "$configuredProviders provider key${if (configuredProviders == 1) "" else "s"} saved",
        ),
    )
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        content()
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            content()
        }
    }
}

@Composable
private fun SettingsFeedbackCard(
    feedback: SettingsFeedback,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
) {
    val isError = feedback is SettingsFeedback.Error
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isError) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.primaryContainer
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = when (feedback) {
                    is SettingsFeedback.Success -> feedback.message
                    is SettingsFeedback.Error -> feedback.message
                },
                modifier = Modifier.weight(1f),
                color = if (isError) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                },
            )
            if (isError) {
                TextButton(onClick = onRetry) { Text("Retry") }
            }
            TextButton(onClick = onDismiss) { Text("Dismiss") }
        }
    }
}

@Composable
private fun ReadinessChip(
    label: String,
    ready: Boolean,
) {
    FilterChip(
        selected = ready,
        onClick = {},
        enabled = false,
        label = { Text(if (ready) "$label key saved" else "$label not set") },
    )
}

private fun saveSecret(
    scope: kotlinx.coroutines.CoroutineScope,
    secretStore: SecretStore,
    key: SecretKey,
    draft: String,
    label: String,
    onSuccess: () -> Unit,
    onFailure: (String) -> Unit,
) {
    val value = draft.trim()
    if (value.isEmpty()) return
    scope.launch {
        try {
            secretStore.write(key, value)
            onSuccess()
        } catch (exception: kotlinx.coroutines.CancellationException) {
            throw exception
        } catch (_: Exception) {
            onFailure("Could not save the $label key locally. Try again.")
        }
    }
}

private fun deleteSecret(
    scope: kotlinx.coroutines.CoroutineScope,
    secretStore: SecretStore,
    key: SecretKey,
    label: String,
    onSuccess: () -> Unit,
    onFailure: (String) -> Unit,
) {
    scope.launch {
        try {
            secretStore.delete(key)
            onSuccess()
        } catch (exception: kotlinx.coroutines.CancellationException) {
            throw exception
        } catch (_: Exception) {
            onFailure("Could not clear the $label key locally. Try again.")
        }

        private fun validateSecret(
            scope: kotlinx.coroutines.CoroutineScope,
            secretStore: SecretStore,
            key: SecretKey,
            label: String,
            client: HttpClient,
            onSuccess: () -> Unit,
            onFailure: (String) -> Unit,
        ) {
            scope.launch {
                try {
                    val value = secretStore.read(key)?.trim().orEmpty()
                    if (value.isEmpty()) {
                        onFailure("No $label key is saved locally.")
                        return@launch
                    }
                    val response = when (key) {
                        SecretKey.OPENAI -> client.get("https://api.openai.com/v1/models") {
                            header("Authorization", "Bearer $value")
                        }
                        SecretKey.MISTRAL -> client.get("https://api.mistral.ai/v1/models") {
                            header("Authorization", "Bearer $value")
                        }
                        SecretKey.ZOTERO -> client.get("https://api.zotero.org/keys/$value")
                        SecretKey.OPENALEX -> client.get("https://api.openalex.org/works") {
                            parameter("api_key", value)
                            parameter("per-page", 1)
                        }
                    }
                    if (response.status.value in 200..299) onSuccess()
                    else if (response.status.value == 401 || response.status.value == 403) {
                        onFailure("$label rejected the key (invalid or expired).")
                    } else {
                        onFailure("$label key check returned HTTP ${response.status.value}.")
                    }
                } catch (exception: kotlinx.coroutines.CancellationException) {
                    throw exception
                } catch (_: Exception) {
                    onFailure("Could not reach $label to check the key.")
                }
            }
        }
    }
}

private val ThemePreference.label: String
    get() = when (this) {
        ThemePreference.SYSTEM -> "System default"
        ThemePreference.LIGHT -> "Light"
        ThemePreference.DARK -> "Dark"
    }

private val ThemePreference.description: String
    get() = when (this) {
        ThemePreference.SYSTEM -> "Follow the device appearance"
        ThemePreference.LIGHT -> "Use the light PaperConnekt palette"
        ThemePreference.DARK -> "Use the dark PaperConnekt palette"
    }
