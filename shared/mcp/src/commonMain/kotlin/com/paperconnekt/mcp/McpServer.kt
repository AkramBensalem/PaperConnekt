package com.paperconnekt.mcp

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlin.concurrent.Volatile
import kotlin.time.TimeSource

/**
 * Transport-independent Model Context Protocol server: JSON-RPC 2.0 with the `tools` capability.
 *
 * It implements `initialize`, `ping`, `tools/list` and `tools/call`, accepts notifications, and answers anything else
 * with "method not found". Transports pass each received message to [handle] and send back the returned text, if
 * any. Requests in a batch run concurrently, and [handle] itself is safe to call from parallel requests, so a client
 * can run several tool calls at once.
 */
class McpServer(
    private val info: McpServerInfo,
    private val tools: suspend () -> List<McpTool>,
    private val clock: () -> Long = { 0L },
    private val onActivity: (McpActivity) -> Unit = {},
) {
    @Volatile
    var clientName: String? = null
        private set

    /** Handles one transport message; returns the response text, or null when nothing must be sent back. */
    suspend fun handle(message: String): String? {
        val element = try {
            json.parseToJsonElement(message)
        } catch (exception: SerializationException) {
            return error(JsonNull, McpProtocol.PARSE_ERROR, "Parse error: ${exception.message}").toString()
        } catch (exception: IllegalArgumentException) {
            return error(JsonNull, McpProtocol.PARSE_ERROR, "Parse error: ${exception.message}").toString()
        }
        return handle(element)?.toString()
    }

    suspend fun handle(element: JsonElement): JsonElement? = when (element) {
        is JsonArray -> {
            if (element.isEmpty()) error(JsonNull, McpProtocol.INVALID_REQUEST, "Empty batch.")
            else coroutineScope {
                element.map { async { handleSingle(it) } }.awaitAll().filterNotNull()
                    .takeIf { it.isNotEmpty() }?.let(::JsonArray)
            }
        }
        else -> handleSingle(element)
    }

    private suspend fun handleSingle(element: JsonElement): JsonElement? {
        val message = element as? JsonObject
            ?: return error(JsonNull, McpProtocol.INVALID_REQUEST, "A JSON-RPC message must be an object.")
        if ((message["jsonrpc"] as? JsonPrimitive)?.content != "2.0") {
            return error(message["id"] ?: JsonNull, McpProtocol.INVALID_REQUEST, "jsonrpc must be \"2.0\".")
        }
        val id = message["id"]
        val method = (message["method"] as? JsonPrimitive)?.takeIf { it.isString }?.content
        if (method == null) {
            // A response to a server request (this server sends none) or an invalid message without a method.
            return if (id == null || "result" in message || "error" in message) null
            else error(id, McpProtocol.INVALID_REQUEST, "Missing method.")
        }
        if (id == null) return null // Notification such as notifications/initialized or notifications/cancelled.
        if (id !is JsonPrimitive || id is JsonNull) return error(JsonNull, McpProtocol.INVALID_REQUEST, "Invalid id.")
        val params = message["params"] as? JsonObject ?: JsonObject(emptyMap())
        return try {
            when (method) {
                "initialize" -> result(id, initialize(params))
                "ping" -> result(id, JsonObject(emptyMap()))
                "tools/list" -> result(id, listTools())
                "tools/call" -> callTool(id, params)
                else -> error(id, McpProtocol.METHOD_NOT_FOUND, "Method not found: $method")
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: InvalidParams) {
            error(id, McpProtocol.INVALID_PARAMS, exception.message ?: "Invalid params.")
        } catch (exception: Exception) {
            error(id, McpProtocol.INTERNAL_ERROR, exception.message ?: exception::class.simpleName ?: "Internal error.")
        }
    }

    private fun initialize(params: JsonObject): JsonObject {
        val requested = (params["protocolVersion"] as? JsonPrimitive)?.contentOrNull
        val version = requested?.takeIf { it in McpProtocol.SUPPORTED_VERSIONS } ?: McpProtocol.LATEST_VERSION
        val client = (params["clientInfo"] as? JsonObject)?.let { clientInfo ->
            listOfNotNull(
                (clientInfo["name"] as? JsonPrimitive)?.contentOrNull,
                (clientInfo["version"] as? JsonPrimitive)?.contentOrNull,
            ).joinToString(" ").takeIf(String::isNotBlank)
        }
        clientName = client
        onActivity(McpActivity(clock(), client, "initialize", detail = "protocol $version"))
        return buildJsonObject {
            put("protocolVersion", version)
            putJsonObject("capabilities") { putJsonObject("tools") { put("listChanged", false) } }
            putJsonObject("serverInfo") {
                put("name", info.name)
                put("version", info.version)
                info.title?.let { put("title", it) }
            }
            info.instructions?.let { put("instructions", it) }
        }
    }

    private suspend fun listTools(): JsonObject {
        val available = tools()
        return buildJsonObject {
            putJsonArray("tools") {
                available.forEach { tool ->
                    add(buildJsonObject {
                        put("name", tool.name)
                        tool.title?.let { put("title", it) }
                        put("description", tool.description)
                        put("inputSchema", tool.inputSchema)
                        putJsonObject("annotations") {
                            tool.title?.let { put("title", it) }
                            put("readOnlyHint", tool.readOnly)
                            put("destructiveHint", false)
                            put("idempotentHint", tool.readOnly)
                            put("openWorldHint", false)
                        }
                    })
                }
            }
        }
    }

    private suspend fun callTool(id: JsonElement, params: JsonObject): JsonElement {
        val name = (params["name"] as? JsonPrimitive)?.takeIf { it.isString }?.content
            ?: throw InvalidParams("tools/call needs a tool name.")
        val arguments = when (val raw = params["arguments"]) {
            null, JsonNull -> JsonObject(emptyMap())
            is JsonObject -> raw
            else -> throw InvalidParams("Tool arguments must be an object.")
        }
        val tool = tools().firstOrNull { it.name == name } ?: throw InvalidParams("Unknown tool: $name")
        val started = TimeSource.Monotonic.markNow()
        val outcome = try {
            tool.call(arguments)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            McpToolResult("Error: ${exception.message ?: exception::class.simpleName}", isError = true)
        }
        onActivity(
            McpActivity(
                epochMillis = clock(),
                client = clientName,
                method = "tools/call",
                tool = name,
                ok = !outcome.isError,
                durationMillis = started.elapsedNow().inWholeMilliseconds,
                detail = if (outcome.isError) outcome.text.take(160) else summarize(arguments),
            ),
        )
        return result(id, buildJsonObject {
            put("content", buildJsonArray {
                add(buildJsonObject {
                    put("type", "text")
                    put("text", outcome.text)
                })
            })
            put("isError", outcome.isError)
        })
    }

    private class InvalidParams(message: String) : Exception(message)

    private companion object {
        val json = Json { ignoreUnknownKeys = true }

        fun result(id: JsonElement, result: JsonElement) = buildJsonObject {
            put("jsonrpc", "2.0")
            put("id", id)
            put("result", result)
        }

        fun error(id: JsonElement, code: Int, message: String) = buildJsonObject {
            put("jsonrpc", "2.0")
            put("id", id)
            putJsonObject("error") {
                put("code", code)
                put("message", message)
            }
        }

        fun summarize(arguments: JsonObject): String =
            arguments.entries.joinToString(", ") { (key, value) ->
                val text = (value as? JsonPrimitive)?.takeIf { it.isString }?.content ?: value.toString()
                "$key=${if (text.length > 60) text.take(60) + "…" else text}"
            }.take(200)
    }
}
