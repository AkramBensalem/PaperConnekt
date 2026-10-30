package com.akram.bensalem.paper.connekt.shared.zotero.model.zotero

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import com.akram.bensalem.paper.connekt.shared.dto.request.zotero.ZoteroCreator
import com.akram.bensalem.paper.connekt.shared.dto.request.zotero.ZoteroTag
import com.akram.bensalem.paper.connekt.shared.dto.request.zotero.StringOrFalseAsNullSerializer

@Serializable
data class ZoteroItem(
    val key: String,
    val version: Int? = null,
    val data: ItemData
) {
    @Serializable
    data class ItemData(
        val itemType: String,
        val title: String? = null,
        val collections: List<String> = emptyList(),
        @Serializable(with = StringOrFalseAsNullSerializer::class)
        val parentItem: String? = null,
        val linkMode: String? = null,
        val contentType: String? = null,
        val filename: String? = null,
        val creators: List<ZoteroCreator> = emptyList(),
        val tags: List<ZoteroTag> = emptyList(),
        val abstractNote: String? = null,
        val publicationTitle: String? = null,
        val date: String? = null,
        val volume: String? = null,
        val issue: String? = null,
        val pages: String? = null,
        @SerialName("DOI") val doi: String? = null,
        val url: String? = null,
        val md5: String? = null,
        val extra: String? = null,
        val dateAdded: String? = null,
        val dateModified: String? = null
    )
}