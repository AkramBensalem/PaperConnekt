package com.paperconnekt.mcp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

/**
 * MCP stdio transport: one JSON-RPC message per line on [input], one response per line on [output].
 *
 * Requests are handled concurrently, so a slow tool call does not hold back the next one; responses are written whole,
 * one at a time. Nothing else may write to [output], so the launcher must send its logs to stderr. Returns when the
 * client closes [input] and every in-flight request has answered.
 */
class StdioMcpTransport(
    private val server: McpServer,
    private val input: InputStream = System.`in`,
    private val output: OutputStream = System.out,
) {
    suspend fun run() = coroutineScope {
        val reader = input.bufferedReader(Charsets.UTF_8)
        val writer = output.bufferedWriter(Charsets.UTF_8)
        val writeLock = Mutex()
        while (true) {
            val line = withContext(Dispatchers.IO) { reader.readLine() } ?: break
            if (line.isBlank()) continue
            launch(Dispatchers.Default) {
                val response = server.handle(line) ?: return@launch
                writeLock.withLock {
                    withContext(Dispatchers.IO) {
                        writer.write(response)
                        writer.write("\n")
                        writer.flush()
                    }
                }
            }
        }
    }
}
