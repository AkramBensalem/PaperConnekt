package com.akram.bensalem.paper.connekt.shared.zotero.model.zotero

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

private val annotationJson = Json { ignoreUnknownKeys = true }

@Serializable
data class ZoteroAnnotation(
    val key: String,
    val version: Int,
    val data: JsonObject
) {
    val type: String get() = data.getValue("annotationType").jsonPrimitive.content
    val text: String get() = data["annotationText"]?.jsonPrimitive?.content.orEmpty()
    val comment: String get() = data["annotationComment"]?.jsonPrimitive?.content.orEmpty()
    val color: String get() = data.getValue("annotationColor").jsonPrimitive.content
    val pageLabel: String get() = data["annotationPageLabel"]?.jsonPrimitive?.content.orEmpty()
    val isIdea: Boolean get() = hasTag("paperconnekt:idea")
    val isAiGenerated: Boolean get() = hasTag("paperconnekt:ai")
    private fun hasTag(tag: String) = (data["tags"] as? JsonArray).orEmpty().any {
        (it as? JsonObject)?.get("tag")?.jsonPrimitive?.content == tag
    }
    val positionData: JsonObject get() {
        val value = data.getValue("annotationPosition")
        val objectValue = if (value is JsonPrimitive) Json.parseToJsonElement(value.content) else value
        return objectValue.jsonObject
    }
    val position: AnnotationPosition get() = annotationJson.decodeFromJsonElement(positionData)
    val needsInkPositionRepair: Boolean get() = type == "ink" && "rects" in positionData && position.paths.isNotEmpty()
}

@Serializable
data class AnnotationPosition(
    val pageIndex: Int,
    val rects: List<List<Float>> = emptyList(),
    val nextPageRects: List<List<Float>> = emptyList(),
    val paths: List<List<Float>> = emptyList(),
    val width: Float = 1f,
    val rotation: Float = 0f,
    val fontSize: Float = 12f
) {
    val boundingRects: List<List<Float>> get() {
        if (paths.isEmpty()) return rects
        val points = paths.flatMap { it.chunked(2) }
        val radius = width / 2f
        return listOf(listOf(
            points.minOf { it[0] } - radius,
            points.minOf { it[1] } - radius,
            points.maxOf { it[0] } + radius,
            points.maxOf { it[1] } + radius
        ))
    }
}
