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
import com.akram.bensalem.paper.connekt.feature.mcp.LocalMcpHost
import com.paperconnekt.domain.model.LanguagePreference
import com.paperconnekt.domain.model.ThemePreference
import com.paperconnekt.secrets.SecretKey
import com.paperconnekt.secrets.SecretStore
import com.paperconnekt.storage.LocalResearchStore
import kotlinx.coroutines.launch
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.parameter
import com.paperconnekt.app.resources.Res
import com.paperconnekt.app.resources.mistral
import com.paperconnekt.app.resources.openai
import com.paperconnekt.app.resources.openalex
import com.paperconnekt.app.resources.settings
import com.paperconnekt.app.resources.zotero
import com.paperconnekt.app.resources.*
import org.jetbrains.compose.resources.stringResource
import com.paperconnekt.app.resources.appearance
import com.paperconnekt.app.resources.appearance_description
import com.paperconnekt.app.resources.appearance_for
import com.paperconnekt.app.resources.arabic
import com.paperconnekt.app.resources.assistant_providers
import com.paperconnekt.app.resources.assistant_usage
import com.paperconnekt.app.resources.checking_provider_readiness
import com.paperconnekt.app.resources.dark
import com.paperconnekt.app.resources.discovery
import com.paperconnekt.app.resources.dismiss
import com.paperconnekt.app.resources.enable_zotero
import com.paperconnekt.app.resources.english
import com.paperconnekt.app.resources.follow_device_appearance
import com.paperconnekt.app.resources.french
import com.paperconnekt.app.resources.keys_unchanged_retry
import com.paperconnekt.app.resources.language
import com.paperconnekt.app.resources.language_description
import com.paperconnekt.app.resources.language_for
import com.paperconnekt.app.resources.light
import com.paperconnekt.app.resources.local_first_workspace
import com.paperconnekt.app.resources.optional_openalex_discovery
import com.paperconnekt.app.resources.paper_search
import com.paperconnekt.app.resources.provider_description
import com.paperconnekt.app.resources.provider_readiness_unavailable
import com.paperconnekt.app.resources.readiness
import com.paperconnekt.app.resources.retry
import com.paperconnekt.app.resources.save_workspace
import com.paperconnekt.app.resources.save_zotero_details
import com.paperconnekt.app.resources.search_papers_by_meaning
import com.paperconnekt.app.resources.settings_workspace_subtitle
import com.paperconnekt.app.resources.system_default
import com.paperconnekt.app.resources.use_arabic
import com.paperconnekt.app.resources.use_dark_palette
import com.paperconnekt.app.resources.use_english
import com.paperconnekt.app.resources.use_french
import com.paperconnekt.app.resources.use_light_palette
import com.paperconnekt.app.resources.workspace
import com.paperconnekt.app.resources.openai_provider_description
import com.paperconnekt.app.resources.mistral_provider_description
import com.paperconnekt.app.resources.zotero_provider_description
import com.paperconnekt.app.resources.openalex_provider_description
import com.paperconnekt.app.resources.workspace_description
import com.paperconnekt.app.resources.workspace_name
import com.paperconnekt.app.resources.workspace_saved
import com.paperconnekt.app.resources.zotero_library
import com.paperconnekt.app.resources.zotero_library_id_optional
import com.paperconnekt.app.resources.zotero_user_id
import com.paperconnekt.app.resources.zotero_local_description
import com.paperconnekt.app.resources.openalex_rate_limit_description
import com.paperconnekt.app.resources.openalex_discovery_description
import com.paperconnekt.app.resources.semantic_search_description
import com.paperconnekt.app.resources.provider_key_saved_message
import com.paperconnekt.app.resources.provider_key_needed_message
import com.paperconnekt.app.resources.configured_providers_summary
import com.paperconnekt.app.resources.provider_key_saved_locally
import com.paperconnekt.app.resources.provider_cleared_locally
import com.paperconnekt.app.resources.provider_key_valid
import com.paperconnekt.app.resources.zotero_identifiers_saved
import com.paperconnekt.app.resources.provider_key_saved_chip
import com.paperconnekt.app.resources.provider_not_set_chip
import com.paperconnekt.app.resources.provider_rejected_not_saved
import com.paperconnekt.app.resources.provider_check_failed_not_saved
import com.paperconnekt.app.resources.provider_unreachable_not_saved
import com.paperconnekt.app.resources.provider_save_failed
import com.paperconnekt.app.resources.provider_clear_failed
import com.paperconnekt.app.resources.provider_no_key
import com.paperconnekt.app.resources.provider_rejected
import com.paperconnekt.app.resources.provider_check_failed

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
    var semanticSearchEnabled by remember { mutableStateOf(settings.semanticSearchEnabled) }
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
    var providerFeedback by remember { mutableStateOf<ProviderFeedback?>(null) }
    val uriHandler = LocalUriHandler.current
    val httpClient = remember { HttpClient() }
    val mcpHost = LocalMcpHost.current
    val workspaceSavedMessage = stringResource(Res.string.workspace_saved)
    val enableZoteroDescription = stringResource(Res.string.enable_zotero)
    val optionalOpenAlexDescription = stringResource(Res.string.optional_openalex_discovery)
    val searchByMeaningDescription = stringResource(Res.string.search_papers_by_meaning)
    val providerSettingsReadError = stringResource(Res.string.provider_readiness_unavailable)
    val openAiName = stringResource(Res.string.openai)
    val mistralName = stringResource(Res.string.mistral)
    val zoteroName = stringResource(Res.string.zotero)
    val openAlexName = stringResource(Res.string.openalex)
    val openAiSavedMessage = stringResource(Res.string.provider_key_saved_locally, openAiName)
    val openAiClearedMessage = stringResource(Res.string.provider_cleared_locally, openAiName)
    val openAiValidMessage = stringResource(Res.string.provider_key_valid, openAiName)
    val mistralSavedMessage = stringResource(Res.string.provider_key_saved_locally, mistralName)
    val mistralClearedMessage = stringResource(Res.string.provider_cleared_locally, mistralName)
    val mistralValidMessage = stringResource(Res.string.provider_key_valid, mistralName)
    val zoteroSavedMessage = stringResource(Res.string.provider_key_saved_locally, zoteroName)
    val zoteroClearedMessage = stringResource(Res.string.provider_cleared_locally, zoteroName)
    val zoteroValidMessage = stringResource(Res.string.provider_key_valid, zoteroName)
    val openAlexSavedMessage = stringResource(Res.string.provider_key_saved_locally, openAlexName)
    val openAlexClearedMessage = stringResource(Res.string.provider_cleared_locally, openAlexName)
    val openAlexValidMessage = stringResource(Res.string.provider_key_valid, openAlexName)
    val zoteroIdentifiersSavedMessage = stringResource(Res.string.zotero_identifiers_saved)
    val openAiErrors = providerErrorMessages(openAiName)
    val mistralErrors = providerErrorMessages(mistralName)
    val zoteroErrors = providerErrorMessages(zoteroName)
    val openAlexErrors = providerErrorMessages(openAlexName)

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
            feedback = SettingsFeedback.Error(providerSettingsReadError)
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
        val compact = maxWidth < 1120.dp
        val narrow = maxWidth < 420.dp
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = if (narrow) 12.dp else if (compact) 16.dp else 32.dp,
                end = if (narrow) 12.dp else if (compact) 16.dp else 32.dp,
                top = if (compact) 16.dp else 24.dp,
                bottom = if (compact) 96.dp else 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(if (compact) 14.dp else 18.dp),
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
                SettingsSection(title = stringResource(Res.string.workspace)) {
                    SettingsCard {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                stringResource(Res.string.workspace_description),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            OutlinedTextField(
                                modifier = Modifier.fillMaxWidth(),
                                value = workspaceName,
                                onValueChange = { workspaceName = it },
                                label = { Text(stringResource(Res.string.workspace_name)) },
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
                                        feedback = SettingsFeedback.Success(workspaceSavedMessage)
                                    }
                                },
                            ) {
                                Text(stringResource(Res.string.save_workspace))
                            }
                            Divider()
                            Text(stringResource(Res.string.appearance), style = MaterialTheme.typography.titleMedium)
                            Text(
                                stringResource(Res.string.appearance_description),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            ThemePreference.values().forEach { preference ->
                                val label = preference.localizedLabel()
                                val accessibilityLabel = stringResource(Res.string.appearance_for, label)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .semantics {
                                            contentDescription = accessibilityLabel
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
                                        Text(label)
                                        Text(
                                            preference.localizedDescription(),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                            Divider()
                            Text(stringResource(Res.string.language), style = MaterialTheme.typography.titleMedium)
                            Text(
                                stringResource(Res.string.language_description),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            LanguagePreference.values().forEach { preference ->
                                val label = preference.localizedLabel()
                                val accessibilityLabel = stringResource(Res.string.language_for, label)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .semantics {
                                            contentDescription = accessibilityLabel
                                        },
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    RadioButton(
                                        selected = settings.selectedLanguage == preference,
                                        onClick = {
                                            scope.launch {
                                                store.updateSettings {
                                                    it.copy(selectedLanguage = preference)
                                                }
                                            }
                                        },
                                    )
                                    Column {
                                        Text(label)
                                        Text(
                                            preference.localizedDescription(),
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
                SettingsSection(title = stringResource(Res.string.assistant_providers)) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            stringResource(Res.string.provider_description),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (isLoadingSecrets) {
                            SettingsCard {
                                Text(stringResource(Res.string.checking_provider_readiness))
                            }
                        } else if (secretLoadFailed) {
                            SettingsCard {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(stringResource(Res.string.provider_readiness_unavailable))
                                    Text(
                                        stringResource(Res.string.keys_unchanged_retry),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    TextButton(onClick = { reloadSecretsToken += 1 }) {
                                        Text(stringResource(Res.string.retry))
                                    }
                                }
                            }
                        } else {
                            ProviderCredentialCard(
                                providerName = stringResource(Res.string.openai),
                                description = stringResource(Res.string.openai_provider_description),
                                providerIcon = Res.drawable.openai,
                                providerIconBackground = MaterialTheme.colorScheme.onSurface,
                                feedback = providerFeedback?.takeIf { it.provider == "OpenAI" }?.message,
                                feedbackIsError = providerFeedback?.takeIf { it.provider == "OpenAI" }?.isError ?: true,
                                configured = openAiConfigured,
                                draft = openAiDraft,
                                onDraftChange = { openAiDraft = it },
                                onSave = {
                                    validateAndSaveSecret(
                                        scope = scope,
                                        secretStore = secretStore,
                                        key = SecretKey.OPENAI,
                                        draft = openAiDraft,
                                        label = "OpenAI",
                                        errors = openAiErrors,
                                        client = httpClient,
                                        onSuccess = {
                                            openAiConfigured = true
                                            openAiDraft = ""
                                            providerFeedback = ProviderFeedback("OpenAI", openAiSavedMessage, false)
                                        },
                                        onFailure = {
                                            providerFeedback = ProviderFeedback("OpenAI", it, true)
                                        },
                                    )
                                },
                                onClear = {
                                    deleteSecret(
                                        scope = scope,
                                        secretStore = secretStore,
                                        key = SecretKey.OPENAI,
                                        label = "OpenAI",
                                        errors = openAiErrors,
                                        onSuccess = {
                                            openAiConfigured = false
                                            providerFeedback = ProviderFeedback("OpenAI", openAiClearedMessage, false)
                                        },
                                        onFailure = {
                                            providerFeedback = ProviderFeedback("OpenAI", it, true)
                                        },
                                    )
                                },
                                onValidate = {
                                    validateSecret(
                                        scope, secretStore, SecretKey.OPENAI, "OpenAI", openAiErrors, httpClient,
                                        onSuccess = { providerFeedback = ProviderFeedback("OpenAI", openAiValidMessage, false) },
                                        onFailure = { providerFeedback = ProviderFeedback("OpenAI", it, true) },
                                    )
                                },
                                onOpenKeyPage = { uriHandler.openUri("https://platform.openai.com/api-keys") },
                            )
                            ProviderCredentialCard(
                                providerName = stringResource(Res.string.mistral),
                                description = stringResource(Res.string.mistral_provider_description),
                                providerIcon = Res.drawable.mistral,
                                feedback = providerFeedback?.takeIf { it.provider == "Mistral" }?.message,
                                feedbackIsError = providerFeedback?.takeIf { it.provider == "Mistral" }?.isError ?: true,
                                configured = mistralConfigured,
                                draft = mistralDraft,
                                onDraftChange = { mistralDraft = it },
                                onSave = {
                                    validateAndSaveSecret(
                                        scope = scope,
                                        secretStore = secretStore,
                                        key = SecretKey.MISTRAL,
                                        draft = mistralDraft,
                                        label = "Mistral",
                                        errors = mistralErrors,
                                        client = httpClient,
                                        onSuccess = {
                                            mistralConfigured = true
                                            mistralDraft = ""
                                            providerFeedback = ProviderFeedback("Mistral", mistralSavedMessage, false)
                                        },
                                        onFailure = {
                                            providerFeedback = ProviderFeedback("Mistral", it, true)
                                        },
                                    )
                                },
                                onClear = {
                                    deleteSecret(
                                        scope = scope,
                                        secretStore = secretStore,
                                        key = SecretKey.MISTRAL,
                                        label = "Mistral",
                                        errors = mistralErrors,
                                        onSuccess = {
                                            mistralConfigured = false
                                            providerFeedback = ProviderFeedback("Mistral", mistralClearedMessage, false)
                                        },
                                        onFailure = {
                                            providerFeedback = ProviderFeedback("Mistral", it, true)
                                        },
                                    )
                                },
                                onValidate = {
                                    validateSecret(
                                        scope, secretStore, SecretKey.MISTRAL, "Mistral", mistralErrors, httpClient,
                                        onSuccess = { providerFeedback = ProviderFeedback("Mistral", mistralValidMessage, false) },
                                        onFailure = { providerFeedback = ProviderFeedback("Mistral", it, true) },
                                    )
                                },
                                onOpenKeyPage = { uriHandler.openUri("https://console.mistral.ai/api-keys") },
                            )
                        }
                    }
                }
            }
            item {
                SettingsSection(title = stringResource(Res.string.zotero_library)) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SettingsCard {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(stringResource(Res.string.enable_zotero), style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            stringResource(Res.string.zotero_local_description),
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
                                            contentDescription = enableZoteroDescription
                                        },
                                    )
                                }
                                if (settings.zoteroEnabled) {
                                    OutlinedTextField(
                                        modifier = Modifier.fillMaxWidth(),
                                        value = zoteroUserId,
                                        onValueChange = { zoteroUserId = it },
                                        label = { Text(stringResource(Res.string.zotero_user_id)) },
                                        singleLine = true,
                                    )
                                    OutlinedTextField(
                                        modifier = Modifier.fillMaxWidth(),
                                        value = zoteroLibraryId,
                                        onValueChange = { zoteroLibraryId = it },
                                        label = { Text(stringResource(Res.string.zotero_library_id_optional)) },
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
                                                feedback = SettingsFeedback.Success(zoteroIdentifiersSavedMessage)
                                            }
                                        },
                                    ) {
                                        Text(stringResource(Res.string.save_zotero_details))
                                    }
                                }
                            }
                        }
                        ProviderCredentialCard(
                            providerName = stringResource(Res.string.zotero),
                            description = stringResource(Res.string.zotero_provider_description),
                            feedback = providerFeedback?.takeIf { it.provider == "Zotero" }?.message,
                            feedbackIsError = providerFeedback?.takeIf { it.provider == "Zotero" }?.isError ?: true,
                            configured = zoteroConfigured,
                            draft = zoteroDraft,
                            providerIcon = Res.drawable.zotero,
                            onDraftChange = { zoteroDraft = it },
                            onSave = {
                                validateAndSaveSecret(
                                    scope = scope,
                                    secretStore = secretStore,
                                    key = SecretKey.ZOTERO,
                                    draft = zoteroDraft,
                                    label = "Zotero",
                                    errors = zoteroErrors,
                                    client = httpClient,
                                    onSuccess = {
                                        zoteroConfigured = true
                                        zoteroDraft = ""
                                        providerFeedback = ProviderFeedback("Zotero", zoteroSavedMessage, false)
                                    },
                                    onFailure = {
                                        providerFeedback = ProviderFeedback("Zotero", it, true)
                                    },
                                )
                            },
                            onClear = {
                                deleteSecret(
                                    scope = scope,
                                    secretStore = secretStore,
                                    key = SecretKey.ZOTERO,
                                    label = "Zotero",
                                    errors = zoteroErrors,
                                    onSuccess = {
                                        zoteroConfigured = false
                                        providerFeedback = ProviderFeedback("Zotero", zoteroClearedMessage, false)
                                    },
                                    onFailure = {
                                        providerFeedback = ProviderFeedback("Zotero", it, true)
                                    },
                                )
                            },
                            onValidate = {
                                validateSecret(
                                    scope, secretStore, SecretKey.ZOTERO, "Zotero", zoteroErrors, httpClient,
                                    onSuccess = { providerFeedback = ProviderFeedback("Zotero", zoteroValidMessage, false) },
                                    onFailure = { providerFeedback = ProviderFeedback("Zotero", it, true) },
                                )
                            },
                            onOpenKeyPage = { uriHandler.openUri("https://www.zotero.org/settings/keys") },
                        )
                    }
                }
            }
            item {
                SettingsSection(title = stringResource(Res.string.openalex)) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            stringResource(Res.string.openalex_rate_limit_description),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        ProviderCredentialCard(
                            providerName = stringResource(Res.string.openalex),
                            description = stringResource(Res.string.openalex_provider_description),
                            providerIcon = Res.drawable.openalex,
                            feedback = providerFeedback?.takeIf { it.provider == "OpenAlex" }?.message,
                            feedbackIsError = providerFeedback?.takeIf { it.provider == "OpenAlex" }?.isError ?: true,
                            configured = openAlexConfigured,
                            draft = openAlexDraft,
                            onDraftChange = { openAlexDraft = it },
                            onSave = {
                                validateAndSaveSecret(scope, secretStore, SecretKey.OPENALEX, openAlexDraft, "OpenAlex", openAlexErrors, httpClient,
                                    onSuccess = {
                                        openAlexConfigured = true
                                        openAlexDraft = ""
                                        providerFeedback = ProviderFeedback("OpenAlex", openAlexSavedMessage, false)
                                    },
                                    onFailure = { providerFeedback = ProviderFeedback("OpenAlex", it, true) })
                            },
                            onClear = {
                                deleteSecret(scope, secretStore, SecretKey.OPENALEX, "OpenAlex", openAlexErrors,
                                    onSuccess = {
                                        openAlexConfigured = false
                                        providerFeedback = ProviderFeedback("OpenAlex", openAlexClearedMessage, false)
                                    },
                                    onFailure = { providerFeedback = ProviderFeedback("OpenAlex", it, true) })
                            },
                            onValidate = {
                                validateSecret(
                                    scope, secretStore, SecretKey.OPENALEX, "OpenAlex", openAlexErrors, httpClient,
                                    onSuccess = { providerFeedback = ProviderFeedback("OpenAlex", openAlexValidMessage, false) },
                                    onFailure = { providerFeedback = ProviderFeedback("OpenAlex", it, true) },
                                )
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
                SettingsSection(title = stringResource(Res.string.discovery)) {
                    SettingsCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(Res.string.optional_openalex_discovery), style = MaterialTheme.typography.titleMedium)
                                Text(
                                    stringResource(Res.string.openalex_discovery_description),
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
                                    contentDescription = optionalOpenAlexDescription
                                },
                            )
                        }
                    }
                }
            }
            item {
                SettingsSection(title = stringResource(Res.string.paper_search)) {
                    SettingsCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(Res.string.search_papers_by_meaning), style = MaterialTheme.typography.titleMedium)
                                Text(
                                    stringResource(Res.string.semantic_search_description),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = semanticSearchEnabled,
                                onCheckedChange = { enabled ->
                                    semanticSearchEnabled = enabled
                                    scope.launch {
                                        store.updateSettings {
                                            it.copy(semanticSearchEnabled = enabled)
                                        }
                                    }
                                },
                                modifier = Modifier.semantics {
                                    contentDescription = searchByMeaningDescription
                                },
                            )
                        }
                    }
                }
            }
            mcpHost?.let { host ->
                item { McpSettingsSection(host, store, secretStore, settings) }
            }
            item {
                SettingsSection(title = stringResource(Res.string.readiness)) {
                    SettingsCard {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(Res.string.assistant_usage), style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${sessions.size} local conversation${if (sessions.size == 1) "" else "s"} · ${papers.size} saved paper${if (papers.size == 1) "" else "s"}",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                when {
                                    openAiConfigured || mistralConfigured ->
                                        stringResource(Res.string.provider_key_saved_message)
                                    else ->
                                        stringResource(Res.string.provider_key_needed_message)
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            val readiness = listOf(
                                "OpenAI" to openAiConfigured,
                                "Mistral" to mistralConfigured,
                                "Zotero" to (zoteroConfigured && settings.zoteroEnabled),
                                "OpenAlex" to openAlexConfigured,
                            )
                            BoxWithConstraints(Modifier.fillMaxWidth()) {
                                val columns = if (maxWidth < 360.dp) 1 else 2
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    readiness.chunked(columns).forEach { rowItems ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            rowItems.forEach { (label, ready) ->
                                                ReadinessChip(
                                                    label = label,
                                                    ready = ready,
                                                    modifier = Modifier.weight(1f),
                                                )
                                            }
                                            repeat(columns - rowItems.size) {
                                                androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
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

private data class ProviderFeedback(
    val provider: String,
    val message: String,
    val isError: Boolean,
)

private data class ProviderErrorMessages(
    val rejectedNotSaved: String,
    val checkFailedNotSaved: String,
    val unreachableNotSaved: String,
    val saveFailed: String,
    val clearFailed: String,
    val noKey: String,
    val rejected: String,
    val checkFailed: String,
)

@Composable
private fun providerErrorMessages(name: String) = ProviderErrorMessages(
    rejectedNotSaved = stringResource(Res.string.provider_rejected_not_saved, name),
    checkFailedNotSaved = stringResource(Res.string.provider_check_failed_not_saved, name),
    unreachableNotSaved = stringResource(Res.string.provider_unreachable_not_saved, name),
    saveFailed = stringResource(Res.string.provider_save_failed, name),
    clearFailed = stringResource(Res.string.provider_clear_failed, name),
    noKey = stringResource(Res.string.provider_no_key, name),
    rejected = stringResource(Res.string.provider_rejected, name),
    checkFailed = stringResource(Res.string.provider_check_failed, name),
)

@Composable
private fun SettingsHeader(
    workspaceName: String,
    configuredProviders: Int,
) {
    WorkspaceHeader(
        title = stringResource(Res.string.settings),
        subtitle = stringResource(Res.string.settings_workspace_subtitle, workspaceName),
        icon = Res.drawable.settings,
        summaries = listOf(
            stringResource(Res.string.local_first_workspace),
            stringResource(
                Res.string.configured_providers_summary,
                configuredProviders,
                if (configuredProviders == 1) "" else "s",
            ),
        ),
    )
}

@Composable
internal fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        content()
    }
}

@Composable
internal fun SettingsCard(content: @Composable () -> Unit) {
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
                TextButton(onClick = onRetry) { Text(stringResource(Res.string.retry)) }
            }
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.dismiss)) }
        }
    }
}

