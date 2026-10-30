package com.paperconnekt.mcp

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class McpTransportsTest {
    private val server = McpServer(
        McpServerInfo("paperconnekt", "test"),
        tools = {
            listOf(McpTool("echo", "Echo.", JsonObject(emptyMap())) { McpToolResult((it["text"] as JsonPrimitive).content) })
        },
    )
    private val token = "0123456789abcdef0123456789abcdef"

    @Test
    fun stdioAnswersEachRequestOnItsOwnLine() = runBlocking {
        val input = listOf(
            """{"jsonrpc":"2.0","id":1,"method":"ping"}""",
            "",
            """{"jsonrpc":"2.0","method":"notifications/initialized"}""",
            """{"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"echo","arguments":{"text":"a\nb"}}}""",
        ).joinToString("\n")
        val output = ByteArrayOutputStream()
        StdioMcpTransport(server, ByteArrayInputStream(input.toByteArray()), output).run()
        val lines = output.toString(Charsets.UTF_8).trimEnd().lines()
        assertEquals(2, lines.size)
        assertTrue(lines.any { "\"id\":1" in it })
        assertTrue(lines.any { "\"id\":2" in it && "a\\nb" in it })
    }

    @Test
    fun httpRequiresTheTokenAndAnswersJson() {
        HttpMcpTransport(server, token, port = 0).start().use { transport ->
            val client = HttpClient.newHttpClient()
            fun post(body: String, auth: String? = "Bearer $token", origin: String? = null): HttpResponse<String> {
                val request = HttpRequest.newBuilder(URI(transport.endpoint))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                auth?.let { request.header("Authorization", it) }
                origin?.let { request.header("Origin", it) }
                return client.send(request.build(), HttpResponse.BodyHandlers.ofString())
            }
            val ping = """{"jsonrpc":"2.0","id":1,"method":"ping"}"""
            assertEquals(401, post(ping, auth = null).statusCode())
            assertEquals(401, post(ping, auth = "Bearer wrong-token-wrong-token").statusCode())
            assertEquals(403, post(ping, origin = "https://evil.example").statusCode())

            val ok = post(ping, origin = "http://localhost:3000")
            assertEquals(200, ok.statusCode())
            assertTrue(ok.headers().firstValue("Content-Type").get().startsWith("application/json"))
            assertTrue("\"result\"" in ok.body())

            assertEquals(202, post("""{"jsonrpc":"2.0","method":"notifications/initialized"}""").statusCode())

            val get = client.send(
                HttpRequest.newBuilder(URI(transport.endpoint)).header("Authorization", "Bearer $token").GET().build(),
                HttpResponse.BodyHandlers.ofString(),
            )
            assertEquals(405, get.statusCode())
        }
    }

    @Test
    fun originPolicy() {
        assertTrue(HttpMcpTransport.originAllowed(null))
        assertTrue(HttpMcpTransport.originAllowed("http://127.0.0.1:5173"))
        assertFalse(HttpMcpTransport.originAllowed("http://localhost.evil.example"))
        assertFalse(HttpMcpTransport.originAllowed("not a uri"))
    }
}
