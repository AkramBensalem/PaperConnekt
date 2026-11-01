package com.akram.bensalem.paper.connekt.feature.assistant

import kotlinx.datetime.TimeZone
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Instant

const val LOCAL_RESEARCH_ACCOUNT = "local-workspace"

class AssistantRepository(private val store: ResearchHistoryStore = syncedResearchHistoryStore()) {
    private companion object {
        const val GRAPH_USAGE_ACCOUNT = "local-graph-triage"
        val graphUsageMutex = Mutex()
    }

    suspend fun loadHistory(userId: String): ResearchHistory = loadUsageHistory(userId)

    suspend fun loadUsageHistory(userId: String?): ResearchHistory {
        val local = store.load(LOCAL_RESEARCH_ACCOUNT)
        val remote = userId
            ?.takeIf { it.isNotBlank() && it != LOCAL_RESEARCH_ACCOUNT }
            ?.let { store.load(it) }
            ?: ResearchHistory()
        val graphUsage = store.load(GRAPH_USAGE_ACCOUNT).graphUsage
        return mergeResearchHistory(local, remote).copy(graphUsage = graphUsage)
    }

    suspend fun saveHistory(userId: String, history: ResearchHistory) =
        store.save(userId, history.copy(graphUsage = emptyMap()))

    suspend fun recordWorkspaceOpen(timestamp: Long) {
        val history = store.load(LOCAL_RESEARCH_ACCOUNT)
        store.save(
            LOCAL_RESEARCH_ACCOUNT,
            history.recordWorkspaceOpen(timestamp),
        )
    }

    suspend fun recordGraphUsage(paperId: String, event: AiUsageEvent): Map<String, List<AiUsageEvent>> =
        graphUsageMutex.withLock {
            val history = store.load(GRAPH_USAGE_ACCOUNT)
            val updated = history.copy(graphUsage = history.graphUsage +
                (paperId to (history.graphUsage[paperId].orEmpty() + event)))
            store.save(GRAPH_USAGE_ACCOUNT, updated)
            updated.graphUsage
        }
}