@Composable
private fun ReadinessChip(
    label: String,
    ready: Boolean,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        modifier = modifier,
        selected = ready,
        onClick = {},
        enabled = false,
        label = {
            Text(
                stringResource(
                    if (ready) Res.string.provider_key_saved_chip else Res.string.provider_not_set_chip,
                    label,
                ),
            )
        },
    )
}

private fun validateAndSaveSecret(
    scope: kotlinx.coroutines.CoroutineScope,
    secretStore: SecretStore,
    key: SecretKey,
    draft: String,
    label: String,
    errors: ProviderErrorMessages,
    client: HttpClient,
    onSuccess: () -> Unit,
    onFailure: (String) -> Unit,
) {
    // API keys are opaque tokens; clipboard copies can include line breaks or
    // spaces from formatted text, so normalize that presentation noise.
    val value = draft.filterNot(Char::isWhitespace)
    if (value.isEmpty()) return
    scope.launch {
        try {
            val response = requestKeyValidation(client, key, value)
            if (response.status.value !in 200..299) {
                if (response.status.value == 401 || response.status.value == 403) {
                    onFailure(errors.rejectedNotSaved)
                } else {
                    onFailure(errors.checkFailedNotSaved)
                }
                return@launch
            }
            secretStore.write(key, value)
            onSuccess()
        } catch (exception: kotlinx.coroutines.CancellationException) {
            throw exception
        } catch (_: Exception) {
            onFailure(errors.unreachableNotSaved)
        }
    }
}

