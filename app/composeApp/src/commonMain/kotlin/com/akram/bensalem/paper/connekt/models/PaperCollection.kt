package com.akram.bensalem.paper.connekt.models

import kotlinx.serialization.Serializable

@Serializable
data class PaperCollection(
    val id: String,
    val name: String,
    val papers: List<Paper> = emptyList(),
    val subCollections: List<PaperCollection> = emptyList()
) {
    fun allPapersCount(): Int {
        return papers.size + subCollections.sumOf { it.allPapersCount() }
    }
}
