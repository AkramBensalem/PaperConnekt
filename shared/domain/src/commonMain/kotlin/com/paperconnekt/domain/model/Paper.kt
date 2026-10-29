package com.paperconnekt.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Paper(
    val id: String,
    val title: String,
    val authors: List<String> = emptyList(),
    val abstractText: String? = null,
    val year: Int? = null,
    val pdfPath: String? = null,
    val completed: Boolean = false,
    val doi: String? = null,
    val tags: List<String> = emptyList(),
    val source: String = "Local",
    val sourceUrl: String? = null,
    val citationCount: Int? = null,
    val referenceCount: Int? = null,
    val citingCount: Int? = null,
    val archived: Boolean = false,
)
