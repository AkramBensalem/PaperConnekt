package com.akram.bensalem.paper.connekt.feature.mcp

import androidx.compose.runtime.staticCompositionLocalOf
import com.akram.bensalem.paper.connekt.core.aiassistant.EmbeddingProviders
import com.akram.bensalem.paper.connekt.feature.assistant.IdeaNoteStore
import com.akram.bensalem.paper.connekt.feature.assistant.syncedIdeaNoteStore
import com.paperconnekt.mcp.McpActivity
import com.paperconnekt.mcp.McpServer
import com.paperconnekt.secrets.SecretStore
import com.paperconnekt.storage.LocalResearchStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** What the user configured in Settings. */
data class McpHostConfig(val enabled: Boolean, val port: Int, val allowIdeaWrites: Boolean)

data class McpHostStatus(
    val running: Boolean = false,
    /** Streamable HTTP endpoint while [running], such as `http://127.0.0.1:7337/mcp`. */
    val endpoint: String? = null,
    val error: String? = null,
)

/**
 * Platform side of "PaperConnekt as an MCP server". Desktop serves Streamable HTTP on the loopback interface while the
 * app runs (and stdio with `--mcp`); platforms without a host provide none and Settings hides the section.
 */
interface McpHost {
    val status: StateFlow<McpHostStatus>
    val activity: StateFlow<List<McpActivity>>

    /** Command line that starts the stdio server, for clients that launch servers themselves; null if unavailable. */
    val stdioCommand: List<String>?

    /** Starts, restarts or stops the server to match [config]. */
    suspend fun apply(config: McpHostConfig, store: LocalResearchStore, secretStore: SecretStore)

    /** Bearer token HTTP clients must send; created on first use. */
    suspend fun token(secretStore: SecretStore): String

    /** Replaces the token, disconnecting every configured client. */
    suspend fun regenerateToken(secretStore: SecretStore): String

    fun clearActivity()
}

val LocalMcpHost = staticCompositionLocalOf<McpHost?> { null }

/**
 * Everything an MCP transport needs, independent of the transport: the library, the server and the activity log.
 * Both the in-app HTTP host and the headless stdio launcher build one.
 */
class PaperConnektMcpRuntime(
    store: LocalResearchStore,
    secretStore: SecretStore?,
    allowIdeaWrites: () -> Boolean,
    ideas: IdeaNoteStore = syncedIdeaNoteStore(),
    private val clock: () -> Long = { kotlin.time.Clock.System.now().toEpochMilliseconds() },
    private val activityLimit: Int = 200,
    /** Activity log, newest first; pass one in to keep it across runtimes. */
    private val log: MutableStateFlow<List<McpActivity>> = MutableStateFlow(emptyList()),
    /** Also receives every entry, for example to print it in a headless launcher. */
    private val onActivity: (McpActivity) -> Unit = {},
) {
    val activity: StateFlow<List<McpActivity>> = log.asStateFlow()

    private val embeddings = EmbeddingProviders(secretStore)

    val library = PaperLibrary(
        store = store,
        embedder = { if (store.settings.value.semanticSearchEnabled) embeddings.current() else null },
        onEmbedded = { model, texts ->
            record(
                McpActivity(
                    epochMillis = clock(),
                    client = null,
                    method = "embeddings",
                    detail = "Embedded ${texts.size} passage(s) with $model, about ${texts.sumOf { it.length } / 4} tokens",
                ),
            )
        },
    )

    val server: McpServer = paperConnektMcpServer(library, ideas, allowIdeaWrites, clock, ::record)

    fun record(entry: McpActivity) {
        log.update { (listOf(entry) + it).take(activityLimit) }
        onActivity(entry)
    }

    fun clearActivity() {
        log.value = emptyList()
    }

    fun close() = library.close()
}
