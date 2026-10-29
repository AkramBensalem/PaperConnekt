package com.akram.bensalem.paper.connekt.shared.zotero.model.zotero

import com.akram.bensalem.paper.connekt.shared.dto.request.zotero.StringOrFalseAsNullSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Lightweight models mapping the subset of Zotero Web API we need. */

@Serializable
data class ZoteroCollection(
    val key: String,
    val version: Int,
    val data: CollectionData
) {
    @Serializable
    data class CollectionData(
        val name: String,
        @SerialName("parentCollection")
        @Serializable(with = StringOrFalseAsNullSerializer::class)
        val parentCollectionKey: String? = null,
        val deleted: Boolean = false
    )
}