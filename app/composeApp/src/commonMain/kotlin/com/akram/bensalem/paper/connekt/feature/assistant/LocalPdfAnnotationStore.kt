package com.akram.bensalem.paper.connekt.feature.assistant

import com.akram.bensalem.paper.connekt.shared.zotero.model.zotero.ZoteroAnnotation
import com.paperconnekt.storage.PdfAttachmentStorage
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface LocalPdfAnnotationStore {
    suspend fun load(key: String): List<ZoteroAnnotation>
    suspend fun save(key: String, annotations: List<ZoteroAnnotation>)
}

class StoredLocalPdfAnnotations(
    private val storage: PdfAttachmentStorage,
) : LocalPdfAnnotationStore {
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    override suspend fun load(key: String): List<ZoteroAnnotation> {
        val content = storage.readAnnotations(key) ?: return emptyList()
        val snapshot = json.decodeFromString<LocalAnnotationSnapshot>(content)
        require(snapshot.version == 1) { "Unsupported local annotation version: ${snapshot.version}." }
        return snapshot.annotations
    }

    override suspend fun save(key: String, annotations: List<ZoteroAnnotation>) {
        storage.writeAnnotations(key, json.encodeToString(LocalAnnotationSnapshot(annotations = annotations)))
    }
}

@Serializable
private data class LocalAnnotationSnapshot(
    val version: Int = 1,
    val annotations: List<ZoteroAnnotation>,
)
