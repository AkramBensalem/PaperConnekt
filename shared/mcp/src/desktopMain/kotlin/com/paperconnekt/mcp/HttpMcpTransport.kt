package com.paperconnekt.mcp

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.URI
import java.security.MessageDigest
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * MCP Streamable HTTP transport on the loopback interface, without server-sent events.
 *
 * Clients POST each JSON-RPC message to `http://127.0.0.1:<port>/mcp` and receive the response as `application/json`
 * (notifications get `202 Accepted`). The server never pushes messages, so `GET` answers `405`, as the specification
 * allows. Every request must carry `Authorization: Bearer <token>`, and a browser `Origin` other than localhost is
 * rejected, so neither other users' web pages nor DNS-rebinding sites can reach the user's library.
 */
class HttpMcpTransport(
    private val server: McpServer,
    private val token: String,
    private val port: Int,
    private val path: String = "/mcp",
    private val maxRequestBytes: Int = 1_000_000,
    private val threads: Int = 4,
) : AutoCloseable {
    private var http: HttpServer? = null
    private var executor: ExecutorService? = null

    init {
        require(token.length >= 16) { "The MCP access token is too short." }
    }

    /** Bound port; differs from the requested one only when that was 0 (any free port). */
    val boundPort: Int get() = http?.address?.port ?: port

    val endpoint: String get() = "http://127.0.0.1:$boundPort$path"

    @Synchronized
    fun start(): HttpMcpTransport {
        check(http == null) { "The MCP server is already running." }
        val pool = Executors.newFixedThreadPool(threads) { runnable ->
            Thread(runnable, "paperconnekt-mcp-http").apply { isDaemon = true }
        }
        val created = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), port), 32)
        created.createContext("/") { exchange -> serve(exchange) }
        created.executor = pool
        created.start()
        http = created
        executor = pool
        return this
    }

    @Synchronized
    override fun close() {
        http?.stop(0)
        executor?.shutdownNow()
        http = null
        executor = null
    }

    private fun serve(exchange: HttpExchange) {
        try {
            when {
                exchange.requestURI.path.trimEnd('/') != path -> respond(exchange, 404, "Not found. The MCP endpoint is $path.")
                !originAllowed(exchange.requestHeaders.getFirst("Origin")) -> respond(exchange, 403, "Forbidden origin.")
                !authorized(exchange.requestHeaders.getFirst("Authorization")) -> {
                    exchange.responseHeaders.add("WWW-Authenticate", "Bearer realm=\"PaperConnekt\"")
                    respond(exchange, 401, "Missing or invalid bearer token. Copy it from PaperConnekt Settings.")
                }
                exchange.requestMethod.equals("POST", ignoreCase = true) -> post(exchange)
                else -> {
                    exchange.responseHeaders.add("Allow", "POST")
                    respond(exchange, 405, "This server only accepts POST; it does not open event streams.")
                }
            }
        } catch (exception: Exception) {
            runCatching { respond(exchange, 500, exception.message ?: "Internal error.") }
        } finally {
            exchange.close()
        }
    }

    private fun post(exchange: HttpExchange) {
        val body = exchange.requestBody.readNBytes(maxRequestBytes + 1)
        if (body.size > maxRequestBytes) return respond(exchange, 413, "Request too large.")
        val response = runBlocking { server.handle(body.toString(Charsets.UTF_8)) }
        if (response == null) {
            exchange.sendResponseHeaders(202, -1)
        } else {
            val bytes = response.toByteArray(Charsets.UTF_8)
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
    }

    private fun authorized(header: String?): Boolean {
        val presented = header?.trim()?.takeIf { it.startsWith("Bearer ", ignoreCase = true) }?.substring(7)?.trim()
            ?: return false
        return MessageDigest.isEqual(presented.toByteArray(Charsets.UTF_8), token.toByteArray(Charsets.UTF_8))
    }

    private fun respond(exchange: HttpExchange, status: Int, message: String) {
        val bytes = message.toByteArray(Charsets.UTF_8)
        exchange.responseHeaders.add("Content-Type", "text/plain; charset=utf-8")
        exchange.sendResponseHeaders(status, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }

    companion object {
        /** Native clients send no Origin; browsers must come from a page served by this machine. */
        fun originAllowed(origin: String?): Boolean {
            if (origin.isNullOrBlank()) return true
            val host = runCatching { URI(origin.trim()).host }.getOrNull()?.lowercase() ?: return false
            return host == "localhost" || host == "127.0.0.1" || host == "[::1]" || host == "::1"
        }
    }
}
