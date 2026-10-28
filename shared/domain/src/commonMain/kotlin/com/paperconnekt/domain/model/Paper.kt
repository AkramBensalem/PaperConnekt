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
)

