package com.akram.bensalem.paper.connekt.feature.assistant

/** Local contest storage replaces the reference app's account synchronization boundary. */
fun syncedResearchHistoryStore(): ResearchHistoryStore = defaultResearchHistoryStore()
fun syncedPaperAgentChatStore(): PaperAgentChatStore = defaultPaperAgentChatStore()
fun syncedIdeaNoteStore(): IdeaNoteStore = defaultIdeaNoteStore()

fun mergeResearchHistory(local: ResearchHistory, remote: ResearchHistory): ResearchHistory =
    ResearchHistory(
        papers = (local.papers + remote.papers).groupBy { it.id }.values
            .map { it.maxBy { paper -> paper.lastOpenedAt } }
            .sortedByDescending { it.lastOpenedAt },
        dailyOpens = (local.dailyOpens.keys + remote.dailyOpens.keys).associateWith { key ->
            maxOf(local.dailyOpens[key] ?: 0, remote.dailyOpens[key] ?: 0)
        },
        graphUsage = local.graphUsage + remote.graphUsage,
    )

fun mergeConversations(
    local: List<PaperAgentConversation>,
    remote: List<PaperAgentConversation>,
): List<PaperAgentConversation> = (local + remote).groupBy { it.id }.values
    .map { it.maxBy { conversation -> conversation.updatedAt } }
    .sortedByDescending { it.updatedAt }
