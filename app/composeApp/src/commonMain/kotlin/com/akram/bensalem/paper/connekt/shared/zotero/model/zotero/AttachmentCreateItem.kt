package com.akram.bensalem.paper.connekt.shared.zotero.model.zotero

import kotlinx.serialization.Serializable

@Serializable
data class AttachmentCreateItem(
    val itemType: String = "attachment",
    val linkMode: String = "imported_file",
    val title: String? = null,
    val filename: String,
    val contentType: String = "application/pdf",
    val collections: List<String> = emptyList(),
    val parentItem: String? = null
)