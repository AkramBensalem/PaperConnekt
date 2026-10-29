package com.akram.bensalem.paper.connekt.shared.zotero.model.zotero

import com.akram.bensalem.paper.connekt.shared.dto.request.zotero.StringOrFalseAsNullSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Request model for creating collections. Zotero expects an array of objects. */
@Serializable
data class CreateCollectionRequest(
    val name: String,
    @SerialName("parentCollection")
    @Serializable(with = StringOrFalseAsNullSerializer::class)
    val parentCollectionKey: String? = null
)