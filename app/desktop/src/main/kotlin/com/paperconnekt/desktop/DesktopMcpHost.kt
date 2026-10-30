package com.paperconnekt.desktop

import com.akram.bensalem.paper.connekt.feature.mcp.McpHost
import com.akram.bensalem.paper.connekt.feature.mcp.McpHostConfig
import com.akram.bensalem.paper.connekt.feature.mcp.McpHostStatus
import com.akram.bensalem.paper.connekt.feature.mcp.PaperConnektMcpRuntime
import com.paperconnekt.mcp.HttpMcpTransport
import com.paperconnekt.mcp.McpActivity
import com.paperconnekt.secrets.SecretKey
import com.paperconnekt.secrets.SecretStore
import com.paperconnekt.storage.LocalResearchStore
import java.security.SecureRandom
import java.util.Base64
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Serves the library over Streamable HTTP on 127.0.0.1 while the app runs. The server is restarted only when the port
 * or token changes; the idea-writes switch is read on every request.
 */
class DesktopMcpHost : McpHost {
    private val state = MutableStateFlow(McpHostStatus())
    override val status: StateFlow<McpHostStatus> = state.asStateFlow()

    private val log = MutableStateFlow<List<McpActivity>>(emptyList())
    override val activity: StateFlow<List<McpActivity>> = log.asStateFlow()

    override val stdioCommand: List<String>? by lazy(::currentLaunchCommand)

    private val lock = Mutex()
    private val tokenLock = Mutex()
    private var runtime: PaperConnektMcpRuntime? = null
    private var transport: HttpMcpTransport? = null
    private var servedPort: Int? = null
    private var servedToken: String? = null
    private var lastConfig: McpHostConfig? = null
    private var lastStore: LocalResearchStore? = null

    @Volatile
    private var allowIdeaWrites = false

    override suspend fun apply(config: McpHostConfig, store: LocalResearchStore, secretStore: SecretStore) = lock.withLock {
        lastConfig = config
        lastStore = store
        allowIdeaWrites = config.allowIdeaWrites
        if (!config.enabled) {
            stopTransport()
            state.value = McpHostStatus()
            return@withLock
        }
        val token = try {
            token(secretStore)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            stopTransport()
            state.value = McpHostStatus(error = "The access token could not be read: ${exception.message}")
            return@withLock
        }
        if (transport != null && servedPort == config.port && servedToken == token) return@withLock
        stopTransport()
        val server = (runtime ?: PaperConnektMcpRuntime(store, secretStore, { allowIdeaWrites }, log = log).also { runtime = it }).server
        state.value = try {
            val started = withContext(Dispatchers.IO) { HttpMcpTransport(server, token, config.port).start() }
            transport = started
            servedPort = config.port
            servedToken = token
            McpHostStatus(running = true, endpoint = started.endpoint)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            McpHostStatus(error = "Port ${config.port} is unavailable (${exception.message ?: exception::class.simpleName}). Choose another port.")
        }
    }

    override suspend fun token(secretStore: SecretStore): String = tokenLock.withLock {
        secretStore.read(SecretKey.MCP_TOKEN)?.trim()?.takeIf { it.length >= 32 }
            ?: newToken().also { secretStore.write(SecretKey.MCP_TOKEN, it) }
    }

    override suspend fun regenerateToken(secretStore: SecretStore): String {
        val token = tokenLock.withLock { newToken().also { secretStore.write(SecretKey.MCP_TOKEN, it) } }
        val config = lastConfig
        val store = lastStore
        if (config != null && store != null) apply(config, store, secretStore)
        return token
    }

    override fun clearActivity() {
        log.value = emptyList()
    }

    fun close() {
        transport?.close()
        runtime?.close()
    }

    private fun stopTransport() {
        transport?.close()
        transport = null
        servedPort = null
        servedToken = null
    }
}

private fun newToken(): String {
    val bytes = ByteArray(32).also(SecureRandom()::nextBytes)
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
}

/**
 * The command that started this process plus `--mcp`: the native launcher of an installed app, or the `java` command
 * line when run from Gradle. Null when the OS hides the command line.
 */
private fun currentLaunchCommand(): List<String>? {
    val info = ProcessHandle.current().info()
    val command = info.command().orElse(null) ?: return null
    val arguments = info.arguments().orElse(emptyArray()).toList()
    if (command.substringAfterLast('/').substringAfterLast('\\').startsWith("java") && arguments.isEmpty()) return null
    return listOf(command) + arguments.filter { it != "--mcp" } + "--mcp"
}
