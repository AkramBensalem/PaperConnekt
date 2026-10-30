package com.akram.bensalem.paper.connekt.shared.zotero

import com.akram.bensalem.paper.connekt.models.ZoteroConfig
import com.akram.bensalem.paper.connekt.shared.dto.request.zotero.ZoteroCreator
import com.akram.bensalem.paper.connekt.shared.dto.request.zotero.ZoteroImportDocumentRequest
import com.akram.bensalem.paper.connekt.shared.dto.request.zotero.ZoteroItemRequest
import com.akram.bensalem.paper.connekt.shared.dto.request.zotero.ZoteroTag
import com.akram.bensalem.paper.connekt.shared.dto.response.zotero.ZoteroCollectionResponse
import com.akram.bensalem.paper.connekt.shared.dto.response.zotero.ZoteroItemResponse
import com.akram.bensalem.paper.connekt.shared.zotero.exception.ZoteroApiException
import com.akram.bensalem.paper.connekt.shared.zotero.model.zotero.CreateCollectionRequest
import com.akram.bensalem.paper.connekt.shared.zotero.model.zotero.WriteResponse
import com.akram.bensalem.paper.connekt.shared.zotero.model.zotero.ZoteroCollection
import com.akram.bensalem.paper.connekt.shared.zotero.model.zotero.ZoteroCollectionWithItems
import com.akram.bensalem.paper.connekt.shared.zotero.model.zotero.ZoteroItem
import com.akram.bensalem.paper.connekt.shared.zotero.model.zotero.ZoteroAnnotation
import com.akram.bensalem.paper.connekt.shared.zotero.model.zotero.ZoteroGroup
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

class KtorZoteroClient() : ZoteroClient {

    override suspend fun listGroups(config: ZoteroConfig): List<ZoteroGroup> =
        defaultHttpClient(config).use { http ->
            val groups = mutableListOf<ZoteroGroup>()
            var start = 0
            while (true) {
                val response = http.get("${config.userBase}/groups") {
                    parameter("format", "json")
                    parameter("limit", 100)
                    parameter("start", start)
                }
                if (response.status != HttpStatusCode.OK) {
                    throw ZoteroApiException("Could not load group libraries", response.status)
                }
                val page: List<ZoteroGroup> = response.body()
                groups += page
                if (page.size < 100) break
                start += 100
            }
            groups.distinctBy { it.id }.sortedBy { it.data.name.lowercase() }
        }

    override suspend fun listTrashItems(config: ZoteroConfig): List<ZoteroItem> =
        listItems(config, "/items/trash")

    suspend fun listAnnotations(config: ZoteroConfig, attachmentKey: String): List<ZoteroAnnotation> =
        defaultHttpClient(config).use { http ->
            val result = mutableListOf<ZoteroAnnotation>()
            var start = 0
            while (true) {
                val response = http.get("${config.base}/items/$attachmentKey/children") {
                    parameter("itemType", "annotation")
                    parameter("limit", 100)
                    parameter("start", start)
                }
                if (response.status != HttpStatusCode.OK) throw ZoteroApiException("Could not load PDF annotations", response.status)
                val page: List<ZoteroAnnotation> = response.body()
                result += page
                if (page.size < 100) break
                start += 100
            }
            result.distinctBy { it.key }
        }

    suspend fun createAnnotation(config: ZoteroConfig, data: JsonObject): String =
        defaultHttpClient(config).use { http ->
            val version = fetchLibraryVersion(config)
            val response = http.post("${config.base}/items") {
                contentType(ContentType.Application.Json)
                header("If-Unmodified-Since-Version", version)
                setBody(JsonArray(listOf(data)))
            }
            if (response.status != HttpStatusCode.OK) throw ZoteroApiException("Could not save annotation", response.status)
            createdItemKey(Json.parseToJsonElement(response.bodyAsText()).jsonObject, response.status)
        }

    suspend fun updateAnnotation(config: ZoteroConfig, key: String, version: Int, comment: String, color: String,
        position: JsonObject? = null, text: String? = null) {
        defaultHttpClient(config).use { http ->
            val response = http.patch("${config.base}/items/$key") {
                contentType(ContentType.Application.Json)
                header("If-Unmodified-Since-Version", version)
                setBody(buildJsonObject {
                    put("annotationComment", comment)
                    put("annotationColor", color)
                    position?.let { put("annotationPosition", it.toString()) }
                    text?.let { put("annotationText", it) }
                })
            }
            if (response.status != HttpStatusCode.NoContent) throw ZoteroApiException("Could not update annotation. Refresh if it changed in Zotero.", response.status)
        }
    }

