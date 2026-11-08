package com.paperconnekt.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.akram.bensalem.paper.connekt.feature.mcp.McpHost
import com.paperconnekt.domain.model.AppSettings
import com.paperconnekt.mcp.McpActivity
import com.paperconnekt.secrets.SecretStore
import com.paperconnekt.storage.LocalResearchStore
import com.paperconnekt.app.resources.Res
import org.jetbrains.compose.resources.stringResource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import com.paperconnekt.app.resources.access_token
import com.paperconnekt.app.resources.ai_tools_mcp
import com.paperconnekt.app.resources.claude_desktop_configuration
import com.paperconnekt.app.resources.clear
import com.paperconnekt.app.resources.connect_a_client
import com.paperconnekt.app.resources.connect_clients_description
import com.paperconnekt.app.resources.copy
import com.paperconnekt.app.resources.cursor_configuration
import com.paperconnekt.app.resources.cursor
import com.paperconnekt.app.resources.vs_code
import com.paperconnekt.app.resources.claude_desktop
import com.paperconnekt.app.resources.copied_message
import com.paperconnekt.app.resources.hide
import com.paperconnekt.app.resources.mcp_add_idea_notes
import com.paperconnekt.app.resources.mcp_add_idea_notes_description
import com.paperconnekt.app.resources.mcp_create_token_error
import com.paperconnekt.app.resources.mcp_read_token_error
import com.paperconnekt.app.resources.mcp_use_library
import com.paperconnekt.app.resources.mcp_use_library_description
import com.paperconnekt.app.resources.new_token_created
import com.paperconnekt.app.resources.no_requests_yet
import com.paperconnekt.app.resources.not_running
import com.paperconnekt.app.resources.off
import com.paperconnekt.app.resources.port
import com.paperconnekt.app.resources.recent_requests
import com.paperconnekt.app.resources.regenerate
import com.paperconnekt.app.resources.running_at
import com.paperconnekt.app.resources.show
import com.paperconnekt.app.resources.starting
import com.paperconnekt.app.resources.token
import com.paperconnekt.app.resources.use_port
import com.paperconnekt.app.resources.vs_code_configuration

/**
 * "AI tools (MCP)" settings: lets Claude Desktop, VS Code, Cursor and other MCP clients use the local library, with
 * the endpoint, the access token, ready-to-paste client configurations and a log of every request.
 */
