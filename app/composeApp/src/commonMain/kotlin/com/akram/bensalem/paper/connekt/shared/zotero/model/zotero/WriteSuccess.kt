package com.akram.bensalem.paper.connekt.shared.zotero.model.zotero

import kotlinx.serialization.Serializable

@Serializable
data class WriteSuccess(
    val key: String,
    val version: Int? = null
)