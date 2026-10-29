package com.akram.bensalem.paper.connekt.feature.assistant

import com.akram.bensalem.paper.connekt.models.ZoteroConfig
import com.akram.bensalem.paper.connekt.shared.zotero.KtorZoteroClient
import com.akram.bensalem.paper.connekt.shared.zotero.model.zotero.*
import kotlinx.serialization.json.*

const val AI_ANNOTATION_TAG = "paperconnekt:ai"


interface PdfWorkspaceRepository {
    suspend fun download(config: ZoteroConfig, attachment: ZoteroItem): ByteArray
    suspend fun annotations(config: ZoteroConfig, attachmentKey: String): List<ZoteroAnnotation>
    suspend fun create(config: ZoteroConfig, data: JsonObject): String
    suspend fun update(config: ZoteroConfig, annotation: ZoteroAnnotation, comment: String, color: String)
    suspend fun updateTextRange(config: ZoteroConfig, annotation: ZoteroAnnotation, draft: AnnotationDraft)
    suspend fun repairInkPosition(config: ZoteroConfig, annotation: ZoteroAnnotation)
    suspend fun updatePosition(config: ZoteroConfig, annotation: ZoteroAnnotation, comment: String, position: JsonObject)
    suspend fun delete(config: ZoteroConfig, annotation: ZoteroAnnotation)
}

class ZoteroWorkspaceRepository(private val client: KtorZoteroClient = KtorZoteroClient()) : PdfWorkspaceRepository {
    override suspend fun download(config: ZoteroConfig, attachment: ZoteroItem) = client.downloadFile(config, attachment.key)
    override suspend fun annotations(config: ZoteroConfig, attachmentKey: String): List<ZoteroAnnotation> {
        repeat(3) {
            val version = client.fetchLibraryVersion(config)
            val annotations = client.listAnnotations(config, attachmentKey)
            if (version == client.fetchLibraryVersion(config)) return annotations
        }
        error("Zotero annotations changed during refresh. Try refreshing again.")
    }
    override suspend fun create(config: ZoteroConfig, data: JsonObject) = client.createAnnotation(config, data)
    override suspend fun update(config: ZoteroConfig, annotation: ZoteroAnnotation, comment: String, color: String) =
        client.updateAnnotation(config, annotation.key, annotation.version, comment, color,
            if (annotation.needsInkPositionRepair) repairedInkPosition(annotation) else null)
    override suspend fun updateTextRange(config: ZoteroConfig, annotation: ZoteroAnnotation, draft: AnnotationDraft) {
        require(annotation.position.nextPageRects.isEmpty()) { "Adjust multi-page annotation ranges in Zotero." }
        val payload = annotationPayload(annotation.data.getValue("parentItem").jsonPrimitive.content, draft)
        val position = Json.parseToJsonElement(payload.getValue("annotationPosition").jsonPrimitive.content).jsonObject
        client.updateAnnotation(config, annotation.key, annotation.version, draft.comment, draft.color,
            JsonObject(annotation.positionData - "nextPageRects" + position), draft.text)
    }
    override suspend fun repairInkPosition(config: ZoteroConfig, annotation: ZoteroAnnotation) =
        client.updateAnnotation(config, annotation.key, annotation.version, annotation.comment, annotation.color,
            repairedInkPosition(annotation))
    override suspend fun updatePosition(config: ZoteroConfig, annotation: ZoteroAnnotation, comment: String, position: JsonObject) =
        client.updateAnnotation(config, annotation.key, annotation.version, comment, annotation.color, position)
    override suspend fun delete(config: ZoteroConfig, annotation: ZoteroAnnotation) =
        client.deleteAnnotation(config, annotation.key, annotation.version)
}

internal fun repairedInkPosition(annotation: ZoteroAnnotation): JsonObject {
    require(annotation.needsInkPositionRepair) { "This annotation does not need ink position repair." }
    return JsonObject(annotation.positionData - "rects" - "nextPageRects")
}

fun annotationPayload(attachmentKey: String, draft: AnnotationDraft): JsonObject {
    require(draft.type in listOf("highlight", "underline", "note", "ink", "text", "image")) { "Unsupported annotation type." }
    require(draft.pageIndex >= 0 && draft.rects.isNotEmpty()) { "Choose an annotation position." }
    require(draft.rects.all { r -> r.size == 4 && r.all { it.isFinite() } && r[0] < r[2] && r[1] < r[3] }) { "Invalid annotation position." }
    if (draft.type == "ink") {
        require(draft.paths.isNotEmpty() && draft.paths.all { path ->
            path.size >= 4 && path.size % 2 == 0 && path.all(Float::isFinite)
        }) { "Draw at least one valid pen stroke." }
        require(draft.width.isFinite() && draft.width > 0f) { "Invalid pen width." }
    }
    require(draft.color.matches(Regex("#[0-9a-fA-F]{6}"))) { "Invalid annotation color." }
    require(draft.type != "note" || draft.comment.isNotBlank()) { "Enter a note." }
    require(draft.type != "text" || draft.comment.isNotBlank()) { "Enter some text." }
    require(draft.type != "text" || draft.fontSize.isFinite() && draft.fontSize > 0f) { "Invalid text size." }
    return buildJsonObject {
        put("itemType", "annotation")
        put("key", draft.key)
        put("version", 0)
        put("parentItem", attachmentKey)
        put("annotationType", draft.type)
        if (draft.type in listOf("highlight", "underline")) put("annotationText", draft.text)
        put("annotationComment", draft.comment)
        put("annotationColor", draft.color)
        put("annotationPageLabel", (draft.pageIndex + 1).toString())
        put("annotationSortIndex", "${draft.pageIndex.toString().padStart(5, '0')}|000000|00000")
        put("annotationPosition", buildJsonObject {
            put("pageIndex", draft.pageIndex)
            if (draft.type == "ink") {
                put("paths", JsonArray(draft.paths.map { JsonArray(it.map(::JsonPrimitive)) }))
                put("width", draft.width)
            } else put("rects", JsonArray(draft.rects.map { JsonArray(it.map(::JsonPrimitive)) }))
            if (draft.type == "text") {
                put("fontSize", draft.fontSize)
                put("rotation", 0)
            }
        }.toString())
        put("tags", JsonArray(listOfNotNull("paperconnekt:idea".takeIf { draft.isIdea }, AI_ANNOTATION_TAG.takeIf { draft.aiGenerated })
            .map { tag -> buildJsonObject { put("tag", tag) } }))
    }
}