    suspend fun deleteAnnotation(config: ZoteroConfig, key: String, version: Int) {
        defaultHttpClient(config).use { http ->
            val response = http.delete("${config.base}/items/$key") { header("If-Unmodified-Since-Version", version) }
            if (response.status != HttpStatusCode.NoContent) throw ZoteroApiException("Could not delete annotation. Refresh if it changed in Zotero.", response.status)
        }
    }

    override suspend fun listLibraryItems(config: ZoteroConfig): List<ZoteroItem> =
        listItems(config, "/items/top")

    override suspend fun listAttachments(config: ZoteroConfig): List<ZoteroItem> =
        listItems(config, "/items", "attachment")

    private suspend fun listItems(config: ZoteroConfig, path: String, itemType: String? = null): List<ZoteroItem> {
        val http = defaultHttpClient(config)
        try {
            val items = mutableListOf<ZoteroItem>()
            var start = 0
            val limit = 100
            while (true) {
                val response = http.get("${config.base}$path") {
                    parameter("start", start)
                    parameter("limit", limit)
                    parameter("format", "json")
                    itemType?.let { parameter("itemType", it) }
                }
                if (response.status != HttpStatusCode.OK) {
                    throw ZoteroApiException("Failed to load library items", response.status)
                }
                val page: List<ZoteroItem> = response.body()
                items += page
                if (page.size < limit) return items.distinctBy { it.key }
                start += limit
            }
        } finally {
            http.close()
        }
    }

    override suspend fun downloadFile(config: ZoteroConfig, attachmentKey: String): ByteArray {
        return defaultHttpClient(config, followRedirects = false).use { http ->
            val response = http.get("${config.base}/items/$attachmentKey/file")
            when (response.status) {
                HttpStatusCode.OK -> response.body()
                HttpStatusCode.Found, HttpStatusCode.TemporaryRedirect, HttpStatusCode.SeeOther -> {
                    val location = response.headers[HttpHeaders.Location]
                        ?: throw ZoteroApiException("File redirect has no location", response.status)
                    // Signed storage URLs must never receive the Zotero API key.
                    HttpClient {
                        install(HttpTimeout) { requestTimeoutMillis = 120_000 }
                        expectSuccess = false
                    }.use { storage ->
                        val file = storage.get(location)
                        if (file.status != HttpStatusCode.OK) {
                            throw ZoteroApiException("Could not download the stored attachment", file.status)
                        }
                        file.body()
                    }
                }
                HttpStatusCode.NotFound -> throw ZoteroApiException(
                    "PDF unavailable in Zotero File Storage. Sync it in Zotero first; local/WebDAV files cannot be downloaded here.",
                    response.status
                )
                HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden -> throw ZoteroApiException(
                    "Zotero denied access to this PDF. Check the API key's library and file permissions.",
                    response.status
                )
                else -> throw ZoteroApiException(
                    "Could not download the Zotero PDF (HTTP ${response.status.value}). Try again after checking the connection.",
                    response.status
                )
            }
        }
    }

    override suspend fun checkConnectivity(config: ZoteroConfig): Boolean {
        val http = defaultHttpClient(config)
        val resp = http.get("${config.base}/collections") { parameter("limit", 1) }
        return resp.status == HttpStatusCode.OK
    }

    override suspend fun listCollections(config: ZoteroConfig): List<ZoteroCollection> {
        return defaultHttpClient(config).use { http ->
            val out = mutableListOf<ZoteroCollection>()
            var start = 0
            val limit = 100
            while (true) {
                val resp = http.get("${config.base}/collections") {
                    parameter("start", start)
                    parameter("limit", limit)
                    parameter("sort", "title")
                }
                if (resp.status != HttpStatusCode.OK) throw ZoteroApiException(
                    message = "Failed to list collections",
                    status = resp.status
                )
                val page: List<ZoteroCollection> = resp.body()
                out += page.filter { !it.data.deleted }
                if (page.size < limit) break
                start += limit
            }
            out
        }
    }

