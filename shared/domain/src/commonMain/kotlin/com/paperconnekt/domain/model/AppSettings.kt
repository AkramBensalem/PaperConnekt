package com.paperconnekt.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class AppSettings(
    val onboardingCompleted: Boolean = false,
    val workspaceName: String = "My research",
    val zoteroEnabled: Boolean = false,
    val zoteroUserId: String = "",
    val zoteroLibraryId: String = "",
    val openAlexDiscoveryEnabled: Boolean = false,
    /** Paper search also ranks passages by meaning (embeddings) when a Mistral or OpenAI key is configured. */
    val semanticSearchEnabled: Boolean = true,
    /** Desktop only: serves the library to local MCP clients (Claude Desktop, Copilot, Cursor) on 127.0.0.1. */
    val mcpServerEnabled: Boolean = false,
    val mcpServerPort: Int = 7337,
    /** MCP clients may also add notes to a paper's Idea Board; otherwise the server is read-only. */
    val mcpAllowIdeaWrites: Boolean = false,
    val selectedTheme: ThemePreference = ThemePreference.SYSTEM,
    val libraryUiState: LibraryUiState = LibraryUiState(),
    val zoteroLibraryCache: ZoteroLibraryCache = ZoteroLibraryCache(),
    val pdfReadingPositions: Map<String, SavedPdfReadingPosition> = emptyMap(),
)

@Serializable
data class ZoteroLibraryCache(
    val accountKey: String = "",
    val groupsJson: String = "",
    val collectionsJson: String = "",
    val itemsJson: String = "",
    val attachmentsJson: String = "",
    val syncState: String = "IDLE",
    val lastSyncedAtEpochMillis: Long? = null,
    val error: String? = null,
)

@Serializable
enum class ThemePreference {
    SYSTEM,
    LIGHT,
    DARK,
}

@Serializable
data class LibraryUiState(
    val source: String = "LOCAL",
    val selectedZoteroGroupId: String? = null,
    val selectedZoteroCollectionKey: String? = null,
    val expandedZoteroCollectionIds: List<String> = emptyList(),
    val selectedFolderId: String? = null,
    val expandedFolderIds: List<String> = emptyList(),
    val view: String = "ALL",
    val query: String = "",
    val showCollections: Boolean = false,
    val localPage: Int = 0,
    val zoteroPage: Int = 0,
    val selectedPaperId: String? = null,
    val selectedZoteroPaperKey: String? = null,
)

@Serializable
data class SavedPdfReadingPosition(
    val pageIndex: Int = 0,
    val fraction: Float = 0f,
    val zoom: Float = 1f,
)