private fun saveSecret(
    scope: kotlinx.coroutines.CoroutineScope,
    secretStore: SecretStore,
    key: SecretKey,
    draft: String,
    label: String,
    errors: ProviderErrorMessages,
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
            onFailure(errors.saveFailed)
        }
    }
}

private fun deleteSecret(
    scope: kotlinx.coroutines.CoroutineScope,
    secretStore: SecretStore,
    key: SecretKey,
    label: String,
    errors: ProviderErrorMessages,
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
            onFailure(errors.clearFailed)
        }
    }
}

private fun validateSecret(
    scope: kotlinx.coroutines.CoroutineScope,
    secretStore: SecretStore,
    key: SecretKey,
    label: String,
    errors: ProviderErrorMessages,
    client: HttpClient,
    onSuccess: () -> Unit,
    onFailure: (String) -> Unit,
) {
    scope.launch {
        try {
            val value = secretStore.read(key)?.filterNot(Char::isWhitespace).orEmpty()
            if (value.isEmpty()) {
                onFailure(errors.noKey)
                return@launch
            }
            val response = requestKeyValidation(client, key, value)
            if (response.status.value in 200..299) onSuccess()
            else if (response.status.value == 401 || response.status.value == 403) {
                onFailure(errors.rejected)
            } else {
                onFailure(errors.checkFailed)
            }
        } catch (exception: kotlinx.coroutines.CancellationException) {
            throw exception
        } catch (_: Exception) {
            onFailure(errors.checkFailed)
        }
    }
}

