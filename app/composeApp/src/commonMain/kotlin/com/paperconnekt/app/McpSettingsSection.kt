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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

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

    LaunchedEffect(host, enabled) {
        if (enabled && token == null) token = try {
            host.token(secretStore)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            message = "Could not read the access token: ${exception.message}"
            null
        }
    }

    fun copy(label: String, text: String) {
        clipboard.setText(AnnotatedString(text))
        message = "$label copied."
    }

    SettingsSection(title = "AI tools (MCP)") {
        SettingsCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingSwitchRow(
                    title = "Let AI tools use my library",
                    description = "Serves your papers to MCP clients such as Claude Desktop, VS Code Copilot or Cursor on this computer " +
                        "only. They can list papers and retrieve just the passages they need, so answers stay cited and cheap. " +
                        "Requests need the access token and are listed below.",
                    checked = enabled,
                    contentDescription = "Let AI tools use my library",
                ) { value ->
                    enabled = value
                    scope.launch { store.updateSettings { it.copy(mcpServerEnabled = value) } }
                }
                SettingSwitchRow(
                    title = "Allow adding idea notes",
                    description = "Lets clients add notes to a paper's Idea Board. Everything else is read-only.",
                    checked = allowWrites,
                    contentDescription = "Allow MCP clients to add idea notes",
                ) { value ->
                    allowWrites = value
                    scope.launch { store.updateSettings { it.copy(mcpAllowIdeaWrites = value) } }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = portDraft,
                        onValueChange = { portDraft = it.filter(Char::isDigit).take(5) },
                        label = { Text("Port") },
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
                    ) { Text("Use port") }
                }
                Text(
                    when {
                        status.running -> "Running at ${status.endpoint}"
                        status.error != null -> "Not running: ${status.error}"
                        enabled -> "Starting…"
                        else -> "Off"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (status.error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val currentToken = token
                if (enabled && currentToken != null) {
                    HorizontalDivider()
                    Text("Access token", style = MaterialTheme.typography.titleSmall)
                    Text(
                        if (tokenVisible) currentToken else "•".repeat(24),
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { tokenVisible = !tokenVisible }) { Text(if (tokenVisible) "Hide" else "Show") }
                        TextButton(onClick = { copy("Token", currentToken) }) { Text("Copy") }
                        TextButton(onClick = {
                            scope.launch {
                                token = try {
                                    host.regenerateToken(secretStore).also { message = "New token created. Update your clients." }
                                } catch (exception: CancellationException) {
                                    throw exception
                                } catch (exception: Exception) {
                                    message = "Could not create a token: ${exception.message}"
                                    currentToken
                                }
                            }
                        }) { Text("Regenerate") }
                    }
                    HorizontalDivider()
                    Text("Connect a client", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "VS Code and Cursor connect over HTTP while PaperConnekt is open. Claude Desktop starts its own PaperConnekt " +
                            "process; notes it adds appear when you reopen the paper.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    val endpoint = status.endpoint ?: "http://127.0.0.1:${settings.mcpServerPort}/mcp"
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { copy("VS Code configuration", vsCodeConfig(endpoint, currentToken)) }) { Text("VS Code") }
                        OutlinedButton(onClick = { copy("Cursor configuration", cursorConfig(endpoint, currentToken)) }) { Text("Cursor") }
                        host.stdioCommand?.let { command ->
                            OutlinedButton(onClick = { copy("Claude Desktop configuration", claudeConfig(command)) }) { Text("Claude Desktop") }
                        }
                    }
                }
                message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
            }
        }
        if (enabled || activity.isNotEmpty()) SettingsCard {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Recent requests", style = MaterialTheme.typography.titleSmall)
                    if (activity.isNotEmpty()) TextButton(onClick = host::clearActivity) { Text("Clear") }
                }
                if (activity.isEmpty()) {
                    Text("No requests yet.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
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
