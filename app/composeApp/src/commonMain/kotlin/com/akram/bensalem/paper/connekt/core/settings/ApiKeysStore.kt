package com.akram.bensalem.paper.connekt.core.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class ApiKeys(
    val openAi: String = "",
    val mistral: String = "",
    val zotero: String = "",
    val researchModel: String = "gpt-6-luna",
)

interface ApiKeysStore {
    val apiKeys: StateFlow<ApiKeys>
    suspend fun save(keys: ApiKeys)
}

private class LocalApiKeysStore : ApiKeysStore {
    private val state = MutableStateFlow(ApiKeys())
    override val apiKeys: StateFlow<ApiKeys> = state
    override suspend fun save(keys: ApiKeys) { state.value = keys }
}

private val localApiKeysStore = LocalApiKeysStore()

fun createApiKeysStore(): ApiKeysStore = localApiKeysStore