    override suspend fun listItemsOfCollection(config: ZoteroConfig, collectionKey: String): List<ZoteroItem> {
        val http = defaultHttpClient(config)
        val out = mutableListOf<ZoteroItem>()
        var start = 0
        val limit = 100
        while (true) {
            val resp = http.get("${config.base}/collections/$collectionKey/items/top") {
                parameter("start", start)
                parameter("limit", limit)
                parameter("format", "json")
            }
            if (resp.status != HttpStatusCode.OK) throw ZoteroApiException(
                "Failed to list items for collection $collectionKey",
                resp.status
            )
            val page: List<ZoteroItem> = resp.body()
            out += page
            if (page.size < limit) break
            start += limit
        }
        return out
    }

    override suspend fun listCollectionsWithItems(config: ZoteroConfig): List<ZoteroCollectionResponse> {
        val collections = listCollections(config)
        val s = collections.map { collection ->
            val items = listItemsOfCollection(config, collection.key)
            ZoteroCollectionWithItems(collection = collection, items = items)
        }

        return buildCollectionHierarchy(s)
    }

    override suspend fun createCollection(
        config: ZoteroConfig,
        name: String,
        parentCollectionKey: String?,
        version: Int
    ): String {
        return defaultHttpClient(config).use { http ->
            val resp = http.post("${config.base}/collections") {
                header("If-Unmodified-Since-Version", version)
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                setBody(listOf(CreateCollectionRequest(name = name, parentCollectionKey = parentCollectionKey)))
            }
            if (resp.status != HttpStatusCode.OK) throw ZoteroApiException("Failed to create collection", resp.status)
            val wr: WriteResponse = resp.body()
            val key = wr.success.values.firstOrNull()
                ?: wr.successful.values.firstOrNull()?.key
                ?: throw ZoteroApiException(
                    "Create collection failed: ${wr.failed.values.firstOrNull()?.message ?: "Unknown error"}",
                    resp.status
                )
            key
        }
    }

    override suspend fun getCollection(config: ZoteroConfig, collectionKey: String): ZoteroCollection? {
        val http = defaultHttpClient(config)

        val resp = http.get("${config.base}/collections/$collectionKey")
        if (resp.status != HttpStatusCode.OK) return null
        return resp.body()
    }

    override suspend fun getCollectionByName(
        config: ZoteroConfig,
        name: String,
        parentKey: String?
    ): ZoteroCollection? {
        val http = defaultHttpClient(config)

        var start = 0
        val limit = 100
        val normalizedQuery = name.trim()

        while (true) {
            val resp = http.get("${config.base}/collections") {
                parameter("start", start)
                parameter("limit", limit)
                parameter("sort", "title")
            }
            if (resp.status != HttpStatusCode.OK) {
                throw ZoteroApiException("Failed to list collections while searching by name", resp.status)
            }
            val page: List<ZoteroCollection> = resp.body()

            val match = page
                .asSequence()
                .filter { !it.data.deleted }
                .filter { it.data.name.equals(normalizedQuery, ignoreCase = true) }
                .filter { parentKey == null || it.data.parentCollectionKey == parentKey }
                .firstOrNull()

            if (match != null) return match

            if (page.size < limit) break
            start += limit
        }

        return null
    }

    override suspend fun renameCollection(
        config: ZoteroConfig,
        collectionKey: String,
        newName: String,
        parentCollectionKey: String?,
        version: Int
    ) {
        defaultHttpClient(config).use { http ->
            val body = CreateCollectionRequest(name = newName, parentCollectionKey = parentCollectionKey)
            val resp = http.put("${config.base}/collections/$collectionKey") {
                header("If-Unmodified-Since-Version", version)
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                setBody(body)
            }

            if (resp.status != HttpStatusCode.NoContent) {
                val errorBody = runCatching { resp.body<String>() }.getOrElse { "Unable to read response body" }
                throw ZoteroApiException(
                    "Failed to rename collection $collectionKey, status: ${resp.status}, error: $errorBody",
                    resp.status
                )
            }
        }
    }

    override suspend fun deleteCollection(config: ZoteroConfig, collectionKey: String, version: Int) {
        defaultHttpClient(config).use { http ->
            val resp = http.delete("${config.base}/collections/$collectionKey") {
                header("If-Unmodified-Since-Version", version)
            }
            if (resp.status != HttpStatusCode.NoContent) {
                val errorBody = runCatching { resp.body<String>() }.getOrElse { "Unable to read response body" }
                throw ZoteroApiException("Failed to delete collection $collectionKey, error: $errorBody", resp.status)
            }
        }
    }