private suspend fun requestKeyValidation(
    client: HttpClient,
    key: SecretKey,
    value: String,
) = when (key) {
    SecretKey.OPENAI -> client.get("https://api.openai.com/v1/models") {
        bearerAuth(value)
    }
    SecretKey.MISTRAL -> client.get("https://api.mistral.ai/v1/models") {
        bearerAuth(value)
    }
    SecretKey.ZOTERO -> client.get("https://api.zotero.org/keys/$value")
    SecretKey.OPENALEX -> client.get("https://api.openalex.org/works") {
        parameter("api_key", value)
        parameter("per-page", 1)
    }
    SecretKey.MCP_TOKEN -> error("The MCP access token is local and has no provider to validate against.")
}

@Composable
private fun ThemePreference.localizedLabel(): String = when (this) {
    ThemePreference.SYSTEM -> stringResource(Res.string.system_default)
    ThemePreference.LIGHT -> stringResource(Res.string.light)
    ThemePreference.DARK -> stringResource(Res.string.dark)
}

@Composable
private fun ThemePreference.localizedDescription(): String = when (this) {
    ThemePreference.SYSTEM -> stringResource(Res.string.follow_device_appearance)
    ThemePreference.LIGHT -> stringResource(Res.string.use_light_palette)
    ThemePreference.DARK -> stringResource(Res.string.use_dark_palette)
}

@Composable
private fun LanguagePreference.localizedLabel(): String = when (this) {
    LanguagePreference.ENGLISH -> stringResource(Res.string.english)
    LanguagePreference.FRENCH -> stringResource(Res.string.french)
    LanguagePreference.ARABIC -> stringResource(Res.string.arabic)
}

@Composable
private fun LanguagePreference.localizedDescription(): String = when (this) {
    LanguagePreference.ENGLISH -> stringResource(Res.string.use_english)
    LanguagePreference.FRENCH -> stringResource(Res.string.use_french)
    LanguagePreference.ARABIC -> stringResource(Res.string.use_arabic)
}
