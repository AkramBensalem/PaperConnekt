package com.akram.bensalem.paper.connekt.shared.dto.request

import com.akram.bensalem.paper.connekt.models.Paper
import kotlinx.serialization.Serializable

@Serializable
data class PaperRequest(
    val title: String = "",
    val authors: List<String> = emptyList(),
    val abstract: String? = null,
    val keywords: List<String> = emptyList(),
    val tags: List<String> = emptyList(),

    // Publication info
    val publicationName: String? = null,
    val publicationDate: String? = null,
    val volume: String? = null,
    val issue: String? = null,
    val pages: String? = null,

    // Identifiers and links
    val doi: String? = null, // Digital Object Identifier
    val url: String? = null, // URL to the paper's page
    val filePath: String? = null, // Path to a local file (e.g., PDF)

    // Timestamps for app-specific tracking (epoch milliseconds)
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val visitedAt: Long = 0
)