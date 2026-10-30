package com.akram.bensalem.paper.connekt.feature.assistant

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant

@Serializable
data class StudiedPaper(
    val target: ZoteroPdfTarget,
    val firstOpenedAt: Long,
    val lastOpenedAt: Long,
    val openCount: Int = 1,
    val totalStudyMillis: Long = 0L,
    val dailyStudyMillis: Map<String, Long> = emptyMap(),
    val usage: List<AiUsageEvent> = emptyList()
) {
    val id: String get() = target.groupId?.let { "group:$it:${target.attachment.key}" }
        ?: "personal:${target.attachment.key}"
    val title: String get() = target.item.data.title?.takeIf(String::isNotBlank)
        ?: target.attachment.data.filename ?: "Untitled PDF"
    val authors: String get() = target.item.data.creators.joinToString(", ") {
        it.name ?: listOfNotNull(it.firstName, it.lastName).joinToString(" ")
    }
}

@Serializable
enum class AiUsageService(val label: String) {
    OCR("OCR"),
    CHAT("Chat"),
    LITERATURE_TRIAGE("Literature triage"),
    RELEVANCE_SCORING("Relevance scoring"),
    /** Paper passages embedded for semantic search; inputUnits are estimated tokens. */
    EMBEDDINGS("Paper embeddings"),
}

/** Conservative public-list-price estimates in USD; replace with provider-specific rates when configured. */
object AiUsagePricing {
    const val MISTRAL_OCR_PER_PAGE = 4.0 / 1_000.0
    const val MISTRAL_OCR_CACHED_PER_PAGE = 0.4 / 1_000.0
    const val GPT6_LUNA_INPUT_PER_MILLION_TOKENS = 0.10
    const val GPT6_LUNA_OUTPUT_PER_MILLION_TOKENS = 0.50
    const val GPT5_MINI_INPUT_PER_MILLION_TOKENS = 0.25
    const val GPT5_MINI_OUTPUT_PER_MILLION_TOKENS = 2.0
    const val MISTRAL_MEDIUM_31_INPUT_PER_MILLION_TOKENS = 0.50
    const val MISTRAL_MEDIUM_31_OUTPUT_PER_MILLION_TOKENS = 1.50
    const val MISTRAL_EMBED_PER_MILLION_TOKENS = 0.10
    const val OPENAI_EMBEDDING_3_SMALL_PER_MILLION_TOKENS = 0.02
}

private fun textAiCostUsd(model: String, inputUnits: Long, outputUnits: Long): Double {
    val (inputRate, outputRate) = when (model) {
        "gpt-6-luna" -> AiUsagePricing.GPT6_LUNA_INPUT_PER_MILLION_TOKENS to AiUsagePricing.GPT6_LUNA_OUTPUT_PER_MILLION_TOKENS
        "gpt-5-mini" -> AiUsagePricing.GPT5_MINI_INPUT_PER_MILLION_TOKENS to AiUsagePricing.GPT5_MINI_OUTPUT_PER_MILLION_TOKENS
        "mistral-medium-latest" -> AiUsagePricing.MISTRAL_MEDIUM_31_INPUT_PER_MILLION_TOKENS to AiUsagePricing.MISTRAL_MEDIUM_31_OUTPUT_PER_MILLION_TOKENS
        else -> AiUsagePricing.MISTRAL_MEDIUM_31_INPUT_PER_MILLION_TOKENS to AiUsagePricing.MISTRAL_MEDIUM_31_OUTPUT_PER_MILLION_TOKENS
    }
    return inputUnits.coerceAtLeast(0L) * inputRate / 1_000_000.0 +
        outputUnits.coerceAtLeast(0L) * outputRate / 1_000_000.0
}

@Serializable
data class AiUsageEvent(
    val service: AiUsageService,
    val timestamp: Long,
    val inputUnits: Long = 0L,
    val outputUnits: Long = 0L,
    val model: String = "",
    val cached: Boolean = false,
) {
    val estimatedCostUsd: Double
        get() = when (service) {
            AiUsageService.OCR -> inputUnits.coerceAtLeast(0L) *
                if (cached) AiUsagePricing.MISTRAL_OCR_CACHED_PER_PAGE else AiUsagePricing.MISTRAL_OCR_PER_PAGE
            AiUsageService.CHAT, AiUsageService.LITERATURE_TRIAGE, AiUsageService.RELEVANCE_SCORING ->
                textAiCostUsd(model, inputUnits, outputUnits)
            AiUsageService.EMBEDDINGS -> inputUnits.coerceAtLeast(0L) / 1_000_000.0 *
                if (model.startsWith("openai/")) AiUsagePricing.OPENAI_EMBEDDING_3_SMALL_PER_MILLION_TOKENS
                else AiUsagePricing.MISTRAL_EMBED_PER_MILLION_TOKENS
        }
}

