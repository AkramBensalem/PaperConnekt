package com.akram.bensalem.paper.connekt.shared.dto.request.zotero

import kotlinx.serialization.Serializable

@Serializable
data class ZoteroUplaodDocumentRequest(
    val name : String,
    val collectionKey : String,
    val parentCollectionKey : String? = null
)