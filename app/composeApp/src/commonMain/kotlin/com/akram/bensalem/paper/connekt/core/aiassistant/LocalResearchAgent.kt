package com.akram.bensalem.paper.connekt.core.aiassistant

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.session.AIAgentLLMWriteSession
import ai.koog.agents.core.dsl.builder.node
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.ReceivedToolResults
import ai.koog.agents.core.dsl.extension.nodeExecuteTools
import ai.koog.agents.core.dsl.extension.onTextMessage
import ai.koog.agents.core.dsl.extension.onToolCalls
import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.core.tools.ToolParameterDescriptor
import ai.koog.agents.core.tools.ToolParameterType
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.http.client.ktor.KtorKoogHttpClient
import ai.koog.prompt.executor.clients.mistralai.MistralAILLMClient
import ai.koog.prompt.executor.clients.mistralai.MistralAIModels
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.MessagePart
import ai.koog.prompt.streaming.StreamFrame
import ai.koog.prompt.streaming.toMessageResponse
import ai.koog.serialization.JSONArray
import ai.koog.serialization.JSONElement
import ai.koog.serialization.JSONObject
import ai.koog.serialization.JSONPrimitive
import ai.koog.serialization.JSONSerializer
import ai.koog.serialization.typeToken
import io.ktor.client.HttpClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.paperconnekt.secrets.SecretKey
import com.paperconnekt.secrets.SecretStore

enum class AgentParameterType { STRING, INTEGER, NUMBER, BOOLEAN, STRING_LIST, NUMBER_LIST }

data class AgentToolParameter(
    val name: String,
    val description: String,
    val type: AgentParameterType = AgentParameterType.STRING,
    val required: Boolean = true,
    val options: List<String> = emptyList()
)

/** Arguments decoded from an LLM tool call; numbers may arrive as strings, so accessors are lenient. */
class AgentToolArguments(val values: Map<String, Any?>) {
    fun string(name: String): String? = values[name]?.toString()?.trim()?.takeIf { it.isNotEmpty() && it != "null" }
    fun requireString(name: String): String = string(name) ?: throw IllegalArgumentException("Missing required argument '$name'.")
    fun float(name: String): Float? = when (val value = values[name]) {
        is Number -> value.toFloat()
        else -> value?.toString()?.trim()?.toFloatOrNull()
    }?.takeIf { it.isFinite() }
    fun int(name: String): Int? = float(name)?.toInt()
    fun requireInt(name: String): Int = int(name) ?: throw IllegalArgumentException("Missing numeric argument '$name'.")
    fun boolean(name: String): Boolean? = when (val value = values[name]) {
        is Boolean -> value
        else -> value?.toString()?.trim()?.lowercase()?.toBooleanStrictOrNull()
    }
    fun strings(name: String): List<String> = when (val value = values[name]) {
        null -> emptyList()
        is List<*> -> value.mapNotNull { it?.toString()?.trim()?.takeIf(String::isNotEmpty) }
        else -> value.toString().split(',').map(String::trim).filter(String::isNotEmpty)
    }
    fun floats(name: String): List<Float> = when (val value = values[name]) {
        null -> emptyList()
        is List<*> -> value.mapNotNull { (it as? Number)?.toFloat() ?: it?.toString()?.toFloatOrNull() }
        else -> value.toString().split(',').mapNotNull { it.trim().toFloatOrNull() }
    }.filter { it.isFinite() }
}

class AgentTool(
    val name: String,
    val description: String,
    val parameters: List<AgentToolParameter> = emptyList(),
    val execute: suspend (AgentToolArguments) -> String
)

enum class AgentRole { USER, ASSISTANT }

data class AgentTurn(val role: AgentRole, val text: String)

data class AgentRequest(
    val systemPrompt: String,
    val history: List<AgentTurn>,
    val message: String,
    val tools: List<AgentTool>,
    /** Summary of earlier turns that were compacted away from [history]. */
    val summary: String? = null
)

/** What the agent is doing while it answers; lets the UI stream text and show every step transparently. */
sealed interface AgentEvent {
    /** A model call started; text streamed before it belonged to an intermediate step. */
    data object StepStarted : AgentEvent
    data class TextDelta(val text: String) : AgentEvent
    data class ToolStarted(val callId: Int, val name: String, val arguments: Map<String, Any?>) : AgentEvent
    data class ToolFinished(val callId: Int, val name: String, val ok: Boolean) : AgentEvent
    /** Provider-reported token usage of one model call; counts are null when the provider does not report them. */
    data class Usage(val inputTokens: Int?, val outputTokens: Int?) : AgentEvent
    data class Completed(val text: String) : AgentEvent
}

