package com.akram.bensalem.paper.connekt.core.aiassistant

/** Contest-local replacement for the reference app's provider agent. */
enum class AgentParameterType { STRING, INTEGER, NUMBER, BOOLEAN, STRING_LIST, NUMBER_LIST }

data class AgentToolParameter(
    val name: String,
    val description: String,
    val type: AgentParameterType = AgentParameterType.STRING,
    val required: Boolean = true,
    val options: List<String> = emptyList(),
)

class AgentToolArguments(val values: Map<String, Any?>) {
    fun string(name: String): String? = values[name]?.toString()?.takeIf { it.isNotBlank() }
    fun requireString(name: String) = string(name) ?: error("Missing required argument '$name'.")
    fun float(name: String): Float? = (values[name] as? Number)?.toFloat() ?: values[name]?.toString()?.toFloatOrNull()
    fun int(name: String): Int? = float(name)?.toInt()
    fun requireInt(name: String) = int(name) ?: error("Missing numeric argument '$name'.")
    fun boolean(name: String): Boolean? = values[name] as? Boolean ?: values[name]?.toString()?.toBooleanStrictOrNull()
    fun strings(name: String): List<String> = when (val value = values[name]) {
        is List<*> -> value.mapNotNull { it?.toString() }
        null -> emptyList()
        else -> value.toString().split(',').map(String::trim).filter(String::isNotEmpty)
    }
    fun floats(name: String): List<Float> = strings(name).mapNotNull(String::toFloatOrNull)
}

class AgentTool(
    val name: String,
    val description: String,
    val parameters: List<AgentToolParameter> = emptyList(),
    val execute: suspend (AgentToolArguments) -> String,
)

enum class AgentRole { USER, ASSISTANT }
data class AgentTurn(val role: AgentRole, val text: String)
data class AgentRequest(
    val systemPrompt: String,
    val history: List<AgentTurn>,
    val message: String,
    val tools: List<AgentTool>,
    val summary: String? = null,
)

class AgentNotConfiguredException(message: String) : IllegalStateException(message)

interface ResearchAgent {
    suspend fun respond(request: AgentRequest): String
}

data class ResearchAgentModel(val id: String, val label: String)

object ResearchAgentModels {
    val GPT6_LUNA = ResearchAgentModel("gpt-6-luna", "GPT-6 Luna")
    val GPT5_MINI = ResearchAgentModel("gpt-5-mini", "GPT-5 Mini")
    val MISTRAL_MEDIUM_31 = ResearchAgentModel("mistral-medium-latest", "Mistral Medium 3.1")
    val all = listOf(GPT6_LUNA, GPT5_MINI, MISTRAL_MEDIUM_31)
    fun fromId(id: String) = all.firstOrNull { it.id == id } ?: GPT6_LUNA
}

data class AgentCredentials(
    val openAiKey: String = "",
    val mistralKey: String = "",
    val selectedModel: String = ResearchAgentModels.GPT6_LUNA.id,
)

class LocalResearchAgent : ResearchAgent {
    override suspend fun respond(request: AgentRequest): String =
        throw AgentNotConfiguredException(
            "AI provider execution is not connected in this contest build. Saved keys alone do not enable AI responses yet."
        )
}

fun createResearchAgent(): ResearchAgent = LocalResearchAgent()
fun createReadingAgent(): ResearchAgent = LocalResearchAgent()

enum class PaperAiTask { TRIAGE, RELEVANCE_SCORING }
data class PaperTriageUsage(
    val selectedPaperId: String,
    val estimatedInputTokens: Long = 0,
    val estimatedOutputTokens: Long = 0,
    val model: String = "",
    val task: PaperAiTask = PaperAiTask.TRIAGE,
)
