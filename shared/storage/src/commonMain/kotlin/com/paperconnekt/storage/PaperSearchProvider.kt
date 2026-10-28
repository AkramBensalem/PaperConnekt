package com.paperconnekt.storage

import com.paperconnekt.domain.model.Paper

interface PaperSearchProvider {
    suspend fun search(query: String): List<Paper>
}

class LocalPaperSearchProvider(
    private val store: LocalResearchStore,
) : PaperSearchProvider {
    override suspend fun search(query: String): List<Paper> {
        val normalizedQuery = query.trim().lowercase()
        if (normalizedQuery.isEmpty()) return store.papers.value

        return store.papers.value.filter { paper ->
            paper.title.lowercase().contains(normalizedQuery) ||
                paper.authors.any { it.lowercase().contains(normalizedQuery) } ||
                paper.doi?.lowercase()?.contains(normalizedQuery) == true ||
                paper.tags.any { it.lowercase().contains(normalizedQuery) }
        }
    }
}

class LocalFirstPaperSearchProvider(
    private val localProvider: LocalPaperSearchProvider,
    private val optionalRemoteProvider: PaperSearchProvider? = null,
) {
    suspend fun search(
        query: String,
        includeRemote: Boolean,
    ): List<Paper> {
        val localResults = localProvider.search(query)
        if (!includeRemote || optionalRemoteProvider == null || query.isBlank()) {
            return localResults
        }

        val remoteResults = optionalRemoteProvider.search(query)
        return (localResults + remoteResults)
            .distinctBy(Paper::id)
            .sortedByDescending(Paper::year)
    }
}
