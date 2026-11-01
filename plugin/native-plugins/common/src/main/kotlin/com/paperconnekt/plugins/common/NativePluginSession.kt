package com.paperconnekt.plugins.common

import com.paperconnekt.domain.model.Paper
import com.paperconnekt.plugin.api.NativePlugin
import com.paperconnekt.storage.LocalResearchStore

abstract class NativePluginSession(
    private val index: PluginPaperIndex = PluginPaperIndex(),
) : NativePlugin {
    fun load(papers: List<Paper>) {
        index.replaceAll(papers)
    }

    fun add(paper: Paper) {
        index.add(paper)
    }

    suspend fun loadFrom(store: LocalResearchStore) {
        load(store.papers.value)
    }

    suspend fun loadFrom(reader: NativeWorkspaceReader): PluginWorkspaceSnapshot {
        val snapshot = reader.read()
        load(snapshot.papers)
        return snapshot
    }

    override suspend fun search(query: String): List<Paper> {
        return index.search(query)
    }

    fun snapshot(): PluginWorkspaceSnapshot {
        return index.snapshot()
    }
}
