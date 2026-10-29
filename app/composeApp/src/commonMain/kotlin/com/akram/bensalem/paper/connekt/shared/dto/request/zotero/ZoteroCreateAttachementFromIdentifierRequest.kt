package com.akram.bensalem.paper.connekt.shared.dto.request.zotero

import kotlinx.serialization.Serializable

@Serializable
data class ZoteroCreateAttachementFromIdentifierRequest(val identifier : String)