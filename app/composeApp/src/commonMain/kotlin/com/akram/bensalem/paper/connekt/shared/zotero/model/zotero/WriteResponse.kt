package com.akram.bensalem.paper.connekt.shared.zotero.model.zotero

import kotlinx.serialization.Serializable

/** Response for write operations returns mapping by index. */
@Serializable
data class WriteResponse(
    val successful: Map<String, WriteSuccess> = emptyMap(),
    val success: Map<String, String> = emptyMap(),
    val unchanged: Map<String, String> = emptyMap(),
    val failed: Map<String, WriteError> = emptyMap()
)