interface ResearchAgent {
    suspend fun respond(request: AgentRequest): String

    /** Streams the run as [AgentEvent]s, ending with [AgentEvent.Completed]. Non-streaming agents emit only the final text. */
    fun stream(request: AgentRequest): Flow<AgentEvent> = flow { emit(AgentEvent.Completed(respond(request))) }
}

class AgentNotConfiguredException(message: String) : IllegalStateException(message)

data class ResearchAgentModel(
    val id: String,
    val label: String,
    val provider: ResearchAgentProvider,
)

enum class ResearchAgentProvider { OPEN_AI, MISTRAL }

object ResearchAgentModels {
    val GPT6_LUNA = ResearchAgentModel("gpt-6-luna", "GPT-6 Luna", ResearchAgentProvider.OPEN_AI)
    val GPT5_MINI = ResearchAgentModel("gpt-5-mini", "GPT-5 Mini", ResearchAgentProvider.OPEN_AI)
    val MISTRAL_MEDIUM_31 = ResearchAgentModel("mistral-medium-latest", "Mistral Medium 3.1", ResearchAgentProvider.MISTRAL)
    val all = listOf(GPT6_LUNA, GPT5_MINI, MISTRAL_MEDIUM_31)

    fun fromId(id: String): ResearchAgentModel = all.firstOrNull { it.id == id } ?: GPT6_LUNA
}

data class AgentCredentials(
    val openAiKey: String = "",
    val mistralKey: String = "",
    val selectedModel: String = ResearchAgentModels.GPT6_LUNA.id,
)

/**
 * Tool-calling research agent backed by Koog and the selected provider/model from API settings.
 *
 * Runs the [paperResearchStrategy] graph: streamed model calls, independent tool calls executed in parallel, and an
 * [AgentEvent] for every step so the reader can follow (and stop) the agent.
 */
class KoogResearchAgent(
    private val credentials: suspend () -> AgentCredentials,
    private val maxIterations: Int = 60
) : ResearchAgent {
    override suspend fun respond(request: AgentRequest): String =
        stream(request).filterIsInstance<AgentEvent.Completed>().first().text

    override fun stream(request: AgentRequest): Flow<AgentEvent> = channelFlow {
        val (executor, model) = providerFor(credentials())
        val toolCalls = CallCounter()
        val sink: suspend (AgentEvent) -> Unit = { send(it) }
        val agent = AIAgent(
            promptExecutor = executor,
            llmModel = model,
            strategy = paperResearchStrategy(sink),
            toolRegistry = ToolRegistry { request.tools.forEach { tool(KoogAgentTool(it, sink, toolCalls)) } },
            systemPrompt = request.systemPrompt,
            maxIterations = maxIterations
        )
        val reply = agent.run(conversationInput(request.history, request.message, summary = request.summary))
        send(AgentEvent.Completed(reply.trim()))
    }

    private fun providerFor(keys: AgentCredentials): Pair<MultiLLMPromptExecutor, LLModel> {
        val selected = ResearchAgentModels.fromId(keys.selectedModel)
        return when (selected.provider) {
            ResearchAgentProvider.OPEN_AI -> {
                if (keys.openAiKey.isBlank()) {
                    throw AgentNotConfiguredException("Add an OpenAI API key in Settings → API keys to use ${selected.label}.")
                }
                MultiLLMPromptExecutor(
                    OpenAILLMClient(
                        apiKey = keys.openAiKey.trim(),
                        httpClientFactory = KtorKoogHttpClient.Factory(HttpClient()),
                    )
                ) to
                    if (selected == ResearchAgentModels.GPT5_MINI) OpenAIModels.Chat.GPT5Mini else gpt6LunaModel
            }
            ResearchAgentProvider.MISTRAL -> {
                if (keys.mistralKey.isBlank()) {
                    throw AgentNotConfiguredException("Add a Mistral API key in Settings → API keys to use ${selected.label}.")
                }
                MultiLLMPromptExecutor(
                    MistralAILLMClient(
                        apiKey = keys.mistralKey.trim(),
                        httpClientFactory = KtorKoogHttpClient.Factory(HttpClient()),
                    )
                ) to MistralAIModels.Chat.MistralMedium31
            }
        }
    }

    private val gpt6LunaModel = LLModel(
        provider = LLMProvider.OpenAI,
        id = "gpt-6-luna",
        capabilities = listOf(
            LLMCapability.Schema.JSON.Basic,
            LLMCapability.Schema.JSON.Standard,
            LLMCapability.Speculation,
            LLMCapability.Tools,
            LLMCapability.ToolChoice,
            LLMCapability.Vision.Image,
            LLMCapability.Document,
            LLMCapability.Completion,
            LLMCapability.MultipleChoices,
            LLMCapability.OpenAIEndpoint.Responses,
            LLMCapability.Thinking,
        ),
        contextLength = 400_000,
        maxOutputTokens = 128_000,
    )
}

