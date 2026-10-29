package com.paperconnekt.storage

import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DesktopPdfAttachmentStorage(
    private val directory: File = File(System.getProperty("user.home"), ".paperconnekt/attachments"),
) : PdfAttachmentStorage {
    override suspend fun importPdf(source: String): String = withContext(Dispatchers.IO) {
        val file = if (source.startsWith("file:")) File(URI(source)) else File(source)
        require(file.isFile && file.canRead()) { "The selected file cannot be read." }
        file.inputStream().use { requirePdfHeader(it.readNBytes(5)) }
        check(directory.mkdirs() || directory.isDirectory) { "Unable to create the PDF attachment directory." }
        val destination = File(File(directory, UUID.randomUUID().toString()), file.name)
        check(destination.parentFile.mkdirs()) { "Unable to create the PDF attachment folder." }
        try {
            file.copyTo(destination)
            destination.absolutePath
        } catch (error: Exception) {
            Files.deleteIfExists(destination.toPath())
            throw error
        }
    }

    override suspend fun readPdf(path: String): ByteArray = withContext(Dispatchers.IO) {
        val file = if (path.startsWith("file:")) File(URI(path)) else File(path)
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
        Files.createDirectories(file.parentFile.toPath())
        val temporary = Files.createTempFile(file.parentFile.toPath(), "annotations-", ".tmp")
        try {
            Files.write(temporary, content.toByteArray(Charsets.UTF_8))
            try {
                Files.move(temporary, file.toPath(), ATOMIC_MOVE, REPLACE_EXISTING)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temporary, file.toPath(), REPLACE_EXISTING)
            }
        } finally {
            Files.deleteIfExists(temporary)
        }
        Unit
    }
}
