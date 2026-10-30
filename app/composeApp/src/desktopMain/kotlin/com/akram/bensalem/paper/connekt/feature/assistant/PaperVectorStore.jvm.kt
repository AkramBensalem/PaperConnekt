package com.akram.bensalem.paper.connekt.feature.assistant

import com.akram.bensalem.paper.connekt.shared.utils.paperConnektDataDirectory
import com.paperconnekt.knowledge.VectorCodec
import com.paperconnekt.knowledge.VectorStore
import com.paperconnekt.storage.createPrivateDirectories
import com.paperconnekt.storage.writePrivateFileAtomically
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path

/** Chunk embeddings in the app's private data directory, one binary file per paper and embedding model. */
class FilePaperVectorStore(
    private val root: Path = paperConnektDataDirectory().resolve("knowledge"),
) : VectorStore {
    private fun file(paperId: String, modelId: String): Path =
        paperId.split('/').filter { it.isNotBlank() }.fold(root) { path, segment -> path.resolve(safePathSegment(segment)) }
            .resolve("${safePathSegment(modelId)}.vec")

    override suspend fun load(paperId: String, modelId: String): Map<Long, FloatArray>? = withContext(Dispatchers.IO) {
        val file = file(paperId, modelId)
        if (Files.isRegularFile(file)) VectorCodec.decode(Files.readAllBytes(file)) else null
    }

    override suspend fun save(paperId: String, modelId: String, vectors: Map<Long, FloatArray>) = withContext(Dispatchers.IO) {
        val file = file(paperId, modelId)
        createPrivateDirectories(file.parent)
        writePrivateFileAtomically(file, VectorCodec.encode(vectors))
    }
}

private val sharedVectorStore by lazy { FilePaperVectorStore() }

actual fun defaultPaperVectorStore(): VectorStore = sharedVectorStore
