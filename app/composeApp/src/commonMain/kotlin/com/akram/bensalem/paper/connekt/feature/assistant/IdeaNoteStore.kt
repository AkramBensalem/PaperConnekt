package com.akram.bensalem.paper.connekt.feature.assistant

import kotlinx.serialization.Serializable

@Serializable
data class Idea(
    val id: String,
    val title: String,
    val description: String,
    val x: Float = 0f,
    val y: Float = 0f,
    val pageColor: String = "cream"
)

interface IdeaNoteStore {
    suspend fun load(paperId: String): List<Idea>
    suspend fun save(paperId: String, ideas: List<Idea>)
}

class InMemoryIdeaNoteStore : IdeaNoteStore {
    private val ideasByPaper = mutableMapOf<String, List<Idea>>()

    override suspend fun load(paperId: String): List<Idea> = ideasByPaper[paperId].orEmpty()

    override suspend fun save(paperId: String, ideas: List<Idea>) {
        ideasByPaper[paperId] = ideas
    }
}

expect fun defaultIdeaNoteStore(): IdeaNoteStore
