package com.akram.bensalem.paper.connekt.feature.assistant

import com.akram.bensalem.paper.connekt.core.settings.requireAndroidApplicationContext
import com.paperconnekt.knowledge.VectorCodec
import com.paperconnekt.knowledge.VectorStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Chunk embeddings in the app's no-backup storage, one binary file per paper and embedding model. */
private class AndroidPaperVectorStore : VectorStore {
    private val root = File(requireAndroidApplicationContext().noBackupFilesDir, "paperconnekt-contest/knowledge")

    private fun file(paperId: String, modelId: String): File =
        File(paperId.split('/').filter(String::isNotBlank).fold(root) { parent, segment -> File(parent, safePathSegment(segment)) },
            "${safePathSegment(modelId)}.vec")

    override suspend fun load(paperId: String, modelId: String): Map<Long, FloatArray>? = withContext(Dispatchers.IO) {
        val file = file(paperId, modelId)
        if (file.isFile) VectorCodec.decode(file.readBytes()) else null
    }

    override suspend fun save(paperId: String, modelId: String, vectors: Map<Long, FloatArray>) = withContext(Dispatchers.IO) {
        val file = file(paperId, modelId)
        val directory = checkNotNull(file.parentFile)
        check(directory.isDirectory || directory.mkdirs()) { "Could not create paper vector storage." }
        val temporary = File(directory, "${file.name}.tmp")
        temporary.writeBytes(VectorCodec.encode(vectors))
        check(temporary.renameTo(file) || (file.delete() && temporary.renameTo(file))) { "Could not save paper vectors." }
    }
}

private val vectorStore by lazy { AndroidPaperVectorStore() }

actual fun defaultPaperVectorStore(): VectorStore = vectorStore