    override suspend fun deleteItem(config: ZoteroConfig, itemKey: String) {
        val http = defaultHttpClient(config)
        val libraryVersion = fetchLibraryVersion(config)
        val resp = http.delete("${config.base}/items/$itemKey") {
            header("If-Unmodified-Since-Version", libraryVersion)
        }
        if (resp.status != HttpStatusCode.NoContent) throw ZoteroApiException(
            "Failed to delete item $itemKey",
            resp.status
        )
    }

    override suspend fun trashItem(config: ZoteroConfig, itemKey: String, version: Int) {
        defaultHttpClient(config).use { http ->
            val response = http.patch("${config.base}/items/$itemKey") {
                contentType(ContentType.Application.Json)
                header("If-Unmodified-Since-Version", version)
                setBody(buildJsonObject { put("deleted", 1) })
            }
            if (response.status != HttpStatusCode.NoContent) throw ZoteroApiException("Could not move item $itemKey to the trash.", response.status)
        }
    }

    override suspend fun restoreItem(config: ZoteroConfig, itemKey: String, version: Int) {
        defaultHttpClient(config).use { http ->
            val response = http.patch("${config.base}/items/$itemKey") {
                contentType(ContentType.Application.Json)
                header("If-Unmodified-Since-Version", version)
                setBody(buildJsonObject { put("deleted", false) })
            }
            if (response.status != HttpStatusCode.NoContent) {
                throw ZoteroApiException("Could not restore item $itemKey from the trash.", response.status)
            }
        }
    }

    override suspend fun setItemCollections(config: ZoteroConfig, itemKey: String, version: Int, collections: List<String>) {
        defaultHttpClient(config).use { http ->
            val response = http.patch("${config.base}/items/$itemKey") {
                contentType(ContentType.Application.Json)
                header("If-Unmodified-Since-Version", version)
                setBody(buildJsonObject {
                    put("collections", JsonArray(collections.distinct().map(::JsonPrimitive)))
                })
            }
            if (response.status != HttpStatusCode.NoContent) {
                throw ZoteroApiException("Could not change collections for $itemKey.", response.status)
            }
        }
    }

    override suspend fun duplicateItem(config: ZoteroConfig, itemKey: String, version: Int, collections: List<String>): String =
        defaultHttpClient(config).use { http ->
            val original = http.get("${config.base}/items/$itemKey")
            if (original.status != HttpStatusCode.OK) {
                throw ZoteroApiException("Could not read item $itemKey for duplication.", original.status)
            }
            val root = Json.parseToJsonElement(original.bodyAsText()).jsonObject
            if (root["version"]?.jsonPrimitive?.intOrNull != version) {
                throw ZoteroApiException("Item changed in Zotero. Refresh before duplicating.", HttpStatusCode.PreconditionFailed)
            }
            val data = root.getValue("data").jsonObject
            require(data["itemType"]?.jsonPrimitive?.content !in listOf("attachment", "annotation")) {
                "Duplicate a reference or note, not an attachment or annotation."
            }
            require(data["parentItem"] == null || data["parentItem"] == JsonPrimitive(false) || data["parentItem"] == JsonNull) {
                "Only top-level items can be duplicated."
            }
            val copy = buildJsonObject {
                data.filterKeys { it !in setOf("key", "version", "dateAdded", "dateModified", "collections", "relations", "deleted", "parentItem") }
                    .forEach { (field, value) -> put(field, value) }
                put("collections", JsonArray(collections.distinct().map(::JsonPrimitive)))
                put("relations", buildJsonObject {})
            }
            val response = http.post("${config.base}/items") {
                contentType(ContentType.Application.Json)
                setBody(JsonArray(listOf(copy)))
            }
            if (response.status != HttpStatusCode.OK) {
                throw ZoteroApiException("Could not duplicate item: ${response.bodyAsText()}", response.status)
            }
            createdItemKey(Json.parseToJsonElement(response.bodyAsText()).jsonObject, response.status)
        }

