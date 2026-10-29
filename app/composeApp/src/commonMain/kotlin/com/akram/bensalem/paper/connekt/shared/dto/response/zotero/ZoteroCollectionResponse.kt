package com.akram.bensalem.paper.connekt.shared.dto.response.zotero

import kotlinx.serialization.Serializable

@Serializable
data class ZoteroCollectionResponse(
    val id : String,
    val version : Int?,
    val name: String,
    val items: List<ZoteroItemResponse> = emptyList(),
    val children:  List<ZoteroCollectionResponse> = emptyList()
)

@Serializable
data class ZoteroItemResponse(
    val key: String,
    val version: Int? = null,
    val itemType: String,
    val title: String? = null,
    val parentItem: String? = null,
    val linkMode: String? = null,
    val contentType: String? = null,
    val filename: String? = null
)