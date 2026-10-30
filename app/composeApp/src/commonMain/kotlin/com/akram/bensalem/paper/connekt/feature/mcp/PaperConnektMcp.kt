package com.akram.bensalem.paper.connekt.feature.mcp

import com.akram.bensalem.paper.connekt.core.aiassistant.AgentParameterType
import com.akram.bensalem.paper.connekt.core.aiassistant.AgentTool
import com.akram.bensalem.paper.connekt.core.aiassistant.AgentToolArguments
import com.akram.bensalem.paper.connekt.feature.assistant.IdeaNoteStore
import com.paperconnekt.mcp.McpActivity
import com.paperconnekt.mcp.McpServer
import com.paperconnekt.mcp.McpServerInfo
import com.paperconnekt.mcp.McpTool
import com.paperconnekt.mcp.McpToolResult
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * Builds PaperConnekt's MCP server over [library]. Lookup tools are always served; `add_idea_note` is only listed
 * when [allowIdeaWrites] returns true at the time of the request, so the Settings switch takes effect immediately.
 */
fun paperConnektMcpServer(
    library: PaperLibrary,
    ideas: IdeaNoteStore,
    allowIdeaWrites: () -> Boolean,
    clock: () -> Long = { kotlin.time.Clock.System.now().toEpochMilliseconds() },
    onActivity: (McpActivity) -> Unit = {},
): McpServer {
    val readTools = paperLibraryTools(library).map { it.toMcpTool(readOnly = true) }
    val writeTools = paperLibraryTools(library, ideas)
        .filter { it.name in PaperLibraryWriteTools }
        .map { it.toMcpTool(readOnly = false) }
    return McpServer(
        info = McpServerInfo(
            name = "paperconnekt",
            version = "1.0.0",
            title = "PaperConnekt research library",
            instructions = MCP_INSTRUCTIONS,
        ),
        tools = { if (allowIdeaWrites()) readTools + writeTools else readTools },
        clock = clock,
        onActivity = onActivity,
    )
}

/** Serves an in-app [AgentTool] over MCP: its parameters become a JSON Schema and "Error:" results become tool errors. */
fun AgentTool.toMcpTool(readOnly: Boolean = true): McpTool = McpTool(
    name = name,
    description = description,
    inputSchema = inputSchema(),
    readOnly = readOnly,
) { arguments ->
    try {
        val text = execute(AgentToolArguments(arguments.mapValues { it.value.toKotlin() }))
        McpToolResult(text, isError = text.startsWith("Error:"))
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: IllegalArgumentException) {
        McpToolResult(exception.message ?: "Invalid arguments.", isError = true)
    }
}

internal fun AgentTool.inputSchema(): JsonObject = buildJsonObject {
    put("type", "object")
    putJsonObject("properties") {
        parameters.forEach { parameter ->
            putJsonObject(parameter.name) {
                when (parameter.type) {
                    AgentParameterType.STRING -> put("type", "string")
                    AgentParameterType.INTEGER -> put("type", "integer")
                    AgentParameterType.NUMBER -> put("type", "number")
                    AgentParameterType.BOOLEAN -> put("type", "boolean")
                    AgentParameterType.STRING_LIST -> {
                        put("type", "array")
                        putJsonObject("items") { put("type", "string") }
                    }
                    AgentParameterType.NUMBER_LIST -> {
                        put("type", "array")
                        putJsonObject("items") { put("type", "number") }
                    }
                }
                put("description", parameter.description)
                if (parameter.options.isNotEmpty()) putJsonArray("enum") { parameter.options.forEach { add(JsonPrimitive(it)) } }
            }
        }
    }
    val required = parameters.filter { it.required }
    if (required.isNotEmpty()) putJsonArray("required") { required.forEach { add(JsonPrimitive(it.name)) } }
    put("additionalProperties", false)
}

private fun JsonElement.toKotlin(): Any? = when (this) {
    is JsonNull -> null
    is JsonObject -> mapValues { it.value.toKotlin() }
    is JsonArray -> map { it.toKotlin() }
    is JsonPrimitive -> when {
        isString -> content
        else -> booleanOrNull ?: longOrNull ?: doubleOrNull ?: content
    }
}

private const val MCP_INSTRUCTIONS =
    "PaperConnekt exposes the user's local research library. Work cheaply: call list_papers or search_library to find " +
        "papers, get_paper or get_outline for structure, search_paper for the passages that answer a question, and " +
        "read_chunks for their full text. Use read_pages only when passages are not enough. Cite the paper ID, page " +
        "and chunk ID of every claim. Papers become searchable after the user opens them once in PaperConnekt."