@Composable
internal fun McpSettingsSection(
    host: McpHost,
    store: LocalResearchStore,
    secretStore: SecretStore,
    settings: AppSettings,
) {
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val status by host.status.collectAsState()
    val activity by host.activity.collectAsState()
    var enabled by remember { mutableStateOf(settings.mcpServerEnabled) }
    var allowWrites by remember { mutableStateOf(settings.mcpAllowIdeaWrites) }
    var portDraft by remember { mutableStateOf(settings.mcpServerPort.toString()) }
    var token by remember { mutableStateOf<String?>(null) }
    var tokenVisible by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val accessTokenLabel = stringResource(Res.string.access_token)
    val clearLabel = stringResource(Res.string.clear)
    val noRequestsLabel = stringResource(Res.string.no_requests_yet)
    val tokenLabel = stringResource(Res.string.token)
    val newTokenCreatedMessage = stringResource(Res.string.new_token_created)
    val readTokenError = stringResource(Res.string.mcp_read_token_error)
    val createTokenError = stringResource(Res.string.mcp_create_token_error)
    val vsCodeConfigurationLabel = stringResource(Res.string.vs_code_configuration)
    val cursorConfigurationLabel = stringResource(Res.string.cursor_configuration)
    val claudeDesktopConfigurationLabel = stringResource(Res.string.claude_desktop_configuration)
    val vsCodeCopiedMessage = stringResource(Res.string.copied_message, vsCodeConfigurationLabel)
    val cursorCopiedMessage = stringResource(Res.string.copied_message, cursorConfigurationLabel)
    val claudeDesktopCopiedMessage = stringResource(Res.string.copied_message, claudeDesktopConfigurationLabel)
    val tokenCopiedMessage = stringResource(Res.string.copied_message, tokenLabel)

    LaunchedEffect(host, enabled) {
        if (enabled && token == null) token = try {
            host.token(secretStore)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            message = readTokenError
            null
        }
    }

    fun copy(localizedMessage: String, text: String) {
        clipboard.setText(AnnotatedString(text))
        message = localizedMessage
    }

    SettingsSection(title = stringResource(Res.string.ai_tools_mcp)) {
        SettingsCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingSwitchRow(
                    title = stringResource(Res.string.mcp_use_library),
                    description = stringResource(Res.string.mcp_use_library_description),
                    checked = enabled,
                    contentDescription = stringResource(Res.string.mcp_use_library),
                ) { value ->
                    enabled = value
                    scope.launch { store.updateSettings { it.copy(mcpServerEnabled = value) } }
                }
                SettingSwitchRow(
                    title = stringResource(Res.string.mcp_add_idea_notes),
                    description = stringResource(Res.string.mcp_add_idea_notes_description),
                    checked = allowWrites,
                    contentDescription = stringResource(Res.string.mcp_add_idea_notes),
                ) { value ->
                    allowWrites = value
                    scope.launch { store.updateSettings { it.copy(mcpAllowIdeaWrites = value) } }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = portDraft,
                        onValueChange = { portDraft = it.filter(Char::isDigit).take(5) },
                        label = { Text(stringResource(Res.string.port)) },
                        singleLine = true,
                        isError = portDraft.toIntOrNull()?.let { it !in 1024..65535 } ?: true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedButton(
                        enabled = portDraft.toIntOrNull()?.let { it in 1024..65535 && it != settings.mcpServerPort } == true,
                        onClick = {
                            val port = portDraft.toInt()
                            scope.launch { store.updateSettings { it.copy(mcpServerPort = port) } }
                        },
                    ) { Text(stringResource(Res.string.use_port)) }
                }
                Text(
                    when {
                        status.running -> stringResource(Res.string.running_at, status.endpoint.orEmpty())
                        status.error != null -> stringResource(Res.string.not_running, status.error.orEmpty())
                        enabled -> stringResource(Res.string.starting)
                        else -> stringResource(Res.string.off)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (status.error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val currentToken = token
                if (enabled && currentToken != null) {
                    HorizontalDivider()
                    Text(accessTokenLabel, style = MaterialTheme.typography.titleSmall)
                    Text(
                        if (tokenVisible) currentToken else "•".repeat(24),
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { tokenVisible = !tokenVisible }) { Text(stringResource(if (tokenVisible) Res.string.hide else Res.string.show)) }
                        TextButton(onClick = { copy(tokenCopiedMessage, currentToken) }) { Text(stringResource(Res.string.copy)) }
                        TextButton(onClick = {
                            scope.launch {
                                token = try {
                                    host.regenerateToken(secretStore).also { message = newTokenCreatedMessage }
                                } catch (exception: CancellationException) {
                                    throw exception
                                } catch (exception: Exception) {
                                    message = createTokenError
                                    currentToken
                                }
                            }
                        }) { Text(stringResource(Res.string.regenerate)) }
                    }
                    HorizontalDivider()
                    Text(stringResource(Res.string.connect_a_client), style = MaterialTheme.typography.titleSmall)
                    Text(
                        stringResource(Res.string.connect_clients_description),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    val endpoint = status.endpoint ?: "http://127.0.0.1:${settings.mcpServerPort}/mcp"
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { copy(vsCodeCopiedMessage, vsCodeConfig(endpoint, currentToken)) }) { Text(stringResource(Res.string.vs_code)) }
                        OutlinedButton(onClick = { copy(cursorCopiedMessage, cursorConfig(endpoint, currentToken)) }) { Text(stringResource(Res.string.cursor)) }
                        host.stdioCommand?.let { command ->
                            OutlinedButton(onClick = { copy(claudeDesktopCopiedMessage, claudeConfig(command)) }) { Text(stringResource(Res.string.claude_desktop)) }
                        }
                    }
                }
                message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
            }
        }
        if (enabled || activity.isNotEmpty()) SettingsCard {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(Res.string.recent_requests), style = MaterialTheme.typography.titleSmall)
                    if (activity.isNotEmpty()) TextButton(onClick = host::clearActivity) { Text(clearLabel) }
                }
                if (activity.isEmpty()) {
                    Text(noRequestsLabel, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
                activity.take(30).forEach { entry -> ActivityRow(entry) }
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    contentDescription: String,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.semantics { this.contentDescription = contentDescription },
        )
    }
}

@Composable
private fun ActivityRow(entry: McpActivity) {
    val what = entry.tool ?: entry.method
    val who = entry.client?.let { " · $it" }.orEmpty()
    val time = "${entry.durationMillis} ms"
    Column {
        Text(
            "${if (entry.ok) "✓" else "✗"} ${clockTime(entry.epochMillis)} $what$who · $time",
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = if (entry.ok) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
        )
        if (entry.detail.isNotBlank()) {
            Text(entry.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
        }
    }
}

private fun clockTime(epochMillis: Long): String {
    val secondsOfDay = (epochMillis / 1000) % 86_400
    fun two(value: Long) = value.toString().padStart(2, '0')
    return "${two(secondsOfDay / 3600)}:${two(secondsOfDay % 3600 / 60)}:${two(secondsOfDay % 60)} UTC"
}

private val prettyJson = Json { prettyPrint = true }

private fun vsCodeConfig(endpoint: String, token: String) = prettyJson.encodeToString(
    JsonObject.serializer(),
    buildJsonObject {
        putJsonObject("servers") {
            putJsonObject("paperconnekt") {
                put("type", "http")
                put("url", endpoint)
                putJsonObject("headers") { put("Authorization", "Bearer $token") }
            }
        }
    },
)

private fun cursorConfig(endpoint: String, token: String) = prettyJson.encodeToString(
    JsonObject.serializer(),
    buildJsonObject {
        putJsonObject("mcpServers") {
            putJsonObject("paperconnekt") {
                put("url", endpoint)
                putJsonObject("headers") { put("Authorization", "Bearer $token") }
            }
        }
    },
)

private fun claudeConfig(command: List<String>) = prettyJson.encodeToString(
    JsonObject.serializer(),
    buildJsonObject {
        putJsonObject("mcpServers") {
            putJsonObject("paperconnekt") {
                put("command", command.first())
                put("args", JsonArray(command.drop(1).map(::JsonPrimitive)))
            }
        }
    },
)
