package com.akram.bensalem.paper.connekt.shared.dto.request.zotero

import kotlinx.serialization.Serializable
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ZoteroImportDocumentRequest(
    val itemType: String = "attachment",
    val linkMode: String = "imported_file",
    val title : String = "",
    val filename: String,
    val contentType: String = "application/pdf",
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val collections : List<String> = emptyList(),
    val parentItem: String? = null,
)