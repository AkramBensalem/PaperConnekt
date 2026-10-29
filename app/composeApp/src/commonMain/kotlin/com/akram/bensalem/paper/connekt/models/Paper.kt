package com.akram.bensalem.paper.connekt.models

import kotlinx.serialization.Serializable

@Serializable
data class Paper(
    val id: String,

    val title: String,
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
    val createdAt: Long,
    val updatedAt: Long,
    val visitedAt: Long
)