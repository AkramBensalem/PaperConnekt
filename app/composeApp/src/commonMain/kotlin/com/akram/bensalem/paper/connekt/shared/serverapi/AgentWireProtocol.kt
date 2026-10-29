package com.akram.bensalem.paper.connekt.shared.serverapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull

/**
 * Messages exchanged over [ServerRoutes.AGENT].
 *
 * The browser sends [Start] with tool descriptors; the server runs the agent and forwards every
 * tool invocation as [ToolCall] because tools drive the client UI. The browser answers with
 * [ToolResult] and the server finishes with [Final] or [Failure].
 */
@Serializable
sealed interface AgentWireMessage {
    @Serializable
    @SerialName("start")
    data class Start(
        val systemPrompt: String,
        val history: List<WireTurn> = emptyList(),
        val message: String,
        val summary: String? = null,
        val tools: List<WireTool> = emptyList(),
        /** Model id to force, or null to use the model selected in the API key settings. */
        val model: String? = null,
        val maxIterations: Int = 60,
    ) : AgentWireMessage

    @Serializable
    @SerialName("tool_call")
    data class ToolCall(val id: String, val name: String, val arguments: JsonObject) : AgentWireMessage

    @Serializable
    @SerialName("tool_result")
    data class ToolResult(val id: String, val result: String) : AgentWireMessage

    @Serializable
    @SerialName("final")
    data class Final(val text: String) : AgentWireMessage

    @Serializable
    @SerialName("failure")
    data class Failure(val message: String, val notConfigured: Boolean = false) : AgentWireMessage
}

@Serializable
data class WireTurn(val fromUser: Boolean, val text: String)

@Serializable
data class WireTool(
    val name: String,
    val description: String,
    val parameters: List<WireToolParameter> = emptyList(),
)

@Serializable
data class WireToolParameter(
    val name: String,
    val description: String,
    val type: String,
    val required: Boolean = true,
    val options: List<String> = emptyList(),
)

/** Encodes tool-call argument values (strings, numbers, booleans, lists, maps) as JSON. */
fun Map<String, Any?>.toJsonObject(): JsonObject = JsonObject(mapValues { (_, value) -> value.toJsonElement() })

private fun Any?.toJsonElement(): JsonElement = when (this) {
    null -> JsonNull
    is JsonElement -> this
    is String -> JsonPrimitive(this)
    is Boolean -> JsonPrimitive(this)
    is Number -> JsonPrimitive(this)
    is Map<*, *> -> JsonObject(entries.associate { (key, value) -> key.toString() to value.toJsonElement() })
    is Iterable<*> -> JsonArray(map { it.toJsonElement() })
    is Array<*> -> JsonArray(map { it.toJsonElement() })
    else -> JsonPrimitive(toString())
}

/** Decodes arguments the same way the agent runtime does: strings stay strings, other primitives become Boolean/Double. */
fun JsonObject.toArgumentValues(): Map<String, Any?> = mapValues { (_, value) -> value.toArgumentValue() }

private fun JsonElement.toArgumentValue(): Any? = when (this) {
    is JsonNull -> null
    is JsonObject -> toArgumentValues()
    is JsonArray -> map { it.toArgumentValue() }
    is JsonPrimitive -> when {
        isString -> content
        else -> booleanOrNull ?: doubleOrNull ?: content
    }
}
