package com.akram.bensalem.paper.connekt.feature.assistant

/** Local contest storage replaces the reference app's account synchronization boundary. */
fun syncedResearchHistoryStore(): ResearchHistoryStore = defaultResearchHistoryStore()
fun syncedPaperAgentChatStore(): PaperAgentChatStore = defaultPaperAgentChatStore()
fun syncedIdeaNoteStore(): IdeaNoteStore = defaultIdeaNoteStore()

fun mergeResearchHistory(local: ResearchHistory, remote: ResearchHistory): ResearchHistory =
    ResearchHistory(
        papers = (local.papers + remote.papers).groupBy { it.id }.values
            .map { entries ->
                entries.maxBy { paper -> paper.lastOpenedAt }.copy(
                    openCount = entries.sumOf { it.openCount },
                    totalStudyMillis = entries.sumOf { it.totalStudyMillis },
                    dailyStudyMillis = entries
                        .flatMap { it.dailyStudyMillis.entries }
                        .groupingBy { it.key }
                        .fold(0L) { total, entry -> total + entry.value },
                    usage = entries.flatMap { it.usage }.distinctBy { event ->
                        "${event.timestamp}:${event.service}:${event.model}:${event.inputUnits}:${event.outputUnits}"
                    },
                )
            }
            .sortedByDescending { it.lastOpenedAt },
        dailyOpens = (local.dailyOpens.keys + remote.dailyOpens.keys).associateWith { key ->
            (local.dailyOpens[key] ?: 0) + (remote.dailyOpens[key] ?: 0)
        },
        graphUsage = (local.graphUsage.keys + remote.graphUsage.keys).associateWith { key ->
            local.graphUsage[key].orEmpty() + remote.graphUsage[key].orEmpty()
        },
        unassignedUsage = local.unassignedUsage + remote.unassignedUsage,
    )

fun mergeConversations(
    local: List<PaperAgentConversation>,
    remote: List<PaperAgentConversation>,
): List<PaperAgentConversation> = (local + remote).groupBy { it.id }.values
    .map { it.maxBy { conversation -> conversation.updatedAt } }
    .sortedByDescending { it.updatedAt }
