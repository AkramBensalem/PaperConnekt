package com.akram.bensalem.paper.connekt.feature.assistant

import com.akram.bensalem.paper.connekt.shared.zotero.model.zotero.ZoteroItem

@kotlinx.serialization.Serializable
data class ZoteroPdfTarget(val item: ZoteroItem, val attachment: ZoteroItem, val groupId: String? = null)
