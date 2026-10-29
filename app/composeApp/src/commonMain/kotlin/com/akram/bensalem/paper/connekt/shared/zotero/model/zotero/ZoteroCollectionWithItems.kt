package com.akram.bensalem.paper.connekt.shared.zotero.model.zotero

/** PaperCollection with its items. */
data class ZoteroCollectionWithItems(
    val collection: ZoteroCollection,
    val items: List<ZoteroItem>
)