package com.akram.bensalem.paper.connekt.shared.zotero.model.metadata

import kotlinx.serialization.Serializable

@Serializable
internal data class CrossRefSearchMessage(
    val items: List<CrossRefWork> = emptyList()
)