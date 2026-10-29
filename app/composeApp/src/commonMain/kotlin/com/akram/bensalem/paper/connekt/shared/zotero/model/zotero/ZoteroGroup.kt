package com.akram.bensalem.paper.connekt.shared.zotero.model.zotero

import kotlinx.serialization.Serializable

@Serializable
data class ZoteroGroup(
    val id: Int,
    val data: GroupData
) {
    @Serializable
    data class GroupData(val name: String)
}
