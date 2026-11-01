package com.paperconnekt.web

import com.paperconnekt.storage.WorkspaceStorage

class BrowserWorkspaceStorage : WorkspaceStorage {
    private val workspaceKey = "workspace"
    private val corruptWorkspaceKey = "workspace.corrupt"

    override suspend fun read(): String? = BrowserIndexedDb.read(workspaceKey)

    override suspend fun write(content: String) {
        BrowserIndexedDb.write(workspaceKey, content)
    }

    override suspend fun quarantineCorruptData() {
        BrowserIndexedDb.move(workspaceKey, corruptWorkspaceKey)
    }
}
