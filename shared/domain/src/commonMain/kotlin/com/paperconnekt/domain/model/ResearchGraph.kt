package com.paperconnekt.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class ResearchGraph(
    val id: String,
    val name: String,
    val paperIds: List<String> = emptyList(),
    val connections: List<GraphConnection> = emptyList(),
    val updatedAt: Instant? = null,
)

@Serializable
data class GraphConnection(
    val fromPaperId: String,
    val toPaperId: String,
    val type: ConnectionType,
)

@Serializable
enum class ConnectionType {
    CITATION,
    SHARED_REFERENCE,
    RELATED_TOPIC,
}