data class AiUsageSummary(
    val events: List<AiUsageEvent> = emptyList(),
) {
    val totalCostUsd: Double get() = events.sumOf { it.estimatedCostUsd }
    val todayCostUsd: Double
        get() {
            val today = Instant.fromEpochMilliseconds(kotlin.time.Clock.System.now().toEpochMilliseconds())
                .toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
            return events.filter { event ->
                Instant.fromEpochMilliseconds(event.timestamp)
                    .toLocalDateTime(TimeZone.currentSystemDefault()).date.toString() == today
            }.sumOf { it.estimatedCostUsd }
        }
    val byService: Map<AiUsageService, Double>
        get() = events.groupingBy { it.service }.fold(0.0) { total, event -> total + event.estimatedCostUsd }
}

@Serializable
data class ResearchHistory(
    val papers: List<StudiedPaper> = emptyList(),
    val dailyOpens: Map<String, Int> = emptyMap(),
    val graphUsage: Map<String, List<AiUsageEvent>> = emptyMap()
) {
    fun record(target: ZoteroPdfTarget, timestamp: Long): ResearchHistory {
        val opened = StudiedPaper(target, timestamp, timestamp)
        val previous = papers.find { it.id == opened.id }
        val updated = opened.copy(
            firstOpenedAt = previous?.firstOpenedAt ?: timestamp,
            openCount = (previous?.openCount ?: 0) + 1,
            totalStudyMillis = previous?.totalStudyMillis ?: 0L,
            dailyStudyMillis = previous?.dailyStudyMillis ?: emptyMap(),
            usage = previous?.usage ?: emptyList()
        )
        val day = Instant.fromEpochMilliseconds(timestamp)
            .toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
        return copy(
            papers = (papers.filterNot { it.id == opened.id } + updated).sortedByDescending { it.lastOpenedAt },
            dailyOpens = dailyOpens + (day to ((dailyOpens[day] ?: 0) + 1))
        )
    }

    fun recordStudyTime(paperId: String, fromTimestamp: Long, toTimestamp: Long): ResearchHistory {
        if (toTimestamp <= fromTimestamp) return this
        val paper = papers.find { it.id == paperId } ?: return this
        val timeZone = TimeZone.currentSystemDefault()
        var cursor = fromTimestamp
        var dailyStudyMillis = paper.dailyStudyMillis
        while (cursor < toTimestamp) {
            val day = Instant.fromEpochMilliseconds(cursor)
                .toLocalDateTime(timeZone).date
            val nextDay = (day + DatePeriod(days = 1))
                .atStartOfDayIn(timeZone).toEpochMilliseconds()
            val intervalEnd = minOf(toTimestamp, nextDay)
            val duration = intervalEnd - cursor
            dailyStudyMillis = dailyStudyMillis + (day.toString() to ((dailyStudyMillis[day.toString()] ?: 0L) + duration))
            cursor = intervalEnd
        }
        val duration = toTimestamp - fromTimestamp
        return copy(papers = papers.map {
            if (it.id == paperId) it.copy(
                totalStudyMillis = it.totalStudyMillis + duration,
                dailyStudyMillis = dailyStudyMillis
            ) else it
        })
    }

    fun recordUsage(paperId: String?, event: AiUsageEvent): ResearchHistory {
        if (paperId == null) return this
        return copy(papers = papers.map { paper ->
            if (paper.id == paperId) paper.copy(usage = paper.usage + event) else paper
        })
    }

    val usage: List<AiUsageEvent>
        get() = papers.flatMap { it.usage } + graphUsage.values.flatten()

    val usageSummary: AiUsageSummary
        get() = AiUsageSummary(usage)
}

interface ResearchHistoryStore {
    suspend fun load(userId: String): ResearchHistory
    suspend fun save(userId: String, history: ResearchHistory)
}

class InMemoryResearchHistoryStore : ResearchHistoryStore {
    private val histories = mutableMapOf<String, ResearchHistory>()
    override suspend fun load(userId: String): ResearchHistory = histories[userId] ?: ResearchHistory()
    override suspend fun save(userId: String, history: ResearchHistory) {
        histories[userId] = history
    }
}

expect fun defaultResearchHistoryStore(): ResearchHistoryStore
