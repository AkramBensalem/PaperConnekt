package com.paperconnekt.mcp

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class McpServerTest {
    private val activity = mutableListOf<McpActivity>()
    private val echo = McpTool(
        name = "echo",
        description = "Repeats the text.",
        inputSchema = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") { putJsonObject("text") { put("type", "string") } }
        },
    ) { args ->
        val text = (args["text"] as? JsonPrimitive)?.content
        if (text == null) McpToolResult("Missing text.", isError = true) else McpToolResult(text)
    }
    private val failing = McpTool("boom", "Always throws.", JsonObject(emptyMap()), readOnly = false) { error("kaput") }
    private val server = McpServer(
        McpServerInfo("paperconnekt", "1.0", instructions = "Use the tools."),
        tools = { listOf(echo, failing) },
        clock = { 42L },
        onActivity = { activity += it },
    )

    private suspend fun call(message: String): JsonObject = Json.parseToJsonElement(server.handle(message)!!).jsonObject

    @Test
    fun initializeNegotiatesTheVersionAndRecordsTheClient() = runTest {
        val response = call(
            """{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-03-26",""" +
                """"capabilities":{},"clientInfo":{"name":"claude","version":"1.2"}}}""",
        )
        val result = response["result"]!!.jsonObject
        assertEquals("2025-03-26", result["protocolVersion"]!!.jsonPrimitive.content)
        assertEquals("paperconnekt", result["serverInfo"]!!.jsonObject["name"]!!.jsonPrimitive.content)
        assertEquals("Use the tools.", result["instructions"]!!.jsonPrimitive.content)
        assertTrue("tools" in result["capabilities"]!!.jsonObject)
        assertEquals("claude 1.2", server.clientName)

        val unknownVersion = call("""{"jsonrpc":"2.0","id":2,"method":"initialize","params":{"protocolVersion":"1999-01-01"}}""")
        assertEquals(McpProtocol.LATEST_VERSION, unknownVersion["result"]!!.jsonObject["protocolVersion"]!!.jsonPrimitive.content)
    }

    @Test
    fun notificationsGetNoResponse() = runTest {
        assertNull(server.handle("""{"jsonrpc":"2.0","method":"notifications/initialized"}"""))
    }

    @Test
    fun listsToolsWithSchemasAndHints() = runTest {
        val tools = call("""{"jsonrpc":"2.0","id":"a","method":"tools/list"}""")["result"]!!.jsonObject["tools"]!!.jsonArray
        assertEquals(listOf("echo", "boom"), tools.map { it.jsonObject["name"]!!.jsonPrimitive.content })
        val annotations = tools[1].jsonObject["annotations"]!!.jsonObject
        assertEquals("false", annotations["readOnlyHint"]!!.jsonPrimitive.content)
        assertEquals("object", tools[0].jsonObject["inputSchema"]!!.jsonObject["type"]!!.jsonPrimitive.content)
    }

    @Test
    fun callsToolsAndLogsActivity() = runTest {
        val ok = call("""{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"echo","arguments":{"text":"hi"}}}""")
        val result = ok["result"]!!.jsonObject
        assertEquals("hi", result["content"]!!.jsonArray[0].jsonObject["text"]!!.jsonPrimitive.content)
        assertEquals("false", result["isError"]!!.jsonPrimitive.content)
        assertEquals(3, ok["id"]!!.jsonPrimitive.int)
        assertEquals(McpActivity(42L, null, "tools/call", "echo", true, activity.single().durationMillis, "text=hi"), activity.single())
    }

    @Test
    fun toolFailuresAreResultsNotProtocolErrors() = runTest {
        val thrown = call("""{"jsonrpc":"2.0","id":4,"method":"tools/call","params":{"name":"boom"}}""")["result"]!!.jsonObject
        assertEquals("true", thrown["isError"]!!.jsonPrimitive.content)
        assertTrue(thrown["content"]!!.jsonArray[0].jsonObject["text"]!!.jsonPrimitive.content.contains("kaput"))
        val missing = call("""{"jsonrpc":"2.0","id":5,"method":"tools/call","params":{"name":"echo","arguments":{}}}""")
        assertEquals("true", missing["result"]!!.jsonObject["isError"]!!.jsonPrimitive.content)
        assertEquals(listOf(false, false), activity.map { it.ok })
    }

    @Test
    fun protocolErrors() = runTest {
        assertEquals(McpProtocol.PARSE_ERROR, errorCode(call("{not json")))
        assertEquals(McpProtocol.INVALID_REQUEST, errorCode(call("42")))
        assertEquals(McpProtocol.METHOD_NOT_FOUND, errorCode(call("""{"jsonrpc":"2.0","id":6,"method":"resources/list"}""")))
        assertEquals(
            McpProtocol.INVALID_PARAMS,
            errorCode(call("""{"jsonrpc":"2.0","id":7,"method":"tools/call","params":{"name":"nope"}}""")),
        )
        assertEquals(
            McpProtocol.INVALID_PARAMS,
            errorCode(call("""{"jsonrpc":"2.0","id":8,"method":"tools/call","params":{"name":"echo","arguments":[1]}}""")),
        )
    }

    @Test
    fun batchesAnswerEveryRequestButNotNotifications() = runTest {
        val response = Json.parseToJsonElement(
            server.handle(
                """[{"jsonrpc":"2.0","id":1,"method":"ping"},{"jsonrpc":"2.0","method":"notifications/initialized"},""" +
                    """{"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"echo","arguments":{"text":"x"}}}]""",
            )!!,
        ) as JsonArray
        assertEquals(listOf(1, 2), response.map { it.jsonObject["id"]!!.jsonPrimitive.int })
    }

    private fun errorCode(response: JsonObject) = response["error"]!!.jsonObject["code"]!!.jsonPrimitive.int
}
