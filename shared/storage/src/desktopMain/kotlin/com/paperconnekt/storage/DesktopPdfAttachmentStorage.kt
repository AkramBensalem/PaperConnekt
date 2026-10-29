package com.paperconnekt.storage

import java.io.File
import java.net.URI
import java.nio.file.Files
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DesktopPdfAttachmentStorage(
    private val directory: File = desktopAppDataDirectory().resolve("attachments").toFile(),
) : PdfAttachmentStorage {
    override suspend fun importPdf(source: String): String = withContext(Dispatchers.IO) {
        val file = if (source.startsWith("file:")) File(URI(source)) else File(source)
        require(file.isFile && file.canRead()) { "The selected file cannot be read." }
        file.inputStream().use { requirePdfHeader(it.readNBytes(5)) }
        createPrivateDirectories(directory.toPath())
        val destination = File(File(directory, UUID.randomUUID().toString()), file.name)
        createPrivateDirectories(destination.parentFile.toPath())
        try {
            file.copyTo(destination)
            makePrivateFile(destination.toPath())
            destination.absolutePath
        } catch (error: Exception) {
            Files.deleteIfExists(destination.toPath())
            throw error
        }
    }

    override suspend fun readPdf(path: String): ByteArray = withContext(Dispatchers.IO) {
        val file = if (path.startsWith("file:")) File(URI(path)) else File(path)
        require(file.canonicalFile.toPath().startsWith(directory.canonicalFile.toPath())) {
            "This PDF is not an app-managed attachment. Attach it through the Library first."
        }
        file.readBytes().also(::requirePdfHeader)
    }

    private fun annotationFile(key: String): File {
        val hash = MessageDigest.getInstance("SHA-256").digest(key.toByteArray())
            .joinToString("") { "%02x".format(it) }
        return File(File(directory, "annotations"), "$hash.json")
    }

    override suspend fun readAnnotations(key: String): String? = withContext(Dispatchers.IO) {
        val file = annotationFile(key)
        if (file.exists()) file.readText() else null
    }

    override suspend fun writeAnnotations(key: String, content: String) = withContext(Dispatchers.IO) {
        val file = annotationFile(key)
        writePrivateFileAtomically(file.toPath(), content.toByteArray(Charsets.UTF_8))
        Unit
    }
}
