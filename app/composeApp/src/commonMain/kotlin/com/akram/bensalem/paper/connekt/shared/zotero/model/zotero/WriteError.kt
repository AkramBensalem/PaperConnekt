package com.akram.bensalem.paper.connekt.shared.zotero.model.zotero

import kotlinx.serialization.Serializable

@Serializable
data class WriteError(
    val code: Int,
    val message: String
)