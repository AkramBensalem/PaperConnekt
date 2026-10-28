package com.paperconnekt.plugins.common

import com.paperconnekt.domain.model.Paper
import kotlinx.serialization.Serializable

class PluginPaperIndex(
    initialPapers: List<Paper> = emptyList(),
) {
    private val papers = LinkedHashMap<String, Paper>()

    init {
        initialPapers.forEach { paper -> papers[paper.id] = paper }
    }

    fun replaceAll(newPapers: List<Paper>) {
        papers.clear()
        newPapers.forEach { paper -> papers[paper.id] = paper }
    }

    fun add(paper: Paper) {
        papers[paper.id] = paper
    }

    fun search(query: String): List<Paper> {
        val normalizedQuery = query.trim().lowercase()
        if (normalizedQuery.isEmpty()) return papers.values.toList()

        return papers.values.filter { paper ->
            paper.title.lowercase().contains(normalizedQuery) ||
                paper.authors.any { it.lowercase().contains(normalizedQuery) }
        }
    }

    fun snapshot(): PluginWorkspaceSnapshot {
        return PluginWorkspaceSnapshot(papers.values.toList())
    }
}

@Serializable
data class PluginWorkspaceSnapshot(
    val papers: List<Paper> = emptyList(),
)