    override suspend fun updateItems(config: ZoteroConfig, updates: List<ZoteroItemUpdate>): ZoteroBatchWriteResult {
        if (updates.isEmpty()) return ZoteroBatchWriteResult()
        val updated = mutableSetOf<String>()
        val unchanged = mutableSetOf<String>()
        val failed = mutableMapOf<String, String>()
        defaultHttpClient(config).use { http ->
            updates.chunked(50).forEach { batch ->
                val body = JsonArray(batch.map { update ->
                    buildJsonObject {
                        put("key", update.key)
                        put("version", update.version)
                        update.fields.forEach { (field, value) -> put(field, value) }
                    }
                })
                val response = http.post("${config.base}/items") {
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }
                if (response.status != HttpStatusCode.OK) {
                    throw ZoteroApiException("Could not update items: ${response.bodyAsText()}", response.status)
                }
                val root = Json.parseToJsonElement(response.bodyAsText()).jsonObject
                fun keyAt(index: String) = index.toIntOrNull()?.let(batch::getOrNull)?.key
                root["success"]?.jsonObject?.keys?.mapNotNullTo(updated, ::keyAt)
                root["successful"]?.jsonObject?.keys?.mapNotNullTo(updated, ::keyAt)
                root["unchanged"]?.jsonObject?.keys?.mapNotNullTo(unchanged, ::keyAt)
                root["failed"]?.jsonObject?.forEach { (index, error) ->
                    val key = keyAt(index) ?: return@forEach
                    failed[key] = error.jsonObject["message"]?.jsonPrimitive?.contentOrNull ?: "unknown error"
                }
            }
        }
        return ZoteroBatchWriteResult(updated, unchanged, failed)
    }

    override suspend fun getAttachementTemplate(config: ZoteroConfig, linkMode: String): String {
        val client = defaultHttpClient(config)

        val response = client.get("${ZoteroConfig.BASE_URL}/items/new") {
            parameter("itemType", "attachment")
            parameter("linkMode", linkMode)
        }

        if (response.status != HttpStatusCode.OK) {
            val errorText = runCatching { response.bodyAsText() }.getOrElse { "<unreadable body>" }
            throw ZoteroApiException(
                "Get Attachement template failed: HTTP ${response.status}. Body: $errorText",
                response.status
            )
        }

        return response.bodyAsText()
    }

    override suspend fun importDocuments(
        config: ZoteroConfig, requests: List<ZoteroImportDocumentRequest>
    ): String {
        return defaultHttpClient(config).use { client ->
            val response = client.post("${config.base}/items") {
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                header("If-Unmodified-Since-Version", fetchLibraryVersion(config))
                setBody(requests)
            }
            if (response.status != HttpStatusCode.OK) {
                throw ZoteroApiException("Could not create attachment: ${response.bodyAsText()}", response.status)
            }
            createdItemKey(Json.parseToJsonElement(response.bodyAsText()).jsonObject, response.status)
        }
    }

    override suspend fun uploadFileAuthorization(
        config: ZoteroConfig,
        itemKey: String,
        filename: String,
        fileSize: Int,
        mtime: Long,
        md5Hex: String,
    ): JsonObject {

        return defaultHttpClient(config).use { http ->
            val authResp = http.submitForm(
                url = "${config.base}/items/$itemKey/file",
                formParameters = parameters {
                    append("md5", md5Hex.trim().lowercase())
                    append("filename", filename)
                    append("filesize", fileSize.toString())
                    append("mtime", mtime.toString())
                }
            ) {
                header("If-None-Match", "*")
            }
            if (authResp.status != HttpStatusCode.OK) {
                throw ZoteroApiException("Failed to get upload authorization", authResp.status)
            }
            Json.parseToJsonElement(authResp.bodyAsText()).jsonObject
        }
    }


    override suspend fun postFileS3(
        uploadUrl: String,
        contentType: String,
        uploadBody: ByteArray
    ) {
        HttpClient {
            install(HttpTimeout) { requestTimeoutMillis = 120_000 }
            expectSuccess = false
        }.use { uploadClient ->
            val s3Resp = uploadClient.post(uploadUrl) {
                header(HttpHeaders.ContentType, contentType)
                setBody(uploadBody)
            }
            if (s3Resp.status != HttpStatusCode.Created) {
                throw ZoteroApiException("Failed file upload", s3Resp.status)
            }
        }
    }


    override suspend fun registerUploadedFile(
        config: ZoteroConfig,
        itemKey: String,
        uploadUrl: String,
        contentType: String,
        uploadKey: String,
    ) {
        defaultHttpClient(config).use { http ->
            val registerResp = http.submitForm(
                url = "${config.base}/items/$itemKey/file",
                formParameters = parameters { append("upload", uploadKey) }
            ) {
                header("If-None-Match", "*")
            }
            if (registerResp.status != HttpStatusCode.NoContent) {
                throw ZoteroApiException("Failed to register upload", registerResp.status)
            }
        }
    }

