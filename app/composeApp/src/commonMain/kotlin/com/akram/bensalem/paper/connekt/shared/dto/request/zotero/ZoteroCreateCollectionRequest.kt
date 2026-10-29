package com.akram.bensalem.paper.connekt.shared.dto.request.zotero

import kotlinx.serialization.Serializable

@Serializable
data class ZoteroCreateCollectionRequest(
    val name : String,
    val parentCollectionKey : String? = null
)