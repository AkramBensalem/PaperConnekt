package com.paperconnekt.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class AssistantSession(
    val id: String,
    val title: String,
    val paperId: String? = null,
    val messages: List<AssistantMessage> = emptyList(),
    val updatedAt: Instant? = null,
)

@Serializable
data class AssistantMessage(
    val role: AssistantRole,
    val content: String,
)

@Serializable
enum class AssistantRole {
    USER,
    ASSISTANT,
}

