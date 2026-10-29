package com.akram.bensalem.paper.connekt.shared.dto.request.zotero

import kotlinx.serialization.Serializable

/**
 * Base sealed interface for all Zotero item types.
 * This allows type-safe serialization without mixed-type map issues.
 */
@Serializable
sealed interface ZoteroItemRequest {
    val itemType: String
    val title: String?
    val creators: List<ZoteroCreator>
    val abstractNote: String?
    val date: String?
    val DOI: String?
    val url: String?
    val language: String?
    val tags: List<ZoteroTag>
    val collections: List<String>
}

/**
 * Journal Article item type
 */
@Serializable
data class ZoteroJournalArticleRequest(
    override val itemType: String = "journalArticle",
    override val title: String? = null,
    override val creators: List<ZoteroCreator> = emptyList(),
    override val abstractNote: String? = null,
    val publicationTitle: String? = null,
    val volume: String? = null,
    val issue: String? = null,
    val pages: String? = null,
    override val date: String? = null,
    override val DOI: String? = null,
    override val url: String? = null,
    override val language: String? = null,
    val ISSN: String? = null,
    val publisher: String? = null,
    override val tags: List<ZoteroTag> = emptyList(),
    override val collections: List<String> = emptyList(),
) : ZoteroItemRequest

/**
 * Book Section (chapter) item type
 */
@Serializable
data class ZoteroBookSectionRequest(
    override val itemType: String = "bookSection",
    override val title: String? = null,
    override val creators: List<ZoteroCreator> = emptyList(),
    override val abstractNote: String? = null,
    val bookTitle: String? = null,
    val pages: String? = null,
    val publisher: String? = null,
    val place: String? = null,
    override val date: String? = null,
    override val DOI: String? = null,
    override val url: String? = null,
    override val language: String? = null,
    val ISBN: String? = null,
    override val tags: List<ZoteroTag> = emptyList(),
    override val collections: List<String> = emptyList(),
) : ZoteroItemRequest

/**
 * Book item type
 */
@Serializable
data class ZoteroBookRequest(
    override val itemType: String = "book",
    override val title: String? = null,
    override val creators: List<ZoteroCreator> = emptyList(),
    override val abstractNote: String? = null,
    val publisher: String? = null,
    val place: String? = null,
    val edition: String? = null,
    val volume: String? = null,
    val numberOfVolumes: String? = null,
    val numPages: String? = null,
    override val date: String? = null,
    override val DOI: String? = null,
    override val url: String? = null,
    override val language: String? = null,
    val ISBN: String? = null,
    override val tags: List<ZoteroTag> = emptyList(),
    override val collections: List<String> = emptyList(),
) : ZoteroItemRequest

/**
 * Conference Paper item type
 */
@Serializable
data class ZoteroConferencePaperRequest(
    override val itemType: String = "conferencePaper",
    override val title: String? = null,
    override val creators: List<ZoteroCreator> = emptyList(),
    override val abstractNote: String? = null,
    val proceedingsTitle: String? = null,
    val conferenceName: String? = null,
    val place: String? = null,
    val pages: String? = null,
    val publisher: String? = null,
    override val date: String? = null,
    override val DOI: String? = null,
    override val url: String? = null,
    override val language: String? = null,
    val ISBN: String? = null,
    override val tags: List<ZoteroTag> = emptyList(),
    override val collections: List<String> = emptyList(),
) : ZoteroItemRequest

/**
 * Preprint item type
 */
@Serializable
data class ZoteroPreprintRequest(
    override val itemType: String = "preprint",
    override val title: String? = null,
    override val creators: List<ZoteroCreator> = emptyList(),
    override val abstractNote: String? = null,
    val repository: String? = null,
    val archiveID: String? = null,
    override val date: String? = null,
    override val DOI: String? = null,
    override val url: String? = null,
    override val language: String? = null,
    override val tags: List<ZoteroTag> = emptyList(),
    override val collections: List<String> = emptyList(),
) : ZoteroItemRequest

@Serializable
data class ZoteroCreator(
    val creatorType: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val name: String? = null
)

@Serializable
data class ZoteroTag(
    val tag: String,
    val type: Int = 0
)

/**
 * Legacy generic request - kept for backwards compatibility
 */
@Serializable
data class ZoteroCreateItemRequest(
    val itemType: String,
    val title: String? = null,
    val creators: List<ZoteroCreator> = emptyList(),
    val abstractNote: String? = null,
    val publicationTitle: String? = null,
    val volume: String? = null,
    val issue: String? = null,
    val pages: String? = null,
    val date: String? = null,
    val DOI: String? = null,
    val url: String? = null,
    val language: String? = null,
    val ISSN: String? = null,
    val tags: List<ZoteroTag> = emptyList(),
    val collections: List<String> = emptyList(),
)


