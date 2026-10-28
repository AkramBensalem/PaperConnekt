package com.paperconnekt.storage

interface WorkspaceStorage {
    suspend fun read(): String?
    suspend fun write(content: String)
    suspend fun quarantineCorruptData()
}