/** Folds earlier turns (and the summary of compacted ones) into the single-run input so follow-ups keep their context. */
fun conversationInput(history: List<AgentTurn>, message: String, maxTurns: Int = 12, summary: String? = null): String {
    val recent = history.takeLast(maxTurns).filter { it.text.isNotBlank() }
    val earlier = summary?.trim()?.takeIf { it.isNotEmpty() }
    if (recent.isEmpty() && earlier == null) return message
    return buildString {
        if (earlier != null) {
            appendLine("Summary of the earlier conversation:")
            appendLine(earlier)
            appendLine()
        }
        if (recent.isNotEmpty()) {
            appendLine(if (earlier != null) "Most recent messages:" else "Conversation so far:")
            recent.forEach { turn ->
                append(if (turn.role == AgentRole.USER) "User: " else "Assistant: ")
                appendLine(turn.text.take(4_000))
            }
            appendLine()
        }
        append("User: ").append(message)
    }
}

/**
 * The research agent's Koog graph:
 *
 * ```
 * start → call_llm ─(tool calls)→ execute_tools (parallel) → send_tool_results ─(tool calls)→ execute_tools …
 *             └─(text)→ finish                                      └─(text)→ finish
 * ```
 *
 * Both model nodes stream, so the reply appears token by token; independent tool calls of one step (for example
 * several search_paper queries) run concurrently.
 */
internal fun paperResearchStrategy(sink: suspend (AgentEvent) -> Unit) = strategy<String, String>("paper_research") {
    val callLlm by node<String, Message.Assistant>("call_llm") { input ->
        llm.writeSession {
            appendPrompt { user(input) }
            streamResponse(sink)
        }
    }
    val executeTools by nodeExecuteTools("execute_tools", parallel = true)
    val sendToolResults by node<ReceivedToolResults, Message.Assistant>("send_tool_results") { results ->
        llm.writeSession {
            appendPrompt { user { results.toolResults.forEach { toolResult(it.toMessagePart()) } } }
            streamResponse(sink)
        }
    }

    edge(nodeStart forwardTo callLlm)
    edge(callLlm forwardTo executeTools onToolCalls { true })
    edge(callLlm forwardTo nodeFinish onTextMessage { true })
    edge(executeTools forwardTo sendToolResults)
    edge(sendToolResults forwardTo executeTools onToolCalls { true })
    edge(sendToolResults forwardTo nodeFinish onTextMessage { true })
}

/**
 * Streams one model call, forwarding text deltas and usage to [sink], and records the assembled reply in the prompt
 * (streaming requests do not do that themselves). Falls back to a regular request if the provider cannot stream.
 */
private suspend fun AIAgentLLMWriteSession.streamResponse(sink: suspend (AgentEvent) -> Unit): Message.Assistant {
    sink(AgentEvent.StepStarted)
    val frames = mutableListOf<StreamFrame>()
    val streamed = StringBuilder()
    try {
        requestLLMStreaming().collect { frame ->
            frames += frame
            when (frame) {
                is StreamFrame.TextDelta -> if (frame.text.isNotEmpty()) {
                    streamed.append(frame.text)
                    sink(AgentEvent.TextDelta(frame.text))
                }
                is StreamFrame.End -> sink(AgentEvent.Usage(frame.metaInfo.inputTokensCount, frame.metaInfo.outputTokensCount))
                else -> Unit
            }
        }
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        if (frames.isNotEmpty()) throw exception
        val response = requestLLM()
        response.parts.filterIsInstance<MessagePart.Text>().forEach { sink(AgentEvent.TextDelta(it.text)) }
        sink(AgentEvent.Usage(response.metaInfo.inputTokensCount, response.metaInfo.outputTokensCount))
        return response.routable()
    }
    val response = frames.toMessageResponse().withStreamedText(streamed.toString()).routable()
    appendPrompt { message(response) }
    return response
}

