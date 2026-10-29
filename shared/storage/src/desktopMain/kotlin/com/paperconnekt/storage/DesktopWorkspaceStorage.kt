package com.paperconnekt.storage

import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DesktopWorkspaceStorage(
    baseDirectory: File = desktopAppDataDirectory().toFile(),
) : WorkspaceStorage {
    private val directory = baseDirectory
    private val dataFile = File(directory, "workspace.json")
    private val corruptFile = File(directory, "workspace.json.corrupt")

    override suspend fun read(): String? = withContext(Dispatchers.IO) {
        createPrivateDirectories(directory.toPath())
        dataFile.takeIf(File::isFile)?.readText()
    }

    override suspend fun write(content: String) = withContext(Dispatchers.IO) {
        writePrivateFileAtomically(dataFile.toPath(), content.toByteArray(Charsets.UTF_8))
    }

    override suspend fun quarantineCorruptData() {
        if (dataFile.isFile) {
            Files.deleteIfExists(corruptFile.toPath())
            check(dataFile.renameTo(corruptFile)) { "Unable to preserve corrupt workspace data" }
        }
    }
}
