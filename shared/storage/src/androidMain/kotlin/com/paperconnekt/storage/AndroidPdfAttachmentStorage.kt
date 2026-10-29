package com.paperconnekt.storage

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.nio.file.Files
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidPdfAttachmentStorage(context: Context) : PdfAttachmentStorage {
    private val resolver = context.applicationContext.contentResolver
    private val directory = File(context.applicationContext.filesDir, "paperconnekt/attachments")

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
        val bytes = if (uri.scheme == "content") {
            requireNotNull(resolver.openInputStream(uri)) { "The attached PDF cannot be read." }.use { it.readBytes() }
        } else {
            File(uri.path ?: path).readBytes()
        }
        bytes.also(::requirePdfHeader)
    }
}
