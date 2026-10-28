package com.paperconnekt.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class LibraryEntry(
    val id: String,
    val paperId: String? = null,
    val parentId: String? = null,
    val label: String,
    val kind: LibraryEntryKind,
)

@Serializable
enum class LibraryEntryKind {
    FOLDER,
    PAPER,
}
