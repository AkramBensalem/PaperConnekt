package com.akram.bensalem.paper.connekt.shared.zotero.model.zotero

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CommitResponse(
    @SerialName("itemKey") val itemKey: String,
    @SerialName("version") val version: Int
)