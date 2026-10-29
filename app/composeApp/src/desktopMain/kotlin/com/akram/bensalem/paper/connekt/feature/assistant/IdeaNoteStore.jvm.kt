package com.akram.bensalem.paper.connekt.feature.assistant

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import com.akram.bensalem.paper.connekt.shared.utils.paperConnektDataDirectory
import com.paperconnekt.storage.createPrivateDirectories
import com.paperconnekt.storage.writePrivateFileAtomically

class FileIdeaNoteStore(
    private val root: Path = paperConnektDataDirectory().resolve("idea-notes"),
) : IdeaNoteStore {
    private fun directory(paperId: String): Path {
        val safeId = paperId.toByteArray(Charsets.UTF_8).joinToString("") { byte ->
            (byte.toInt() and 0xff).toString(16).padStart(2, '0')
        }.ifBlank { "_" }
        return root.resolve(safeId)
    }

    override suspend fun load(paperId: String): List<Idea> = withContext(Dispatchers.IO) {
        val directory = directory(paperId)
        if (!Files.isDirectory(directory)) return@withContext emptyList()
        Files.list(directory).use { files ->
            files.filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".md") }
                .map(::readIdea)
                .toList()
                .sortedBy { it.title.lowercase() }
        }
    }

    override suspend fun save(paperId: String, ideas: List<Idea>) = withContext(Dispatchers.IO) {
        val directory = directory(paperId)
        createPrivateDirectories(directory)
        val expectedFiles = ideas.associate { idea ->
            safeId(idea.id) to "${slug(idea.title)}--${safeId(idea.id)}.md"
        }
        ideas.forEach { idea ->
            val file = directory.resolve("${slug(idea.title)}--${safeId(idea.id)}.md")
            writeAtomically(file, renderIdea(idea))
        }
        Files.list(directory).use { files ->
            files.filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".md") }
                .forEach { file ->
                    val id = readIdea(file).id
                    if (expectedFiles[id] != file.fileName.toString()) Files.deleteIfExists(file)
                }
        }
    }

    private fun readIdea(file: Path): Idea {
        val contents = Files.readString(file)
        val lines = contents.split('\n')
        val id = lines.firstOrNull()?.let(idMarker::matchEntire)?.groupValues?.get(1)
            ?: error("Idea note is missing its PaperConnekt ID")
        val hasPageColor = lines.getOrNull(1)?.let(pageColorMarker::matches) == true
        val pageColor = if (hasPageColor) {
            pageColorMarker.matchEntire(lines[1])?.groupValues?.get(1)
                ?.takeIf { it in supportedPageColors } ?: "cream"
        } else {
            "cream"
        }
        val positionIndex = if (hasPageColor) 2 else 1
        val position = lines.getOrNull(positionIndex)?.let(positionMarker::matchEntire)
        val x = position?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
        val y = position?.groupValues?.get(2)?.toFloatOrNull() ?: 0f
        require(x.isFinite() && y.isFinite()) { "Idea note has an invalid position." }
        val titleIndex = positionIndex + if (position != null) 1 else 0
        val titleLine = lines.getOrNull(titleIndex)
        val title = titleLine?.removePrefix("# ")?.takeIf { it != titleLine }
            ?: file.fileName.toString().removeSuffix(".md")
        val bodyStart = if (lines.getOrNull(titleIndex + 1).isNullOrEmpty()) titleIndex + 2 else titleIndex + 1
        val body = lines.drop(bodyStart).joinToString("\n").removeSuffix("\n")
        return Idea(id = id, title = title, description = body, x = x, y = y, pageColor = pageColor)
    }

    private fun renderIdea(idea: Idea): String {
        val title = idea.title.replace(Regex("[\\r\\n]+"), " ").trim()
        val pageColor = idea.pageColor.takeIf { it in supportedPageColors } ?: "cream"
        require(idea.x.isFinite() && idea.y.isFinite()) { "Idea note has an invalid position." }
        return "<!-- paperconnekt-idea:${safeId(idea.id)} -->\n<!-- paperconnekt-page-color:$pageColor -->\n<!-- paperconnekt-position:${idea.x},${idea.y} -->\n# $title\n\n${idea.description}\n"
    }

    private fun writeAtomically(file: Path, contents: String) {
        writePrivateFileAtomically(file, contents.toByteArray(Charsets.UTF_8))
    }

    private fun safeId(id: String): String =
        id.map { if (it.isLetterOrDigit() || it == '-') it else '_' }.joinToString("").ifBlank { "idea" }

    private fun slug(title: String): String =
        title.lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .take(50)
            .ifBlank { "idea" }

    private companion object {
        val idMarker = Regex("<!-- paperconnekt-idea:([a-zA-Z0-9_-]+) -->")
        val pageColorMarker = Regex("<!-- paperconnekt-page-color:([a-z]+) -->")
        val positionMarker = Regex("<!-- paperconnekt-position:([^,]+),([^ ]+) -->")
        val supportedPageColors = setOf("cream", "sage", "sky", "lavender", "rose")
    }
}

private val sharedIdeaNoteStore by lazy { FileIdeaNoteStore() }

actual fun defaultIdeaNoteStore(): IdeaNoteStore = sharedIdeaNoteStore
