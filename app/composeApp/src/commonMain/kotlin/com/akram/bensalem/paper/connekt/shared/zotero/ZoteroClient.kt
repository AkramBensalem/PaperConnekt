package com.akram.bensalem.paper.connekt.shared.zotero

import com.akram.bensalem.paper.connekt.models.ZoteroConfig
import com.akram.bensalem.paper.connekt.shared.dto.request.zotero.ZoteroImportDocumentRequest
import com.akram.bensalem.paper.connekt.shared.dto.response.zotero.ZoteroCollectionResponse
import com.akram.bensalem.paper.connekt.shared.zotero.model.zotero.ZoteroCollection
import com.akram.bensalem.paper.connekt.shared.zotero.model.zotero.ZoteroItem
import com.akram.bensalem.paper.connekt.shared.zotero.model.zotero.ZoteroGroup
import kotlinx.serialization.json.JsonObject

data class ZoteroItemUpdate(val key: String, val version: Int, val fields: Map<String, String>)

data class ZoteroBatchWriteResult(
    val updated: Set<String> = emptySet(),
    val unchanged: Set<String> = emptySet(),
    /** Failure message per item key. */
    val failed: Map<String, String> = emptyMap()
)

/** High-level client for the Zotero Web API. */
interface ZoteroClient {
    suspend fun listGroups(config: ZoteroConfig): List<ZoteroGroup>
    suspend fun listTrashItems(config: ZoteroConfig): List<ZoteroItem>

    suspend fun fetchLibraryVersion(config: ZoteroConfig): Int
    /**
     * Checks connectivity and credentials by querying a lightweight endpoint.
     * Returns true if the API responds with 200.
     */
    suspend fun checkConnectivity(config: ZoteroConfig): Boolean

    /** Lists all collections (folders) in the selected user or group library, following pagination. */
    suspend fun listCollections(config: ZoteroConfig): List<ZoteroCollection>

    /** Lists all top-level library items, including items outside collections. */
    suspend fun listLibraryItems(config: ZoteroConfig): List<ZoteroItem>

    suspend fun listAttachments(config: ZoteroConfig): List<ZoteroItem>

    suspend fun downloadFile(config: ZoteroConfig, attachmentKey: String): ByteArray

    /** Lists top-level items in a collection (papers). */
    suspend fun listItemsOfCollection(config: ZoteroConfig, collectionKey: String): List<ZoteroItem>

    /** Lists all collections with their items. */
    suspend fun listCollectionsWithItems(config: ZoteroConfig): List<ZoteroCollectionResponse>

    /** Creates a new collection (optionally as a sub-collection of parentCollectionKey). Returns new collection key. */
    suspend fun createCollection(config: ZoteroConfig, name: String, parentCollectionKey: String? = null, version: Int): String

    /** Renames a collection; parent can be changed by passing parentCollectionKey. */
    suspend fun renameCollection(config: ZoteroConfig, collectionKey: String, newName: String, parentCollectionKey: String? = null, version: Int)

    /** Gets a specific collection by key. */
    suspend fun getCollection(config: ZoteroConfig, collectionKey: String): ZoteroCollection?

    /** Gets a specific collection by name. */
    suspend fun getCollectionByName(config: ZoteroConfig, name: String, parentKey : String? = null): ZoteroCollection?


    /** Deletes a collection (and its sub-collections). */
    suspend fun deleteCollection(config: ZoteroConfig, collectionKey: String, version: Int)

    /** Deletes an item (paper/file) by key. */
    suspend fun deleteItem(config: ZoteroConfig, itemKey: String)

    /** Moves an item (and its child attachments/notes) to the Zotero trash. */
    suspend fun trashItem(config: ZoteroConfig, itemKey: String, version: Int)

    /** Restores an item from the Zotero trash. */
    suspend fun restoreItem(config: ZoteroConfig, itemKey: String, version: Int)

    suspend fun setItemCollections(config: ZoteroConfig, itemKey: String, version: Int, collections: List<String>)

    /** Duplicates top-level metadata only, preserving fields not represented by the browsing model. */
    suspend fun duplicateItem(config: ZoteroConfig, itemKey: String, version: Int, collections: List<String>): String

    /**
     * Partially updates items in batches of 50 (only the given fields change). Each update carries the item's own
     * version, so an item edited elsewhere in the meantime is reported as failed instead of being overwritten.
     */
    suspend fun updateItems(config: ZoteroConfig, updates: List<ZoteroItemUpdate>): ZoteroBatchWriteResult

    suspend fun getAttachementTemplate(config: ZoteroConfig, linkMode: String): String

    suspend fun importDocuments(
        config: ZoteroConfig,
        requests : List<ZoteroImportDocumentRequest>
    ): String

    suspend fun uploadFileAuthorization(
        config: ZoteroConfig,
        itemKey: String,
        filename : String,
        fileSize : Int,
        mtime: Long,
        md5Hex: String,
    ): JsonObject

    suspend fun postFileS3(
        uploadUrl: String,
        contentType: String,
        uploadBody: ByteArray
    )

    suspend fun registerUploadedFile(
        config: ZoteroConfig,
        itemKey: String,
        uploadUrl: String,
        contentType: String,
        uploadKey: String,
    )

    suspend fun createItem(
        config: ZoteroConfig,
        item: com.akram.bensalem.paper.connekt.shared.dto.request.zotero.ZoteroItemRequest
    ): String

}