/**
 * Some providers' frames assemble into a reply without its text (only deltas, no final text frame). The reply shown
 * to the user, and kept in the prompt, must be exactly what was streamed, so missing text is restored from the deltas.
 */
private fun Message.Assistant.withStreamedText(streamed: String): Message.Assistant {
    if (streamed.isBlank() || parts.filterIsInstance<MessagePart.Text>().any { it.text.isNotBlank() }) return this
    return copy(parts = listOf(MessagePart.Text(streamed)) + parts.filterNot { it is MessagePart.Text })
}

/** Guarantees a text or tool-call part so the graph always has an outgoing edge, even for an empty reply. */
private fun Message.Assistant.routable(): Message.Assistant =
    if (parts.any { it is MessagePart.Text || it is MessagePart.Tool.Call }) this
    else copy(parts = parts + MessagePart.Text(""))

/** Numbers tool calls of one run; safe to use from tools executing in parallel. */
internal class CallCounter {
    private val mutex = Mutex()
    private var last = 0
    suspend fun next(): Int = mutex.withLock { ++last }
}

private class KoogAgentTool(
    private val tool: AgentTool,
    private val sink: suspend (AgentEvent) -> Unit = {},
    private val calls: CallCounter = CallCounter(),
) : Tool<JSONObject, String>(
    argsType = typeToken<JSONObject>(),
    resultType = typeToken<String>(),
    descriptor = ToolDescriptor(
        name = tool.name,
        description = tool.description,
        requiredParameters = tool.parameters.filter { it.required }.map(::descriptor),
        optionalParameters = tool.parameters.filterNot { it.required }.map(::descriptor)
    )
) {
    override suspend fun execute(args: JSONObject): String {
        val arguments = args.entries.mapValues { it.value.toKotlin() }
        val callId = calls.next()
        sink(AgentEvent.ToolStarted(callId, tool.name, arguments))
        val result = try {
            tool.execute(AgentToolArguments(arguments))
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            "Error: ${exception.message ?: exception::class.simpleName}"
        }
        sink(AgentEvent.ToolFinished(callId, tool.name, ok = !result.startsWith("Error:")))
        return result
    }

    override fun encodeResultToString(result: String, serializer: JSONSerializer): String = result
}

private fun descriptor(parameter: AgentToolParameter) = ToolParameterDescriptor(
    name = parameter.name,
    description = parameter.description,
    type = when {
        parameter.options.isNotEmpty() -> ToolParameterType.Enum(parameter.options.toTypedArray())
        else -> when (parameter.type) {
            AgentParameterType.STRING -> ToolParameterType.String
            AgentParameterType.INTEGER -> ToolParameterType.Integer
            AgentParameterType.NUMBER -> ToolParameterType.Float
            AgentParameterType.BOOLEAN -> ToolParameterType.Boolean
            AgentParameterType.STRING_LIST -> ToolParameterType.List(ToolParameterType.String)
            AgentParameterType.NUMBER_LIST -> ToolParameterType.List(ToolParameterType.Float)
        }
    }
)

private fun JSONElement.toKotlin(): Any? = when (this) {
    is JSONObject -> entries.mapValues { it.value.toKotlin() }
    is JSONArray -> elements.map { it.toKotlin() }
    is JSONPrimitive -> when {
        contentOrNull == null -> null
        isString -> content
        else -> booleanOrNull ?: doubleOrNull ?: content
    }
}

/** Agent running Koog in this process with the credentials saved in the API key settings. */
fun localResearchAgent(
    secretStore: SecretStore? = null,
    modelOverride: String? = null,
    maxIterations: Int = DEFAULT_AGENT_MAX_ITERATIONS,
): ResearchAgent = KoogResearchAgent({
    val selectedModel = modelOverride ?: ResearchAgentModels.GPT6_LUNA.id
    AgentCredentials(
        openAiKey = secretStore?.read(SecretKey.OPENAI).orEmpty(),
        mistralKey = secretStore?.read(SecretKey.MISTRAL).orEmpty(),
        selectedModel = selectedModel,
    )
}, maxIterations)

const val DEFAULT_AGENT_MAX_ITERATIONS = 60


fun createResearchAgent(secretStore: SecretStore? = null): ResearchAgent = localResearchAgent(secretStore)

enum class PaperAiTask { TRIAGE, RELEVANCE_SCORING }

data class PaperTriageUsage(
    val selectedPaperId: String,
    val estimatedInputTokens: Long = 0,
    val estimatedOutputTokens: Long = 0,
    val model: String = "",
    val task: PaperAiTask = PaperAiTask.TRIAGE,
)
