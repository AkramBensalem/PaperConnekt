package com.paperconnekt.desktop

import com.akram.bensalem.paper.connekt.feature.mcp.PaperConnektMcpRuntime
import com.paperconnekt.mcp.StdioMcpTransport
import com.paperconnekt.storage.DesktopWorkspaceStorage
import com.paperconnekt.storage.PersistentResearchStore
import com.paperconnekt.storage.WorkspaceStorage
import java.io.PrintStream
import kotlin.system.exitProcess
import kotlinx.coroutines.runBlocking

/**
 * `PaperConnekt --mcp`: a headless MCP server over stdio, for clients that launch their servers themselves (Claude
 * Desktop and most CLIs). It reads the workspace as it was at launch and never writes it; only idea notes are written
 * when the user allowed them. Stdout carries the protocol, so every log goes to stderr.
 */
internal fun runStdioMcpServer(): Nothing {
    val protocol = PrintStream(System.out, false, Charsets.UTF_8)
    System.setOut(System.err)
    val exitCode = try {
        runBlocking {
            val store = PersistentResearchStore(ReadOnlyWorkspaceStorage(DesktopWorkspaceStorage()))
            store.initialize()
            if (!store.settings.value.mcpServerEnabled) {
                System.err.println("PaperConnekt: enable Settings > AI tools (MCP) in the app before connecting an MCP client.")
                return@runBlocking 1
            }
            val runtime = PaperConnektMcpRuntime(
                store = store,
                secretStore = DesktopSecretStore(),
                allowIdeaWrites = { store.settings.value.mcpAllowIdeaWrites },
                onActivity = { entry ->
                    System.err.println("PaperConnekt MCP: ${entry.tool ?: entry.method} ${if (entry.ok) "ok" else "failed"} in ${entry.durationMillis} ms ${entry.detail}")
                },
            )
            try {
                StdioMcpTransport(runtime.server, System.`in`, protocol).run()
            } finally {
                runtime.close()
            }
            0
        }
    } catch (exception: Exception) {
        System.err.println("PaperConnekt MCP stopped: ${exception.message}")
        1
    }
    exitProcess(exitCode)
}

/** The GUI owns workspace.json; the stdio server only reads it and must never rewrite or quarantine it. */
private class ReadOnlyWorkspaceStorage(private val delegate: WorkspaceStorage) : WorkspaceStorage {
    override suspend fun read(): String? = delegate.read()
    override suspend fun write(content: String) = Unit
    override suspend fun quarantineCorruptData() = Unit
}
