package com.akram.bensalem.paper.connekt.feature.assistant

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import com.akram.bensalem.paper.connekt.shared.utils.paperConnektDataDirectory
import java.nio.file.StandardCopyOption

/** Stores converted papers under `~/.paperconnekt/markdown/<key>/document.md` with a `figures/` folder. */
class FilePdfMarkdownStore(
    private val root: Path = paperConnektDataDirectory().resolve("markdown"),
) : PdfMarkdownStore {
    private fun directory(key: String): Path =
        key.split('/').filter { it.isNotBlank() }.fold(root) { path, segment -> path.resolve(safePathSegment(segment)) }

    override suspend fun load(key: String): String? = withContext(Dispatchers.IO) {
        val file = directory(key).resolve(DOCUMENT)
        if (Files.isRegularFile(file)) resolveFigures(directory(key), Files.readString(file)) else null
    }

    override suspend fun save(key: String, document: OcrDocument): String = withContext(Dispatchers.IO) {
        val dir = directory(key)
        val figures = dir.resolve(FIGURES)
        Files.createDirectories(figures)
        document.images.forEach { (id, bytes) -> Files.write(figures.resolve(safePathSegment(id)), bytes) }
        writeAtomically(dir, PENDING, "pending")
        Files.deleteIfExists(dir.resolve(READING))
        if (document.layout != null) {
            writeAtomically(dir, LAYOUT, json.encodeToString(OcrLayout.serializer(), document.layout))
        } else {
            Files.deleteIfExists(dir.resolve(LAYOUT))
        }
        writeAtomically(dir, DOCUMENT, document.markdown)
        resolveFigures(dir, document.markdown)
    }

    override suspend fun loadDocument(key: String): OcrDocument? = withContext(Dispatchers.IO) {
        val dir = directory(key)
        val file = dir.resolve(DOCUMENT)
        if (!Files.isRegularFile(file)) return@withContext null
        val markdown = Files.readString(file)
        val images = ocrFigureIds(markdown).associateWith { id ->
            Files.readAllBytes(dir.resolve(FIGURES).resolve(id))
        }
        OcrDocument(markdown, images, loadLayout(key))
    }

    override suspend fun hasPendingSync(key: String): Boolean = withContext(Dispatchers.IO) {
        Files.exists(directory(key).resolve(PENDING))
    }

    override suspend fun setPendingSync(key: String, pending: Boolean) = withContext(Dispatchers.IO) {
        val dir = directory(key)
        if (pending) {
            Files.createDirectories(dir)
            writeAtomically(dir, PENDING, "pending")
        } else {
            Files.deleteIfExists(dir.resolve(PENDING))
        }
        Unit
    }

    override suspend fun loadLayout(key: String): OcrLayout? = withContext(Dispatchers.IO) {
        val file = directory(key).resolve(LAYOUT)
        if (!Files.isRegularFile(file)) return@withContext null
        runCatching { json.decodeFromString(OcrLayout.serializer(), Files.readString(file)) }.getOrNull()
    }

    override suspend fun loadReadingScript(key: String): String? = withContext(Dispatchers.IO) {
        val file = directory(key).resolve(READING)
        if (Files.isRegularFile(file)) Files.readString(file) else null
    }

    override suspend fun saveReadingScript(key: String, script: String) = withContext(Dispatchers.IO) {
        val dir = directory(key)
        Files.createDirectories(dir)
        writeAtomically(dir, READING, script)
    }

    private fun writeAtomically(dir: Path, name: String, content: String) {
        val temporary = Files.createTempFile(dir, name, ".tmp")
        try {
            Files.writeString(temporary, content)
            Files.move(temporary, dir.resolve(name), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } finally {
            Files.deleteIfExists(temporary)
        }
    }

    private fun resolveFigures(dir: Path, markdown: String): String =
        markdown.replace("]($FIGURES/", "](${dir.resolve(FIGURES).toUri().toString().trimEnd('/')}/")

    private companion object {
        const val DOCUMENT = "document.md"
        const val FIGURES = "figures"
        const val LAYOUT = "blocks.json"
        const val READING = "reading.md"
        const val PENDING = "sync-pending"
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    }
}

private val sharedStore by lazy { FilePdfMarkdownStore() }

actual fun defaultPdfMarkdownStore(): PdfMarkdownStore = sharedStore
