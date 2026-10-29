package com.akram.bensalem.paper.connekt.shared.zotero

import kotlinx.serialization.Serializable

/** Unified metadata representation for a scholarly reference regardless of source (CrossRef, arXiv, OpenLibrary, etc.). */
@Serializable
data class ReferenceMetadata(
    val identifier: String,                 // Original identifier provided (DOI, arXiv ID, ISBN)
    val identifierType: String,             // doi | arxiv | isbn | unknown
    val title: String = "",
    val authors: List<String> = emptyList(),
    val abstract: String? = null,
    val keywords: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val publicationName: String? = null,    // Journal / Conference / Book title
    val publicationDate: String? = null,    // ISO-8601 (yyyy-MM-dd) partial allowed
    val volume: String? = null,
    val issue: String? = null,
    val pages: String? = null,
    val doi: String? = null,
    val url: String? = null,
    val isbn10: String? = null,
    val isbn13: String? = null,
    val rawSource: String? = null           // Raw JSON / XML (for debugging or future enrichment)
)

