package com.paperconnekt.storage

import android.content.Context
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING

class AndroidWorkspaceStorage(context: Context) : WorkspaceStorage {
    private val directory = File(context.filesDir, "paperconnekt")
    private val dataFile = File(directory, "workspace.json")
    private val temporaryFile = File(directory, "workspace.json.tmp")
    private val corruptFile = File(directory, "workspace.json.corrupt")

    override suspend fun read(): String? = dataFile.takeIf(File::isFile)?.readText()

    override suspend fun write(content: String) {
        directory.mkdirs()
        temporaryFile.writeText(content)
        try {
            Files.move(temporaryFile.toPath(), dataFile.toPath(), ATOMIC_MOVE, REPLACE_EXISTING)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(temporaryFile.toPath(), dataFile.toPath(), REPLACE_EXISTING)
        }
    }

    override suspend fun quarantineCorruptData() {
        if (dataFile.isFile) {
            corruptFile.delete()
            check(dataFile.renameTo(corruptFile)) { "Unable to preserve corrupt workspace data" }
        }
    }
}
