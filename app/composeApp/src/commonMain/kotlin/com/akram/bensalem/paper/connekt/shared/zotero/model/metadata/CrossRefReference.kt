package com.akram.bensalem.paper.connekt.shared.zotero.model.metadata

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CrossRefReference(
    @SerialName("key") val key: String? = null,
    @SerialName("DOI") val doi: String? = null,
    @SerialName("first-page") val firstPage: String? = null,
    @SerialName("article-title") val articleTitle: String? = null,
    @SerialName("volume") val volume: String? = null,
    @SerialName("author") val author: String? = null,
    @SerialName("year") val year: String? = null,
    @SerialName("journal-title") val journalTitle: String? = null,
    @SerialName("issue") val issue: String? = null,
    @SerialName("doi-asserted-by") val doiAssertedBy: String? = null,
)