    override suspend fun fetchLibraryVersion(config: ZoteroConfig): Int {
        return defaultHttpClient(config).use { http ->
            val resp = http.get("${config.base}/items") { parameter("limit", 1) }
            if (resp.status != HttpStatusCode.OK) throw ZoteroApiException("Failed to obtain library version", resp.status)
            val v = resp.headers["Last-Modified-Version"] ?: throw ZoteroApiException(
                status = HttpStatusCode.NotFound,
                message = "Library version header missing"
            )
            v.toIntOrNull() ?: throw ZoteroApiException(
                status = HttpStatusCode.NotFound,
                message = "Invalid library version: $v"
            )
        }
    }

    override suspend fun createItem(
        config: ZoteroConfig,
        item: ZoteroItemRequest
    ): String {
        return defaultHttpClient(config).use { http ->
            val libraryVersion = fetchLibraryVersion(config)
            val json = Json { encodeDefaults = true; explicitNulls = false }
            val serialized = json.encodeToJsonElement(item).jsonObject
            val body = JsonArray(listOf(JsonObject(serialized.filterKeys { it != "type" })))
            val response = http.post("${config.base}/items") {
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                header("If-Unmodified-Since-Version", libraryVersion)
                setBody(body)
            }
            if (response.status != HttpStatusCode.OK) {
                throw ZoteroApiException("Could not create item: ${response.bodyAsText()}", response.status)
            }
            createdItemKey(Json.parseToJsonElement(response.bodyAsText()).jsonObject, response.status)
        }
    }

    companion object {
        private fun createdItemKey(root: JsonObject, status: HttpStatusCode): String {
            val failed = root["failed"]?.jsonObject
            if (!failed.isNullOrEmpty()) {
                val errors = failed.values.joinToString("; ") {
                    it.jsonObject["message"]?.jsonPrimitive?.content ?: "unknown error"
                }
                throw ZoteroApiException("Zotero item write failed: $errors", status)
            }
            return root["success"]?.jsonObject?.values?.firstOrNull()?.jsonPrimitive?.content
                ?: root["successful"]?.jsonObject?.values?.firstOrNull()?.jsonObject?.get("key")?.jsonPrimitive?.content
                ?: throw ZoteroApiException("No item key returned", status)
        }

        private fun defaultHttpClient(config: ZoteroConfig, followRedirects: Boolean = true): HttpClient = HttpClient {
            this.followRedirects = followRedirects
            install(HttpTimeout) {
                requestTimeoutMillis = 60_000
                connectTimeoutMillis = 15_000
                socketTimeoutMillis = 60_000
            }
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        encodeDefaults = true
                        explicitNulls = false
                    }
                )
            }
            expectSuccess = false
            defaultRequest {
                header(HttpHeaders.Accept, ContentType.Application.Json)
                header("Zotero-API-Key", config.apiKey)
                header("Zotero-API-Version", "3")
            }
        }

        private fun buildCollectionHierarchy(collections: List<ZoteroCollectionWithItems>): List<ZoteroCollectionResponse> {
            val childrenByParent = collections.groupBy { it.collection.data.parentCollectionKey }

            fun buildTree(parentKey: String?): List<ZoteroCollectionResponse> {
                return childrenByParent[parentKey]?.map { collectionWithItems ->
                    ZoteroCollectionResponse(
                        id = collectionWithItems.collection.key,
                        version = collectionWithItems.collection.version,
                        name = collectionWithItems.collection.data.name,
                        items = collectionWithItems.items.map { item ->
                            ZoteroItemResponse(
                                key = item.key,
                                version = item.version,
                                itemType = item.data.itemType,
                                title = item.data.title,
                                parentItem = item.data.parentItem,
                                linkMode = item.data.linkMode,
                                contentType = item.data.contentType,
                                filename = item.data.filename
                            )
                        },
                        children = buildTree(collectionWithItems.collection.key)
                    )
                } ?: emptyList()
            }

            return buildTree(null)
        }

    }
}


@Serializable
data class Simple(
    val itemType: String ="journalArticle",
    val title: String? = null,
    val creators: List<ZoteroCreator> = emptyList(),
    val tags: List<ZoteroTag> = emptyList(),
    val collections: List<String> = emptyList(),
    val url: String? = null,
    val language: String? = null,

    @SerialName("DOI")  val doi: String? = null,

    val abstractNote: String? = null,
    val date: String? = null
)