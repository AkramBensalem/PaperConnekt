package com.akram.bensalem.paper.connekt.core.aiassistant

import ai.koog.agents.core.agent.AIAgent
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
import ai.koog.serialization.JSONArray
import ai.koog.serialization.JSONElement
import ai.koog.serialization.JSONObject
import ai.koog.serialization.JSONPrimitive
import ai.koog.serialization.JSONSerializer
import ai.koog.serialization.typeToken
import io.ktor.client.HttpClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
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

interface ResearchAgent {
    suspend fun respond(request: AgentRequest): String
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

/** Tool-calling research agent backed by Koog and the selected provider/model from API settings. */
class KoogResearchAgent(
    private val credentials: suspend () -> AgentCredentials,
    private val maxIterations: Int = 60
) : ResearchAgent {
    override suspend fun respond(request: AgentRequest): String {
        val keys = credentials()
        val selected = ResearchAgentModels.fromId(keys.selectedModel)
        val (executor, model) = when (selected.provider) {
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
        val agent = AIAgent(
            promptExecutor = executor,
            llmModel = model,
            toolRegistry = ToolRegistry { request.tools.forEach { tool(KoogAgentTool(it)) } },
            systemPrompt = request.systemPrompt,
            maxIterations = maxIterations
        )
        return agent.run(conversationInput(request.history, request.message, summary = request.summary)).trim()
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

private class KoogAgentTool(private val tool: AgentTool) : Tool<JSONObject, String>(
    argsType = typeToken<JSONObject>(),
    resultType = typeToken<String>(),
    descriptor = ToolDescriptor(
        name = tool.name,
        description = tool.description,
        requiredParameters = tool.parameters.filter { it.required }.map(::descriptor),
        optionalParameters = tool.parameters.filterNot { it.required }.map(::descriptor)
    )
) {
    override suspend fun execute(args: JSONObject): String = try {
        tool.execute(AgentToolArguments(args.entries.mapValues { it.value.toKotlin() }))
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        "Error: ${exception.message ?: exception::class.simpleName}"
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
