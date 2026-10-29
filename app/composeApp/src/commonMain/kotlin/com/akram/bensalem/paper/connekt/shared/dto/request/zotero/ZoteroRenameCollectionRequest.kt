package com.akram.bensalem.paper.connekt.shared.dto.request.zotero

import kotlinx.serialization.Serializable

@Serializable
data class ZoteroRenameCollectionRequest(
    val collectionKey : String,
    val newName : String,
    val parentCollectionKey : String? = null
)