package com.paperconnekt.mcp

import kotlinx.serialization.json.JsonObject

/** Identity and usage hints a client receives in the `initialize` result. */
data class McpServerInfo(
    val name: String,
    val version: String,
    val title: String? = null,
    /** Guidance the client may add to its model's context, such as which tools to call first. */
    val instructions: String? = null,
)

/**
 * A tool exposed over MCP. [inputSchema] is a JSON Schema object; [readOnly] becomes the `readOnlyHint` annotation so
 * clients can tell lookups from writes. [call] receives the client's arguments and must not throw for expected
 * failures: return [McpToolResult] with `isError = true` so the model can read the message and recover.
 */
class McpTool(
    val name: String,
    val description: String,
    val inputSchema: JsonObject,
    val title: String? = null,
    val readOnly: Boolean = true,
    val call: suspend (JsonObject) -> McpToolResult,
)

data class McpToolResult(val text: String, val isError: Boolean = false)

/** One handled request, for the transparency log the user sees in Settings. */
data class McpActivity(
    val epochMillis: Long,
    val client: String?,
    val method: String,
    val tool: String? = null,
    val ok: Boolean = true,
    val durationMillis: Long = 0,
    /** Short, human-readable summary: the tool arguments or the error. */
    val detail: String = "",
)

object McpProtocol {
    const val LATEST_VERSION = "2025-06-18"

    /** Newest first; a client asking for any of these gets it back, otherwise the server answers with the latest. */
    val SUPPORTED_VERSIONS = listOf(LATEST_VERSION, "2025-03-26", "2024-11-05")

    const val PARSE_ERROR = -32700
    const val INVALID_REQUEST = -32600
    const val METHOD_NOT_FOUND = -32601
    const val INVALID_PARAMS = -32602
    const val INTERNAL_ERROR = -32603
}
