package com.paperconnekt.storage

import java.io.File
import java.net.URI
import java.nio.file.Files
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
}
