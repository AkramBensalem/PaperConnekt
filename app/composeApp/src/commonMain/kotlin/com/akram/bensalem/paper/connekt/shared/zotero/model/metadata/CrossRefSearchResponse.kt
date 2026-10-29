package com.akram.bensalem.paper.connekt.shared.zotero.model.metadata

import kotlinx.serialization.Serializable

@Serializable
internal data class CrossRefSearchResponse(
    val message: CrossRefSearchMessage? = null
)