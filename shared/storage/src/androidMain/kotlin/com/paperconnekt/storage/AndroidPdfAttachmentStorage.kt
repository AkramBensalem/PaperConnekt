package com.paperconnekt.storage

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.nio.file.Files
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidPdfAttachmentStorage(context: Context) : PdfAttachmentStorage {
    private val resolver = context.applicationContext.contentResolver
    private val directory = File(context.applicationContext.noBackupFilesDir, "paperconnekt-contest/attachments")

    override suspend fun importPdf(source: String): String = withContext(Dispatchers.IO) {
        val uri = Uri.parse(source)
        val contentUri = uri.scheme == "content"
        val name = if (contentUri) {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            } ?: "document.pdf"
        } else {
            File(uri.path ?: source).name
        }
        val input = if (contentUri) resolver.openInputStream(uri) else File(uri.path ?: source).inputStream()
        requireNotNull(input) { "The selected file cannot be read." }.use { stream ->
            val header = ByteArray(5)
            var count = 0
            while (count < header.size) {
                val read = stream.read(header, count, header.size - count)
                if (read < 0) break
                count += read
            }
            requirePdfHeader(header.copyOf(count))
            val folder = File(directory, UUID.randomUUID().toString())
            check(folder.mkdirs()) { "Unable to create the PDF attachment folder." }
            val destination = File(folder, File(name).name)
            try {
                destination.outputStream().use { output ->
                    output.write(header)
                    stream.copyTo(output)
                }
                destination.absolutePath
            } catch (error: Exception) {
                Files.deleteIfExists(destination.toPath())
                throw error
            }
        }
    }

    override suspend fun readPdf(path: String): ByteArray = withContext(Dispatchers.IO) {
        val uri = Uri.parse(path)
        val file = File(uri.path ?: path)
        require(uri.scheme != "content" && file.canonicalFile.toPath().startsWith(directory.canonicalFile.toPath())) {
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
