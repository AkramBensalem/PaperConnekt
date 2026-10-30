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
)

@Serializable
data class SavedPdfReadingPosition(
    val pageIndex: Int = 0,
    val fraction: Float = 0f,
    val zoom: Float = 1f,
)
