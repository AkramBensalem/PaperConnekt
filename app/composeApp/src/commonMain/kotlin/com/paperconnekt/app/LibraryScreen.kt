package com.paperconnekt.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.paperconnekt.domain.model.LibraryEntry
import com.paperconnekt.domain.model.LibraryEntryKind
import com.paperconnekt.domain.model.LibraryUiState
import com.paperconnekt.domain.model.Paper
import com.paperconnekt.domain.model.ZoteroLibraryCache
import com.paperconnekt.secrets.SecretKey
import com.paperconnekt.secrets.SecretStore
import com.paperconnekt.storage.LocalResearchStore
import com.akram.bensalem.paper.connekt.models.ZoteroConfig
import com.akram.bensalem.paper.connekt.shared.zotero.KtorZoteroClient
import com.akram.bensalem.paper.connekt.shared.zotero.model.zotero.ZoteroCollection
import com.akram.bensalem.paper.connekt.shared.zotero.model.zotero.ZoteroGroup
import com.akram.bensalem.paper.connekt.shared.zotero.model.zotero.ZoteroItem
import com.akram.bensalem.paper.connekt.shared.dto.request.zotero.ZoteroCreator
import com.akram.bensalem.paper.connekt.shared.dto.request.zotero.ZoteroItemRequest
import com.akram.bensalem.paper.connekt.shared.dto.request.zotero.ZoteroJournalArticleRequest
import com.akram.bensalem.paper.connekt.feature.assistant.ZoteroPdfTarget
import com.akram.bensalem.paper.connekt.feature.assistant.component.PaperDetailsPanel
import com.akram.bensalem.paper.connekt.models.Paper as AssistantPaper
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import com.paperconnekt.app.resources.Res
import com.paperconnekt.app.resources.filled_add
import com.paperconnekt.app.resources.filled_archive
import com.paperconnekt.app.resources.filled_attach_file
import com.paperconnekt.app.resources.filled_check_circle
import com.paperconnekt.app.resources.filled_chevron_right
import com.paperconnekt.app.resources.filled_close
import com.paperconnekt.app.resources.filled_cloud_done
import com.paperconnekt.app.resources.filled_create_new_folder
import com.paperconnekt.app.resources.filled_delete
import com.paperconnekt.app.resources.filled_delete_outline
import com.paperconnekt.app.resources.filled_description
import com.paperconnekt.app.resources.filled_edit
import com.paperconnekt.app.resources.filled_folder
import com.paperconnekt.app.resources.filled_folder_open
import com.paperconnekt.app.resources.filled_groups
import com.paperconnekt.app.resources.filled_more_horiz
import com.paperconnekt.app.resources.filled_picture_as_pdf
import com.paperconnekt.app.resources.filled_restore_from_trash
import com.paperconnekt.app.resources.filled_search
import com.paperconnekt.app.resources.filled_sync
import com.paperconnekt.app.resources.filled_sync_problem
import com.paperconnekt.app.resources.filled_unpublished
import com.paperconnekt.app.resources.research_graph_empty
import org.jetbrains.compose.resources.painterResource

private enum class LibraryView {
    ALL,
    UNFILED,
    RECENT,
    ARCHIVE,
}

private enum class LibrarySource {
    LOCAL,
    ZOTERO,
}

private data class ZoteroLibraryPaper(
    val item: ZoteroItem,
    val attachment: ZoteroItem?,
)

private fun ZoteroLibraryPaper.toAssistantPaper(): AssistantPaper {
    val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
    val authors = item.data.creators.mapNotNull { creator ->
        creator.name ?: listOfNotNull(creator.firstName, creator.lastName)
            .joinToString(" ")
            .takeIf { it.isNotBlank() }
    }
    val tags = item.data.tags.map { it.tag }
    return AssistantPaper(
        id = "zotero:${item.key}",
        title = item.data.title ?: attachment?.data?.filename ?: "Untitled Zotero item",
        authors = authors,
        abstract = item.data.abstractNote,
        tags = tags,
        publicationName = item.data.publicationTitle,
        publicationDate = item.data.date,
        volume = item.data.volume,
        issue = item.data.issue,
        pages = item.data.pages,
        doi = item.data.doi,
        url = item.data.url,
        filePath = attachment?.data?.filename,
        createdAt = now,
        updatedAt = now,
        visitedAt = now,
    )
}

private fun Paper.toAssistantPaper(): AssistantPaper {
    val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
    return AssistantPaper(
        id = id,
        title = title,
        authors = authors,
        abstract = abstractText,
        tags = tags,
        publicationName = source.takeIf { it.isNotBlank() && !it.equals("Local", ignoreCase = true) },
        publicationDate = year?.toString(),
        doi = doi,
        url = sourceUrl,
        filePath = pdfPath,
        createdAt = now,
        updatedAt = now,
        visitedAt = now,
    )
}

private const val LIBRARY_PAGE_SIZE = 10
private const val ZOTERO_SYNC_IDLE = "IDLE"
private const val ZOTERO_SYNCING = "SYNCING"
private const val ZOTERO_SYNCED = "SYNCED"
private const val ZOTERO_SYNC_ERROR = "ERROR"

private val zoteroCacheJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

private inline fun <reified T> decodeZoteroCache(value: String): T =
    zoteroCacheJson.decodeFromString(value)

private fun decodeZoteroPapers(cache: ZoteroLibraryCache): List<ZoteroLibraryPaper> {
    if (cache.itemsJson.isBlank()) return emptyList()
    val items = decodeZoteroCache<List<ZoteroItem>>(cache.itemsJson)
    val attachments = cache.attachmentsJson
        .takeIf(String::isNotBlank)
        ?.let { decodeZoteroCache<List<ZoteroItem>>(it) }
        .orEmpty()
        .groupBy { it.data.parentItem }
    return items.map { item ->
        ZoteroLibraryPaper(item, attachments[item.key].orEmpty().firstOrNull())
    }
}

@Composable
fun LibraryScreen(
    store: LocalResearchStore,
    secretStore: SecretStore,
    paddingValues: PaddingValues,
    onOpenPaper: (Paper) -> Unit = {},
    onOpenGraph: (Paper) -> Unit = {},
    onOpenZoteroPdf: (ZoteroPdfTarget) -> Unit = {},
) {
    val entries by store.libraryEntries.collectAsState()
    val papers by store.papers.collectAsState()
    val settings by store.settings.collectAsState()
    val scope = rememberCoroutineScope()
    val zoteroClient = remember { KtorZoteroClient() }
    val savedLibraryState = settings.libraryUiState
    val initialSource = when (savedLibraryState.source) {
        LibrarySource.ZOTERO.name -> LibrarySource.ZOTERO
        else -> LibrarySource.LOCAL
    }
    val initialGroupId = savedLibraryState.selectedZoteroGroupId
        ?: settings.zoteroLibraryId.takeIf { it.isNotBlank() && it != settings.zoteroUserId }
    var source by remember { mutableStateOf(initialSource) }
    var zoteroCollections by remember { mutableStateOf<List<ZoteroCollection>>(emptyList()) }
    var zoteroGroups by remember { mutableStateOf<List<ZoteroGroup>>(emptyList()) }
    var zoteroPapers by remember { mutableStateOf<List<ZoteroLibraryPaper>>(emptyList()) }
    var zoteroLoading by remember { mutableStateOf(false) }
    var zoteroError by remember { mutableStateOf<String?>(null) }
    var zoteroSyncState by remember { mutableStateOf(ZOTERO_SYNC_IDLE) }
    var zoteroLastSyncedAt by remember { mutableStateOf<Long?>(null) }
    var selectedZoteroCollectionKey by remember { mutableStateOf(savedLibraryState.selectedZoteroCollectionKey) }
    var expandedZoteroCollectionIds by remember {
        mutableStateOf(savedLibraryState.expandedZoteroCollectionIds.toSet())
    }
    var selectedZoteroGroupId by remember { mutableStateOf(initialGroupId) }
    var zoteroRefreshToken by remember { mutableStateOf(0) }
    var selectedFolderId by remember { mutableStateOf(savedLibraryState.selectedFolderId) }
    var view by remember {
        mutableStateOf(
            LibraryView.entries.firstOrNull { it.name == savedLibraryState.view } ?: LibraryView.ALL,
        )
    }
    var zoteroView by remember {
        mutableStateOf(
            LibraryView.entries.firstOrNull { it.name == savedLibraryState.view } ?: LibraryView.ALL,
        )
    }
    var query by remember { mutableStateOf(savedLibraryState.query) }
    var showCreateFolder by remember { mutableStateOf(false) }
    var renameFolder by remember { mutableStateOf<LibraryEntry?>(null) }
    var deleteFolder by remember { mutableStateOf<LibraryEntry?>(null) }
    var movePaper by remember { mutableStateOf<LibraryEntry?>(null) }
    var editPaper by remember { mutableStateOf<Paper?>(null) }
    var selectedPaperId by remember { mutableStateOf<String?>(null) }
    var selectedZoteroPaperKey by remember { mutableStateOf<String?>(null) }
    var permanentlyDeletePaper by remember { mutableStateOf<Paper?>(null) }
    var expandedFolders by remember { mutableStateOf(savedLibraryState.expandedFolderIds.toSet()) }
    var storageError by remember { mutableStateOf<String?>(null) }
    var showPaperSearch by remember { mutableStateOf(false) }
    var showManualPaper by remember { mutableStateOf(false) }
    var newPaperParentFolderId by remember { mutableStateOf<String?>(null) }
    var showCollections by remember { mutableStateOf(savedLibraryState.showCollections) }
    var localPage by remember { mutableStateOf(0) }
    var zoteroPage by remember { mutableStateOf(0) }
    var zoteroTrashItems by remember { mutableStateOf<List<ZoteroItem>>(emptyList()) }
    var zoteroTrashVisible by remember { mutableStateOf(false) }
    var showZoteroCollectionDialog by remember { mutableStateOf(false) }
    var zoteroCollectionParent by remember { mutableStateOf<String?>(null) }
    var zoteroRenameTarget by remember { mutableStateOf<ZoteroCollection?>(null) }
    var zoteroDeleteTarget by remember { mutableStateOf<ZoteroCollection?>(null) }
    var showZoteroItemDialog by remember { mutableStateOf(false) }
    val pdfAttachments = LocalPdfAttachments.current
    val selectedZoteroGroupName = selectedZoteroGroupId?.let { id ->
        zoteroGroups.firstOrNull { it.id.toString() == id }?.data?.name
    }

    LaunchedEffect(source, view, query, selectedFolderId) {
        localPage = 0
    }

    LaunchedEffect(source, selectedZoteroGroupId, selectedZoteroCollectionKey, zoteroView, query, zoteroPapers.size) {
        zoteroPage = 0
    }
    val latestLibraryUiState by androidx.compose.runtime.rememberUpdatedState(
        LibraryUiState(
            source = source.name,
            selectedZoteroGroupId = selectedZoteroGroupId,
            selectedZoteroCollectionKey = selectedZoteroCollectionKey,
            expandedZoteroCollectionIds = expandedZoteroCollectionIds.toList(),
            selectedFolderId = selectedFolderId,
            expandedFolderIds = expandedFolders.toList(),
            view = if (source == LibrarySource.ZOTERO) zoteroView.name else view.name,
            query = query,
            showCollections = showCollections,
        ),
    )

    LaunchedEffect(
        source,
        selectedZoteroGroupId,
        selectedZoteroCollectionKey,
        expandedZoteroCollectionIds,
        selectedFolderId,
        expandedFolders,
        view,
        zoteroView,
        query,
        showCollections,
    ) {
        delay(250)
        try {
            store.updateSettings { it.copy(libraryUiState = latestLibraryUiState) }
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            io.github.aakira.napier.Napier.e("Could not save Library view state", error)
            storageError = error.message ?: "Could not save Library view state."
        }
    }
    androidx.compose.runtime.DisposableEffect(store) {
        onDispose {
            scope.launch(NonCancellable) {
                try {
                    store.updateSettings { it.copy(libraryUiState = latestLibraryUiState) }
                } catch (error: Exception) {
                    io.github.aakira.napier.Napier.e("Could not save Library view state on exit", error)
                }
            }
        }
    }

    val zoteroAccountKey = "${settings.zoteroUserId}:${settings.zoteroLibraryId}:${selectedZoteroGroupId ?: "personal"}"

    LaunchedEffect(source, zoteroAccountKey, settings.zoteroLibraryCache) {
        if (source != LibrarySource.ZOTERO) return@LaunchedEffect
        val cache = settings.zoteroLibraryCache.takeIf { it.accountKey == zoteroAccountKey }
        if (cache == null) {
            zoteroGroups = emptyList()
            zoteroCollections = emptyList()
            zoteroPapers = emptyList()
            zoteroSyncState = ZOTERO_SYNC_IDLE
            zoteroLastSyncedAt = null
            return@LaunchedEffect
        }
        runCatching {
            zoteroGroups = decodeZoteroCache<List<ZoteroGroup>>(cache.groupsJson)
            zoteroCollections = decodeZoteroCache<List<ZoteroCollection>>(cache.collectionsJson)
            zoteroPapers = decodeZoteroPapers(cache)
        }.onFailure { error ->
            io.github.aakira.napier.Napier.e("Could not read cached Zotero Library", error)
            zoteroError = "Saved Zotero results could not be read. Refresh to rebuild the cache."
        }
        zoteroSyncState = cache.syncState
        zoteroLastSyncedAt = cache.lastSyncedAtEpochMillis
        if (cache.syncState == ZOTERO_SYNC_ERROR) {
            zoteroError = cache.error
        }
    }

    LaunchedEffect(source, settings.zoteroEnabled, settings.zoteroUserId, settings.zoteroLibraryId) {
        if (source != LibrarySource.ZOTERO) return@LaunchedEffect
        zoteroError = null
        if (!settings.zoteroEnabled) {
            zoteroGroups = emptyList()
            zoteroCollections = emptyList()
            zoteroPapers = emptyList()
            return@LaunchedEffect
        }
        val apiKey = secretStore.read(SecretKey.ZOTERO).orEmpty()
        if (apiKey.isBlank() || settings.zoteroUserId.isBlank()) {
            zoteroGroups = emptyList()
            zoteroCollections = emptyList()
            zoteroPapers = emptyList()
            zoteroError = "Connect Zotero in Settings before browsing this library."
            return@LaunchedEffect
        }
        try {
            zoteroGroups = withContext(Dispatchers.Default) {
                zoteroClient.listGroups(
                    ZoteroConfig(apiKey = apiKey, userId = settings.zoteroUserId),
                )
            }
            if (selectedZoteroGroupId != null &&
                zoteroGroups.none { it.id.toString() == selectedZoteroGroupId }
            ) {
                selectedZoteroGroupId = null
            }
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            io.github.aakira.napier.Napier.e("Could not load Zotero groups", error)
            zoteroGroups = emptyList()
            zoteroError = error.message ?: "Could not load your Zotero groups."
        }
    }

    LaunchedEffect(source, settings.zoteroEnabled, settings.zoteroUserId, selectedZoteroGroupId, zoteroRefreshToken) {
        if (source != LibrarySource.ZOTERO) return@LaunchedEffect
        val apiKey = secretStore.read(SecretKey.ZOTERO).orEmpty()
        if (!settings.zoteroEnabled || apiKey.isBlank() || settings.zoteroUserId.isBlank()) return@LaunchedEffect
        val accountKey = "${settings.zoteroUserId}:${settings.zoteroLibraryId}:${selectedZoteroGroupId ?: "personal"}"
        val hasCachedResults = zoteroPapers.isNotEmpty() || zoteroCollections.isNotEmpty()
        zoteroLoading = !hasCachedResults
        zoteroSyncState = ZOTERO_SYNCING
        withContext(Dispatchers.Default) {
            store.updateSettings { current ->
                val existing = current.zoteroLibraryCache
                val sameAccount = existing.accountKey == accountKey
                current.copy(
                    zoteroLibraryCache = existing.copy(
                        accountKey = accountKey,
                        groupsJson = if (sameAccount) existing.groupsJson else "",
                        collectionsJson = if (sameAccount) existing.collectionsJson else "",
                        itemsJson = if (sameAccount) existing.itemsJson else "",
                        attachmentsJson = if (sameAccount) existing.attachmentsJson else "",
                        syncState = ZOTERO_SYNCING,
                        error = null,
                    ),
                )
            }
        }
        try {
            val config = ZoteroConfig(
                apiKey = apiKey,
                userId = settings.zoteroUserId,
                groupId = selectedZoteroGroupId,
            )
            val result = withContext(Dispatchers.Default) {
                val topLevelItems = zoteroClient.listLibraryItems(config)
                    .filter { it.data.itemType != "attachment" }
                val attachments = zoteroClient.listAttachments(config)
                    .filter { attachment ->
                        attachment.data.parentItem != null &&
                            (attachment.data.contentType.equals("application/pdf", ignoreCase = true) ||
                                attachment.data.filename?.endsWith(".pdf", ignoreCase = true) == true)
                    }
                val collections = zoteroClient.listCollections(config)
                Triple(topLevelItems, attachments, collections)
            }
            val (topLevelItems, attachments, collections) = result
            val attachmentsByParent = attachments.groupBy { it.data.parentItem }
            zoteroCollections = collections
            val collectionKeys = zoteroCollections.map(ZoteroCollection::key).toSet()
            selectedZoteroCollectionKey = selectedZoteroCollectionKey?.takeIf(collectionKeys::contains)
            expandedZoteroCollectionIds = expandedZoteroCollectionIds.filterTo(mutableSetOf(), collectionKeys::contains)
            zoteroPapers = topLevelItems.map { item ->
                ZoteroLibraryPaper(item, attachmentsByParent[item.key].orEmpty().firstOrNull())
            }
            zoteroSyncState = ZOTERO_SYNCED
            zoteroLastSyncedAt = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
            zoteroError = null
            withContext(Dispatchers.Default) {
                store.updateSettings { current ->
                    current.copy(
                        zoteroLibraryCache = ZoteroLibraryCache(
                            accountKey = accountKey,
                            groupsJson = zoteroCacheJson.encodeToString(zoteroGroups),
                            collectionsJson = zoteroCacheJson.encodeToString(zoteroCollections),
                            itemsJson = zoteroCacheJson.encodeToString(topLevelItems),
                            attachmentsJson = zoteroCacheJson.encodeToString(attachments),
                            syncState = ZOTERO_SYNCED,
                            lastSyncedAtEpochMillis = zoteroLastSyncedAt,
                        ),
                    )
                }
            }
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            io.github.aakira.napier.Napier.e("Could not load Zotero Library", error)
            zoteroError = error.message ?: "Could not load your Zotero library."
            zoteroSyncState = ZOTERO_SYNC_ERROR
            withContext(Dispatchers.Default) {
                store.updateSettings { current ->
                    current.copy(
                        zoteroLibraryCache = current.zoteroLibraryCache.copy(
                            accountKey = accountKey,
                            syncState = ZOTERO_SYNC_ERROR,
                            error = zoteroError,
                        ),
                    )
                }
            }
        } finally {
            zoteroLoading = false
        }
    }

    fun runZoteroWrite(operation: suspend (ZoteroConfig) -> Unit) {
        scope.launch {
            try {
                val apiKey = secretStore.read(SecretKey.ZOTERO).orEmpty()
                require(apiKey.isNotBlank() && settings.zoteroUserId.isNotBlank()) {
                    "Connect Zotero in Settings before making changes."
                }
                operation(
                    ZoteroConfig(
                        apiKey = apiKey,
                        userId = settings.zoteroUserId,
                        groupId = selectedZoteroGroupId,
                    ),
                )
                zoteroRefreshToken++
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                io.github.aakira.napier.Napier.e("Could not save Zotero change", error)
                zoteroError = error.message ?: "Could not save the Zotero change."
            }
        }
    }

    fun loadZoteroTrash() {
        if (zoteroTrashVisible) return
        selectedZoteroPaperKey = null
        runZoteroWrite { config ->
            zoteroTrashItems = withContext(Dispatchers.Default) {
                zoteroClient.listTrashItems(config)
            }
            zoteroTrashVisible = true
        }
    }

    fun uploadZoteroPdf(paper: ZoteroLibraryPaper) {
        runZoteroWrite { config ->
            val file = pdfAttachments.choosePdfForUpload()
            if (file != null) {
                val attachmentKey = zoteroClient.importDocuments(
                    config,
                    listOf(
                        com.akram.bensalem.paper.connekt.shared.dto.request.zotero.ZoteroImportDocumentRequest(
                            filename = file.name,
                            title = file.name,
                            parentItem = paper.item.key,
                        ),
                    ),
                )
                val authorization = zoteroClient.uploadFileAuthorization(
                    config,
                    attachmentKey,
                    file.name,
                    file.bytes.size,
                    file.mtime,
                    file.md5,
                )
                if (authorization["exists"]?.jsonPrimitive?.intOrNull != 1) {
                    fun field(name: String) = authorization[name]?.jsonPrimitive?.content
                        ?: error("Zotero upload authorization did not return $name.")
                    val uploadUrl = field("url")
                    val contentType = field("contentType")
                    val uploadBody = field("prefix").encodeToByteArray() +
                        file.bytes +
                        field("suffix").encodeToByteArray()
                    zoteroClient.postFileS3(uploadUrl, contentType, uploadBody)
                    zoteroClient.registerUploadedFile(
                        config,
                        attachmentKey,
                        uploadUrl,
                        contentType,
                        field("uploadKey"),
                    )
                }
            }
        }
    }

    suspend fun addPaper(paper: Paper, parentFolderId: String? = newPaperParentFolderId) {
        if (store.papers.value.none { it.id == paper.id }) {
            store.savePaper(paper)
        }
        if (parentFolderId != null) {
            store.saveLibraryEntry(
                LibraryEntry(
                    id = "paper-${paper.id}-$parentFolderId",
                    paperId = paper.id,
                    parentId = parentFolderId,
                    label = paper.title,
                    kind = LibraryEntryKind.PAPER,
                ),
            )
            selectedFolderId = parentFolderId
        } else {
            selectedFolderId = null
        }
        newPaperParentFolderId = null
        view = LibraryView.ALL
        query = ""
    }

    fun saveChange(operation: suspend () -> Unit) {
        scope.launch {
            try {
                operation()
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                io.github.aakira.napier.Napier.e("Could not save Library changes", error)
                storageError = error.message ?: "Could not save Library changes."
            }
        }
    }

    val folders = entries.filter { it.kind == LibraryEntryKind.FOLDER }
    val paperEntries = entries.filter { it.kind == LibraryEntryKind.PAPER }
    val paperById = papers.associateBy(Paper::id)
    val selectedPaper = selectedPaperId?.let(paperById::get)
    val attachPdf: suspend (Paper, String) -> Unit = { paper, path ->
        store.updatePaper(paper.id) { it.copy(pdfPath = path) }
    }
    val activePapers = papers.filterNot(Paper::archived)
    val archivedPapers = papers.filter(Paper::archived)
    val filedPaperIds = paperEntries.mapNotNull(LibraryEntry::paperId).toSet()
    val unfiledEntries = activePapers
        .filterNot { it.id in filedPaperIds }
        .map { paper ->
            LibraryEntry(
                id = "unfiled-${paper.id}",
                paperId = paper.id,
                label = paper.title,
                kind = LibraryEntryKind.PAPER,
            )
        }
    val libraryPaperEntries = paperEntries.filter { it.paperId?.let(paperById::get)?.archived != true } + unfiledEntries
    val archivedEntries = archivedPapers.map { paper ->
        LibraryEntry("archived-${paper.id}", paper.id, null, paper.title, LibraryEntryKind.PAPER)
    }
    val selectedFolder = folders.firstOrNull { it.id == selectedFolderId }
    fun belongsToSelectedFolder(entry: LibraryEntry): Boolean {
        var parentId = entry.parentId
        while (parentId != null) {
            if (parentId == selectedFolderId) return true
            parentId = folders.firstOrNull { it.id == parentId }?.parentId
        }
        return false
    }
    val visiblePaperEntries = libraryPaperEntries
        .filter { entry ->
            when {
                view == LibraryView.UNFILED -> entry.parentId == null
                view == LibraryView.RECENT -> entry in libraryPaperEntries.takeLast(8)
                selectedFolderId != null -> entry.parentId == selectedFolderId || belongsToSelectedFolder(entry)
                else -> true
            }
        }
        .filter { entry ->
            val paper = entry.paperId?.let(paperById::get)
            query.isBlank() ||
                entry.label.contains(query, ignoreCase = true) ||
                paper?.authors?.any { author -> author.contains(query, ignoreCase = true) } == true ||
                paper?.doi?.contains(query, ignoreCase = true) == true
        }
    val unfiledCount = unfiledEntries.size
    val orphanCount = paperEntries.count { it.paperId !in paperById }

    LaunchedEffect(visiblePaperEntries.size, archivedEntries.size) {
        localPage = 0
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
    ) {
        val compact = maxWidth < 900.dp
        val pageScrollState = rememberScrollState()
        Box {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(pageScrollState),
            ) {
                LibraryHeader(
                    source = source,
                    paperCount = if (source == LibrarySource.LOCAL) activePapers.size else zoteroPapers.size,
                    folderCount = if (source == LibrarySource.LOCAL) folders.size else zoteroCollections.size,
                    unfiledCount = if (source == LibrarySource.LOCAL) unfiledCount else 0,
                    archiveCount = if (source == LibrarySource.LOCAL) archivedPapers.size else 0,
                    zoteroGroupName = selectedZoteroGroupName,
                    onSourceChange = {
                        source = it
                        query = ""
                        selectedPaperId = null
                        selectedZoteroPaperKey = null
                        zoteroTrashVisible = false
                        zoteroTrashItems = emptyList()
                    },
                    onNewFolder = { showCreateFolder = true },
                    onFindPaper = { showPaperSearch = true },
                    onAddManually = { showManualPaper = true },
                    onCreateZoteroItem = { showZoteroItemDialog = true },
                )
            if (source == LibrarySource.ZOTERO) {
                ZoteroLibraryContent(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = settings.zoteroEnabled,
                    collections = zoteroCollections,
                    papers = zoteroPapers,
                    selectedCollectionKey = selectedZoteroCollectionKey,
                    view = zoteroView,
                    onViewChange = {
                        zoteroView = it
                        if (it != LibraryView.ALL) {
                            zoteroTrashVisible = false
                            zoteroTrashItems = emptyList()
                        }
                    },
                    query = query,
                    onQueryChange = { query = it },
                    loading = zoteroLoading,
                    error = zoteroError,
                    syncState = zoteroSyncState,
                    lastSyncedAt = zoteroLastSyncedAt,
                    page = zoteroPage,
                    onPageChange = { zoteroPage = it },
                    groups = zoteroGroups,
                    selectedGroupId = selectedZoteroGroupId,
                    onSelectGroup = {
                        selectedZoteroGroupId = it
                        zoteroError = null
                        zoteroView = LibraryView.ALL
                        zoteroTrashVisible = false
                        zoteroTrashItems = emptyList()
                    },
                    onSelectCollection = {
                        selectedZoteroCollectionKey = it
                        zoteroView = LibraryView.ALL
                        zoteroTrashVisible = false
                        zoteroTrashItems = emptyList()
                    },
                    expandedCollectionIds = expandedZoteroCollectionIds,
                    onToggleCollection = { collectionKey ->
                        expandedZoteroCollectionIds = if (collectionKey in expandedZoteroCollectionIds) {
                            expandedZoteroCollectionIds - collectionKey
                        } else {
                            expandedZoteroCollectionIds + collectionKey
                        }
                    },
                    onRefresh = { zoteroRefreshToken++ },
                    onSelect = { selectedZoteroPaperKey = it.item.key },
                    selectedPaper = selectedZoteroPaperKey?.let { key ->
                        zoteroPapers.firstOrNull { it.item.key == key }
                    },
                    onCloseDetails = { selectedZoteroPaperKey = null },
                    onOpenPdf = { paper ->
                        paper.attachment?.let { attachment ->
                            onOpenZoteroPdf(
                                ZoteroPdfTarget(
                                    item = paper.item,
                                    attachment = attachment,
                                    groupId = selectedZoteroGroupId,
                                ),
                            )
                        }
                    },
                    onCreateCollection = { parent ->
                        zoteroCollectionParent = parent
                        showZoteroCollectionDialog = true
                    },
                    onCreateItem = { showZoteroItemDialog = true },
                    onRenameCollection = { zoteroRenameTarget = it },
                    onDeleteCollection = { zoteroDeleteTarget = it },
                    onShowTrash = ::loadZoteroTrash,
                    trashVisible = zoteroTrashVisible,
                    trashItems = zoteroTrashItems,
                    onRestoreTrash = { item ->
                        runZoteroWrite { config ->
                            zoteroClient.restoreItem(config, item.key, item.version ?: zoteroClient.fetchLibraryVersion(config))
                            zoteroTrashItems = zoteroTrashItems.filterNot { it.key == item.key }
                        }
                    },
                    onAttachPdf = ::uploadZoteroPdf,
                )
            } else if (compact) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        LibraryContextBar(
                            modifier = Modifier.weight(1f),
                            view = view,
                            selectedFolder = selectedFolder,
                            onViewChange = {
                                view = it
                                selectedFolderId = null
                            },
                        )
                        HeaderSearchField(
                            query = query,
                            onQueryChange = { query = it },
                            label = "Search saved papers",
                            modifier = Modifier.widthIn(max = 280.dp).weight(0.7f),
                        )
                    }
                    TextButton(onClick = { showCollections = true }) {
                        Icon(painterResource(Res.drawable.filled_folder_open), contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(selectedFolder?.label ?: "Browse collections")
                    }
                    if (showCollections) {
                        Dialog(onDismissRequest = { showCollections = false }) {
                            Surface(shape = MaterialTheme.shapes.large) {
                                Column {
                                    LibraryFolderList(
                                        folders = folders,
                                        entries = entries,
                                        selectedFolderId = selectedFolderId,
                                        expandedFolders = expandedFolders,
                                        onSelectFolder = {
                                            selectedFolderId = it
                                            view = LibraryView.ALL
                                            showCollections = false
                                        },
                                        onToggleFolder = { folderId ->
                                            expandedFolders = if (folderId in expandedFolders) {
                                                expandedFolders - folderId
                                            } else {
                                                expandedFolders + folderId
                                            }
                                        },
                                        onNewFolder = {
                                            showCollections = false
                                            showCreateFolder = true
                                        },
                                        onArchive = {
                                            view = LibraryView.ARCHIVE
                                            selectedFolderId = null
                                            showCollections = false
                                        },
                                        archiveCount = archivedPapers.size,
                                        archiveSelected = view == LibraryView.ARCHIVE,
                                        onNewSubcollection = { folder ->
                                            selectedFolderId = folder.id
                                            expandedFolders = expandedFolders + folder.id
                                            showCollections = false
                                            showCreateFolder = true
                                        },
                                        onFindPaper = { folder ->
                                            selectedFolderId = folder.id
                                            newPaperParentFolderId = folder.id
                                            showCollections = false
                                            showPaperSearch = true
                                        },
                                        onAddManually = { folder ->
                                            selectedFolderId = folder.id
                                            newPaperParentFolderId = folder.id
                                            showCollections = false
                                            showManualPaper = true
                                        },
                                        onRename = {
                                            showCollections = false
                                            renameFolder = it
                                        },
                                        onDelete = {
                                            showCollections = false
                                            deleteFolder = it
                                        },
                                    )
                                    TextButton(
                                        onClick = { showCollections = false },
                                        modifier = Modifier.align(Alignment.End).padding(8.dp),
                                    ) { Text("Close collections") }
                                }
                            }
                        }
                    }
                    LibraryPaperList(
                        modifier = Modifier.fillMaxWidth(),
                        entries = if (view == LibraryView.ARCHIVE) archivedEntries else visiblePaperEntries,
                        page = localPage,
                        onPageChange = { localPage = it },
                        paperById = paperById,
                        emptyLabel = if (selectedFolder != null) {
                            "No papers in ${selectedFolder.label} yet."
                        } else {
                            "No saved papers match this view."
                        },
                        onOpenPaper = onOpenPaper,
                        onOpenGraph = onOpenGraph,
                        onMove = { movePaper = it },
                        onEdit = { editPaper = it },
                        onToggleCompleted = { paper -> saveChange { store.updatePaper(paper.id) { it.copy(completed = !it.completed) } } },
                        onSelect = { selectedPaperId = it.id },
                        selectedPaperId = selectedPaperId,
                        onDelete = { entry ->
                            entry.paperId?.let { paperById[it] }?.let { paper ->
                                if (view == LibraryView.ARCHIVE) permanentlyDeletePaper = paper
                                else saveChange { store.updatePaper(paper.id) { it.copy(archived = true) } }
                            }
                        },
                        onRestore = { entry ->
                            entry.paperId?.let { paperById[it] }?.let { paper ->
                                saveChange { store.updatePaper(paper.id) { it.copy(archived = false) } }
                            }
                        },
                        archiveView = view == LibraryView.ARCHIVE,
                    )
                }
                selectedPaper?.let { paper ->
                    Dialog(onDismissRequest = { selectedPaperId = null }) {
                        PaperDetailsPane(
                            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f),
                            paper = paper,
                            onRead = { onOpenPaper(paper) },
                            onAttach = { path -> attachPdf(paper, path) },
                            onEdit = { editPaper = paper },
                            onClose = { selectedPaperId = null },
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    LibraryFolderPane(
                        modifier = Modifier.width(286.dp),
                        folders = folders,
                        entries = entries,
                        selectedFolderId = selectedFolderId,
                        expandedFolders = expandedFolders,
                        onSelectFolder = { selectedFolderId = it },
                        onToggleFolder = { folderId ->
                            expandedFolders = if (folderId in expandedFolders) {
                                expandedFolders - folderId
                            } else {
                                expandedFolders + folderId
                            }
                        },
                        onNewFolder = { showCreateFolder = true },
                        onArchive = { view = LibraryView.ARCHIVE; selectedFolderId = null },
                        archiveCount = archivedPapers.size,
                        archiveSelected = view == LibraryView.ARCHIVE,
                        onNewSubcollection = { folder ->
                            selectedFolderId = folder.id
                            expandedFolders = expandedFolders + folder.id
                            showCreateFolder = true
                        },
                        onFindPaper = { folder ->
                            selectedFolderId = folder.id
                            newPaperParentFolderId = folder.id
                            showPaperSearch = true
                        },
                        onAddManually = { folder ->
                            selectedFolderId = folder.id
                            newPaperParentFolderId = folder.id
                            showManualPaper = true
                        },
                        onRename = { renameFolder = it },
                        onDelete = { deleteFolder = it },
                    )
                    VerticalDivider(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            LibraryContextBar(
                                modifier = Modifier.weight(1f),
                                view = view,
                                selectedFolder = selectedFolder,
                                onViewChange = {
                                    view = it
                                    selectedFolderId = null
                                },
                            )
                            HeaderSearchField(
                                query = query,
                                onQueryChange = { query = it },
                                label = "Search saved papers",
                                modifier = Modifier.widthIn(max = 280.dp).weight(0.7f),
                            )
                        }
                        if (orphanCount > 0) {
                            LibraryNotice("$orphanCount saved item(s) have missing paper metadata.")
                        }
                        LibraryPaperList(
                            modifier = Modifier.fillMaxWidth(),
                            entries = if (view == LibraryView.ARCHIVE) archivedEntries else visiblePaperEntries,
                            page = localPage,
                            onPageChange = { localPage = it },
                            paperById = paperById,
                            emptyLabel = if (selectedFolder != null) {
                                "No papers in ${selectedFolder.label} yet."
                            } else {
                                "No saved papers match this view."
                            },
                            onOpenPaper = onOpenPaper,
                            onOpenGraph = onOpenGraph,
                            onMove = { movePaper = it },
                            onEdit = { editPaper = it },
                            onToggleCompleted = { paper -> saveChange { store.updatePaper(paper.id) { it.copy(completed = !it.completed) } } },
                            onSelect = { selectedPaperId = it.id },
                            selectedPaperId = selectedPaperId,
                            onDelete = { entry ->
                            entry.paperId?.let { paperById[it] }?.let { paper ->
                                if (view == LibraryView.ARCHIVE) permanentlyDeletePaper = paper
                                else saveChange { store.updatePaper(paper.id) { it.copy(archived = true) } }
                            }
                            },
                            onRestore = { entry ->
                                entry.paperId?.let { paperById[it] }?.let { paper ->
                                    saveChange { store.updatePaper(paper.id) { it.copy(archived = false) } }
                                }
                            },
                            archiveView = view == LibraryView.ARCHIVE,
                        )
                    }
                    selectedPaper?.let { paper ->
                        PaperDetailsPane(
                            modifier = Modifier.width(320.dp).heightIn(min = 520.dp),
                            paper = paper,
                            onRead = { if (paper.pdfPath != null) onOpenPaper(paper) },
                            onAttach = { path -> attachPdf(paper, path) },
                            onEdit = { editPaper = paper },
                            onClose = { selectedPaperId = null },
                        )
                    }
                }
            }
            }
            PlatformVerticalScrollbar(
                scrollState = pageScrollState,
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            )
        }
    }

    if (showPaperSearch) {
        LibraryPaperSearchDialog(
            savedPapers = papers,
            onSave = { paper -> addPaper(paper) },
            onDismiss = {
                showPaperSearch = false
                newPaperParentFolderId = null
            },
        )
    }
    if (showZoteroCollectionDialog) {
        FolderNameDialog(
            title = if (zoteroCollectionParent == null) "New Zotero collection" else "New Zotero subcollection",
            confirmLabel = "Create",
            onDismiss = {
                showZoteroCollectionDialog = false
                zoteroCollectionParent = null
            },
            onConfirm = { name ->
                runZoteroWrite { config ->
                    zoteroClient.createCollection(
                        config,
                        name,
                        zoteroCollectionParent,
                        zoteroClient.fetchLibraryVersion(config),
                    )
                }
                showZoteroCollectionDialog = false
                zoteroCollectionParent = null
            },
        )
    }
    if (showZoteroItemDialog) {
        ZoteroCreateItemDialog(
            collectionKey = selectedZoteroCollectionKey,
            onDismiss = { showZoteroItemDialog = false },
            onCreate = { request ->
                runZoteroWrite { config -> zoteroClient.createItem(config, request) }
                showZoteroItemDialog = false
            },
        )
    }
    zoteroRenameTarget?.let { target ->
        FolderNameDialog(
            title = "Rename Zotero collection",
            initialValue = target.data.name,
            confirmLabel = "Save",
            onDismiss = { zoteroRenameTarget = null },
            onConfirm = { name ->
                runZoteroWrite { config ->
                    zoteroClient.renameCollection(
                        config,
                        target.key,
                        name,
                        target.data.parentCollectionKey,
                        target.version ?: zoteroClient.fetchLibraryVersion(config),
                    )
                }
                zoteroRenameTarget = null
            },
        )
    }
    zoteroDeleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { zoteroDeleteTarget = null },
            title = { Text("Delete ${target.data.name}?") },
            text = { Text("This removes the collection from Zotero. Items remain in your library.") },
            confirmButton = {
                Button(
                    onClick = {
                        runZoteroWrite { config ->
                            zoteroClient.deleteCollection(
                                config,
                                target.key,
                                target.version ?: zoteroClient.fetchLibraryVersion(config),
                            )
                        }
                        zoteroDeleteTarget = null
                    },
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { zoteroDeleteTarget = null }) { Text("Cancel") } },
        )
    }
    if (showManualPaper) {
        AddPaperDialog(
            onDismiss = {
                showManualPaper = false
                newPaperParentFolderId = null
            },
            onSave = {
                addPaper(it)
                showManualPaper = false
            },
        )
    }
    if (showCreateFolder) {
        FolderNameDialog(
            title = "Create folder",
            confirmLabel = "Create",
            onDismiss = { showCreateFolder = false },
            onConfirm = { name ->
                saveChange {
                    store.saveLibraryEntry(
                        LibraryEntry(
                            id = "folder-${Random.nextLong()}",
                            label = name,
                            parentId = selectedFolderId,
                            kind = LibraryEntryKind.FOLDER,
                        ),
                    )
                    selectedFolderId?.let { expandedFolders = expandedFolders + it }
                    showCreateFolder = false
                }
            },
        )
    }
    renameFolder?.let { folder ->
        FolderNameDialog(
            title = "Rename folder",
            initialValue = folder.label,
            confirmLabel = "Save",
            onDismiss = { renameFolder = null },
            onConfirm = { name ->
                saveChange {
                    store.saveLibraryEntry(folder.copy(label = name))
                    renameFolder = null
                }
            },
        )
    }
    deleteFolder?.let { folder ->
        AlertDialog(
            onDismissRequest = { deleteFolder = null },
            title = { Text("Delete ${folder.label}?") },
            text = { Text("This removes the folder and its nested library links. Saved papers remain in your local collection.") },
            confirmButton = {
                Button(
                    onClick = {
                        saveChange {
                            store.removeLibraryEntry(folder.id)
                            if (selectedFolderId == folder.id) selectedFolderId = null
                            deleteFolder = null
                        }
                    },
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteFolder = null }) { Text("Cancel") }
            },
        )
    }
    movePaper?.let { entry ->
        MovePaperDialog(
            entry = entry,
            folders = folders,
            onDismiss = { movePaper = null },
            onMove = { folderId ->
                saveChange {
                    store.saveLibraryEntry(entry.copy(parentId = folderId))
                    movePaper = null
                }
            },
        )
    }
    editPaper?.let { paper ->
        PaperDetailsDialog(
            paper = paper,
            onDismiss = { editPaper = null },
            onSave = { updated ->
                saveChange {
                    store.updatePaper(updated.id) { latest ->
                        updated.copy(pdfPath = latest.pdfPath, archived = latest.archived)
                    }
                    editPaper = null
                }
            },
        )
    }
    permanentlyDeletePaper?.let { paper ->
        AlertDialog(
            onDismissRequest = { permanentlyDeletePaper = null },
            title = { Text("Delete permanently?") },
            text = { Text("${paper.title} will be removed from this workspace and cannot be restored.") },
            confirmButton = {
                Button(
                    onClick = {
                        saveChange {
                            store.removePaper(paper.id)
                            permanentlyDeletePaper = null
                        }
                    },
                ) { Text("Delete permanently") }
            },
            dismissButton = { TextButton(onClick = { permanentlyDeletePaper = null }) { Text("Cancel") } },
        )
    }
    storageError?.let { error ->
        AlertDialog(
            onDismissRequest = { storageError = null },
            title = { Text("Library changes were not saved") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { storageError = null }) { Text("Dismiss") }
            },
        )
    }
}

@Composable
private fun LibraryFolderList(
    folders: List<LibraryEntry>,
    entries: List<LibraryEntry>,
    selectedFolderId: String?,
    expandedFolders: Set<String>,
    onSelectFolder: (String?) -> Unit,
    onToggleFolder: (String) -> Unit,
    onNewFolder: () -> Unit,
    onArchive: () -> Unit,
    archiveCount: Int,
    archiveSelected: Boolean = false,
    onNewSubcollection: (LibraryEntry) -> Unit = {},
    onFindPaper: ((LibraryEntry) -> Unit)? = null,
    onAddManually: ((LibraryEntry) -> Unit)? = null,
    onNewItem: ((LibraryEntry) -> Unit)? = null,
    onRename: (LibraryEntry) -> Unit,
    onDelete: (LibraryEntry) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().heightIn(min = 220.dp, max = 330.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Collections", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onNewFolder) { Text("New folder") }
            }
            LazyColumn(
                modifier = Modifier.heightIn(max = 250.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                item {
                    LibraryRootRow(selected = selectedFolderId == null, onClick = { onSelectFolder(null) })
                }
                item {
                    FolderTreeGroup(
                        folders = folders.filter { it.parentId == null },
                        allFolders = folders,
                        allEntries = entries,
                        selectedFolderId = selectedFolderId,
                        expandedFolders = expandedFolders,
                        onSelectFolder = onSelectFolder,
                        onToggleFolder = onToggleFolder,
                        onRename = onRename,
                        onDelete = onDelete,
                        onNewChild = onNewSubcollection,
                        onFindPaper = onFindPaper,
                        onAddManually = onAddManually,
                        onNewItem = onNewItem,
                    )
                }
            }
            HorizontalDivider(
                modifier = Modifier.padding(top = 6.dp, bottom = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
            )
            Spacer(Modifier.height(8.dp))
            ArchiveRow(count = archiveCount, selected = archiveSelected, onClick = onArchive)
        }
    }
}

@Composable
private fun ZoteroLibraryContent(
    modifier: Modifier,
    enabled: Boolean,
    collections: List<ZoteroCollection>,
    papers: List<ZoteroLibraryPaper>,
    selectedCollectionKey: String?,
    view: LibraryView,
    onViewChange: (LibraryView) -> Unit,
    query: String,
    onQueryChange: (String) -> Unit,
    loading: Boolean,
    error: String?,
    syncState: String,
    lastSyncedAt: Long?,
    page: Int,
    onPageChange: (Int) -> Unit,
    groups: List<ZoteroGroup>,
    selectedGroupId: String?,
    onSelectGroup: (String?) -> Unit,
    onSelectCollection: (String?) -> Unit,
    expandedCollectionIds: Set<String>,
    onToggleCollection: (String) -> Unit,
    onRefresh: () -> Unit,
    onSelect: (ZoteroLibraryPaper) -> Unit,
    selectedPaper: ZoteroLibraryPaper?,
    onCloseDetails: () -> Unit,
    onOpenPdf: (ZoteroLibraryPaper) -> Unit,
    onCreateCollection: (String?) -> Unit,
    onCreateItem: () -> Unit,
    onRenameCollection: (ZoteroCollection) -> Unit,
    onDeleteCollection: (ZoteroCollection) -> Unit,
    onShowTrash: () -> Unit,
    trashVisible: Boolean,
    trashItems: List<ZoteroItem>,
    onRestoreTrash: (ZoteroItem) -> Unit,
    onAttachPdf: (ZoteroLibraryPaper) -> Unit,
) {
    val collectionEntries = collections.map { collection ->
        LibraryEntry(
            id = collection.key,
            label = collection.data.name,
            parentId = collection.data.parentCollectionKey,
            kind = LibraryEntryKind.FOLDER,
        )
    }
    val paperEntries = papers.flatMap { paper ->
        val collectionKeys = paper.item.data.collections.ifEmpty { listOf<String?>(null) }
        collectionKeys.map { collectionKey ->
            LibraryEntry(
                id = "zotero-paper-${paper.item.key}-${collectionKey ?: "unfiled"}",
                paperId = paper.item.key,
                parentId = collectionKey,
                label = paper.item.data.title ?: "Untitled Zotero item",
                kind = LibraryEntryKind.PAPER,
            )
        }
    }
    val paperById = papers.associateBy { it.item.key }
    fun belongsToSelectedCollection(entry: LibraryEntry): Boolean {
        var parentId = entry.parentId
        while (parentId != null) {
            if (parentId == selectedCollectionKey) return true
            parentId = collectionEntries.firstOrNull { it.id == parentId }?.parentId
        }
        return false
    }
    val visiblePapers = paperEntries
        .filter { selectedCollectionKey == null || it.parentId == selectedCollectionKey || belongsToSelectedCollection(it) }
        .mapNotNull { it.paperId?.let(paperById::get) }
        .distinctBy { it.item.key }
        .let { filtered ->
            when (view) {
                LibraryView.UNFILED -> filtered.filter { it.item.data.collections.isEmpty() }
                LibraryView.RECENT -> filtered.takeLast(8)
                else -> filtered
            }
        }
        .filter { paper ->
            query.isBlank() || listOfNotNull(
                paper.item.data.title,
                paper.item.data.doi,
                paper.item.data.abstractNote,
                paper.item.data.creators.joinToString(" ") { creator ->
                    creator.name ?: listOfNotNull(creator.firstName, creator.lastName).joinToString(" ")
                },
            ).joinToString(" ").contains(query.trim(), ignoreCase = true)
        }
    val selectedGroupName = selectedGroupId?.let { id ->
        groups.firstOrNull { it.id.toString() == id }?.data?.name
    }
    Column(
        modifier = modifier.padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(onClick = { onCreateCollection(selectedCollectionKey) }, enabled = enabled && !loading) {
                Icon(painterResource(Res.drawable.filled_create_new_folder), contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(if (selectedCollectionKey == null) "New collection" else "New subcollection")
            }
            OutlinedButton(onClick = onCreateItem, enabled = enabled && !loading) {
                Icon(painterResource(Res.drawable.filled_add), contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("New item")
            }
        }
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val compact = maxWidth < 900.dp
            if (compact) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    LibraryFolderPane(
                        modifier = Modifier.fillMaxWidth(),
                        folders = collectionEntries,
                        entries = collectionEntries + paperEntries,
                        selectedFolderId = selectedCollectionKey,
                        expandedFolders = expandedCollectionIds,
                        onSelectFolder = onSelectCollection,
                        onToggleFolder = onToggleCollection,
                        onNewFolder = { onCreateCollection(null) },
                        onArchive = {
                            onViewChange(LibraryView.ALL)
                            onShowTrash()
                        },
                        archiveCount = trashItems.size,
                        archiveSelected = trashVisible,
                        onNewSubcollection = { entry ->
                            collections.firstOrNull { it.key == entry.id }?.let { onCreateCollection(it.key) }
                        },
                        onNewItem = { entry ->
                            onSelectCollection(entry.id)
                            onCreateItem()
                        },
                        onRename = { entry ->
                            collections.firstOrNull { it.key == entry.id }?.let(onRenameCollection)
                        },
                        onDelete = { entry ->
                            collections.firstOrNull { it.key == entry.id }?.let(onDeleteCollection)
                        },
                        showActions = true,
                        showCreateCollection = false,
                        showArchive = true,
                        archiveLabel = "Bin",
                        archiveSubtitle = "Deleted Zotero items",
                        headerSubtitle = "Zotero library · ${selectedGroupName ?: "My library"}",
                        headerAction = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ZoteroGroupSelector(
                                    groups = groups,
                                    selectedGroupName = selectedGroupName,
                                    onSelectGroup = onSelectGroup,
                                )
                                ZoteroSyncIndicator(syncState, lastSyncedAt, onRefresh)
                            }
                        },
                    )
                    ZoteroPaperList(
                        modifier = Modifier.fillMaxWidth(),
                        papers = visiblePapers,
                        view = view,
                        onViewChange = onViewChange,
                        query = query,
                        onQueryChange = onQueryChange,
                        loading = loading,
                        enabled = enabled,
                        error = error,
                        page = page,
                        onPageChange = onPageChange,
                        onRefresh = onRefresh,
                        onSelect = onSelect,
                        selectedPaperKey = selectedPaper?.item?.key,
                        onOpenPdf = onOpenPdf,
                        trashVisible = trashVisible,
                        trashItems = trashItems,
                        onRestoreTrash = onRestoreTrash,
                    )
                    selectedPaper?.let { paper ->
                        Dialog(onDismissRequest = onCloseDetails) {
                            ZoteroPaperDetailsPane(
                                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f),
                                paper = paper,
                                onAttach = { onAttachPdf(paper) },
                                onClose = onCloseDetails,
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    LibraryFolderPane(
                        modifier = Modifier.width(286.dp),
                        folders = collectionEntries,
                        entries = collectionEntries + paperEntries,
                        selectedFolderId = selectedCollectionKey,
                        expandedFolders = expandedCollectionIds,
                        onSelectFolder = onSelectCollection,
                        onToggleFolder = onToggleCollection,
                        onNewFolder = { onCreateCollection(null) },
                        onArchive = {
                            onViewChange(LibraryView.ALL)
                            onShowTrash()
                        },
                        archiveCount = trashItems.size,
                        archiveSelected = trashVisible,
                        onNewSubcollection = { entry ->
                            collections.firstOrNull { it.key == entry.id }?.let { onCreateCollection(it.key) }
                        },
                        onNewItem = { entry ->
                            onSelectCollection(entry.id)
                            onCreateItem()
                        },
                        onRename = { entry ->
                            collections.firstOrNull { it.key == entry.id }?.let(onRenameCollection)
                        },
                        onDelete = { entry ->
                            collections.firstOrNull { it.key == entry.id }?.let(onDeleteCollection)
                        },
                        showActions = true,
                        showCreateCollection = false,
                        showArchive = true,
                        archiveLabel = "Bin",
                        archiveSubtitle = "Deleted Zotero items",
                        headerSubtitle = "Zotero library · ${selectedGroupName ?: "My library"}",
                        headerAction = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ZoteroGroupSelector(
                                    groups = groups,
                                    selectedGroupName = selectedGroupName,
                                    onSelectGroup = onSelectGroup,
                                )
                                ZoteroSyncIndicator(syncState, lastSyncedAt, onRefresh)
                            }
                        },
                    )
                    ZoteroPaperList(
                        modifier = Modifier.weight(1f),
                        papers = visiblePapers,
                        view = view,
                        onViewChange = onViewChange,
                        query = query,
                        onQueryChange = onQueryChange,
                        loading = loading,
                        enabled = enabled,
                        error = error,
                        page = page,
                        onPageChange = onPageChange,
                        onRefresh = onRefresh,
                        onSelect = onSelect,
                        selectedPaperKey = selectedPaper?.item?.key,
                        onOpenPdf = onOpenPdf,
                        trashVisible = trashVisible,
                        trashItems = trashItems,
                        onRestoreTrash = onRestoreTrash,
                    )
                    selectedPaper?.let { paper ->
                        ZoteroPaperDetailsPane(
                            modifier = Modifier.width(360.dp).heightIn(min = 520.dp),
                            paper = paper,
                            onAttach = { onAttachPdf(paper) },
                            onClose = onCloseDetails,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ZoteroTrashPanel(
    items: List<ZoteroItem>,
    query: String,
    onRestore: (ZoteroItem) -> Unit,
) {
    val visibleItems = items.filter { item ->
        query.isBlank() || listOfNotNull(
            item.data.title,
            item.data.filename,
            item.data.doi,
            item.data.abstractNote,
            item.data.creators.joinToString(" ") { creator ->
                creator.name ?: listOfNotNull(creator.firstName, creator.lastName).joinToString(" ")
            },
        ).joinToString(" ").contains(query.trim(), ignoreCase = true)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (visibleItems.isEmpty()) {
            LibraryNotice(if (items.isEmpty()) "The Zotero bin is empty." else "No binned items match this search.")
        } else {
            visibleItems.forEach { item ->
                ZoteroTrashCard(item, onRestore)
            }
        }
    }
}

@Composable
private fun ZoteroTrashCard(
    item: ZoteroItem,
    onRestore: (ZoteroItem) -> Unit,
) {
    val error = MaterialTheme.colorScheme.error
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        ),
        border = BorderStroke(1.dp, error.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    item.data.title ?: item.data.filename ?: "Untitled Zotero item",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    color = error.copy(alpha = 0.9f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                TextButton(onClick = { onRestore(item) }) {
                    Text("Restore", color = error)
                }
            }
            val authors = item.data.creators.mapNotNull { creator ->
                creator.name ?: listOfNotNull(creator.firstName, creator.lastName)
                    .joinToString(" ")
                    .takeIf { it.isNotBlank() }
            }
            if (authors.isNotEmpty()) {
                Text(authors.joinToString(", "), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                listOfNotNull(item.data.publicationTitle, item.data.date, item.data.doi)
                    .joinToString(" • ")
                    .ifBlank { "Zotero item ${item.key}" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Binned Zotero item",
                style = MaterialTheme.typography.labelMedium,
                color = error.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
private fun ZoteroCreateItemDialog(
    collectionKey: String?,
    onDismiss: () -> Unit,
    onCreate: (ZoteroItemRequest) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var authors by remember { mutableStateOf("") }
    var publication by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var doi by remember { mutableStateOf("") }
    var abstractText by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Zotero item") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    if (collectionKey == null) "Create this item in your Zotero library."
                    else "Create this item in the selected collection.",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = authors,
                    onValueChange = { authors = it },
                    label = { Text("Authors (separated by ;)") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = publication,
                    onValueChange = { publication = it },
                    label = { Text("Journal or publication") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = doi,
                    onValueChange = { doi = it },
                    label = { Text("DOI") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = abstractText,
                    onValueChange = { abstractText = it },
                    label = { Text("Abstract") },
                    minLines = 3,
                )
            }
        },
        confirmButton = {
            Button(
                enabled = title.isNotBlank(),
                onClick = {
                    onCreate(
                        ZoteroJournalArticleRequest(
                            title = title.trim(),
                            creators = authors.split(';')
                                .mapNotNull { it.trim().takeIf(String::isNotBlank) }
                                .map { ZoteroCreator("author", name = it) },
                            publicationTitle = publication.trim().ifBlank { null },
                            date = date.trim().ifBlank { null },
                            DOI = doi.trim().ifBlank { null },
                            abstractNote = abstractText.trim().ifBlank { null },
                            collections = listOfNotNull(collectionKey),
                        ),
                    )
                },
            ) { Text("Create item") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ZoteroGroupSelector(
    groups: List<ZoteroGroup>,
    selectedGroupName: String?,
    onSelectGroup: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                painter = painterResource(Res.drawable.filled_groups),
                contentDescription = "Switch Zotero library: ${selectedGroupName ?: "My library"}",
                tint = MaterialTheme.colorScheme.primary,
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text("My library") },
                onClick = {
                    expanded = false
                    onSelectGroup(null)
                },
            )
            groups.forEach { group ->
                DropdownMenuItem(
                    text = { Text(group.data.name) },
                    onClick = {
                        expanded = false
                        onSelectGroup(group.id.toString())
                    },
                )
            }
        }
    }
}

@Composable
private fun ZoteroSyncIndicator(
    syncState: String,
    lastSyncedAt: Long?,
    onRefresh: () -> Unit,
) {
    val syncing = syncState == ZOTERO_SYNCING
    val failed = syncState == ZOTERO_SYNC_ERROR
    IconButton(
        onClick = onRefresh,
        enabled = !syncing,
    ) {
        Icon(
            painter = painterResource(
                when {
                    syncing -> Res.drawable.filled_sync
                    failed -> Res.drawable.filled_sync_problem
                    else -> Res.drawable.filled_cloud_done
                },
            ),
            contentDescription = when {
                syncing -> "Zotero synchronization in progress"
                failed -> "Zotero synchronization failed; retry"
                lastSyncedAt != null -> "Zotero synchronized; refresh"
                else -> "Synchronize Zotero library"
            },
            tint = when {
                syncing -> MaterialTheme.colorScheme.primary
                failed -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.primary
            },
        )
    }
}

@Composable
private fun ZoteroPaperList(
    modifier: Modifier,
    papers: List<ZoteroLibraryPaper>,
    view: LibraryView,
    onViewChange: (LibraryView) -> Unit,
    query: String,
    onQueryChange: (String) -> Unit,
    loading: Boolean,
    enabled: Boolean,
    error: String?,
    page: Int,
    onPageChange: (Int) -> Unit,
    onRefresh: () -> Unit,
    onSelect: (ZoteroLibraryPaper) -> Unit,
    selectedPaperKey: String?,
    onOpenPdf: (ZoteroLibraryPaper) -> Unit,
    trashVisible: Boolean,
    trashItems: List<ZoteroItem>,
    onRestoreTrash: (ZoteroItem) -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LibraryContextBar(
                view = view,
                selectedFolder = null,
                onViewChange = onViewChange,
                showArchive = false,
                modifier = Modifier.weight(1f),
            )
            HeaderSearchField(
                query = query,
                onQueryChange = onQueryChange,
                label = "Search your Zotero library",
                modifier = Modifier.widthIn(max = 320.dp),
            )
        }
        if (trashVisible) {
            ZoteroTrashPanel(
                items = trashItems,
                query = query,
                onRestore = onRestoreTrash,
            )
            return@Column
        }
        when {
            !enabled -> {
                LibraryNotice("Enable Zotero in Settings to browse your connected library.")
            }
            loading -> {
                androidx.compose.material3.LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            error != null && papers.isEmpty() -> {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(error, color = MaterialTheme.colorScheme.onErrorContainer)
                        TextButton(onClick = onRefresh) { Text("Retry") }
                    }
                }
            }
            papers.isEmpty() -> {
                LibraryNotice("No Zotero items match this view.")
            }
            else -> {
                if (error != null) {
                    Text(
                        error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                val pageCount = (papers.size + LIBRARY_PAGE_SIZE - 1) / LIBRARY_PAGE_SIZE
                val currentPage = page.coerceIn(0, (pageCount - 1).coerceAtLeast(0))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    papers.drop(currentPage * LIBRARY_PAGE_SIZE).take(LIBRARY_PAGE_SIZE).forEach { paper ->
                        ZoteroPaperCard(
                            paper = paper,
                            onSelect = onSelect,
                            onOpenPdf = onOpenPdf,
                            selected = paper.item.key == selectedPaperKey,
                        )
                    }
                    if (pageCount > 1) {
                        LibraryPager(
                            page = currentPage,
                            pageCount = pageCount,
                            onPageChange = onPageChange,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ZoteroPaperCard(
    paper: ZoteroLibraryPaper,
    onSelect: (ZoteroLibraryPaper) -> Unit,
    onOpenPdf: (ZoteroLibraryPaper) -> Unit,
    selected: Boolean,
) {
    Card(
        onClick = { onSelect(paper) },
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    paper.item.data.title ?: "Untitled Zotero item",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (paper.attachment != null) {
                    IconButton(onClick = { onOpenPdf(paper) }) {
                        Icon(
                            painter = painterResource(Res.drawable.filled_picture_as_pdf),
                            contentDescription = "Open PDF in reader",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            val authors = paper.item.data.creators.mapNotNull { creator ->
                creator.name ?: listOfNotNull(creator.firstName, creator.lastName).joinToString(" ").takeIf { it.isNotBlank() }
            }
            if (authors.isNotEmpty()) {
                Text(
                    authors.joinToString(", "),
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            Text(
                listOfNotNull(paper.item.data.publicationTitle, paper.item.data.date, paper.item.data.doi)
                    .joinToString(" • ")
                    .ifBlank { "Zotero item ${paper.item.key}" },
                style = MaterialTheme.typography.bodySmall,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Text(
                if (paper.attachment != null) "PDF available in Zotero"
                else "No PDF attachment",
                style = MaterialTheme.typography.labelMedium,
                color = if (paper.attachment != null) {
                    if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                } else if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@Composable
private fun LibraryHeader(
    source: LibrarySource,
    paperCount: Int,
    folderCount: Int,
    unfiledCount: Int,
    archiveCount: Int,
    zoteroGroupName: String?,
    onSourceChange: (LibrarySource) -> Unit,
    onNewFolder: () -> Unit,
    onFindPaper: () -> Unit,
    onAddManually: () -> Unit,
    onCreateZoteroItem: (() -> Unit)? = null,
) {
    var addMenuExpanded by remember { mutableStateOf(false) }
    WorkspaceHeader(
        title = "Library",
        subtitle = if (source == LibrarySource.LOCAL) {
            "Local library"
        } else {
            "Zotero library · ${zoteroGroupName ?: "My library"}"
        },
        icon = Res.drawable.filled_folder_open,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
        summaries = buildList {
            if (source == LibrarySource.ZOTERO) {
                add("$paperCount Zotero item${if (paperCount == 1) "" else "s"}")
            } else {
                add("$paperCount paper${if (paperCount == 1) "" else "s"}")
            }
            add("$folderCount collection${if (folderCount == 1) "" else "s"}")
            if (source == LibrarySource.LOCAL) {
                add("$unfiledCount unfiled")
                add("$archiveCount archived")
            }
        },
        actions = {
            Box {
                HeaderAction(
                    label = "Add paper",
                    icon = Res.drawable.filled_add,
                    onClick = { addMenuExpanded = true },
                )
                DropdownMenu(
                    expanded = addMenuExpanded,
                    onDismissRequest = { addMenuExpanded = false },
                    shape = MaterialTheme.shapes.large,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 4.dp,
                    shadowElevation = 8.dp,
                ) {
                    if (source == LibrarySource.LOCAL) {
                        DropdownMenuItem(
                            text = { Text("Find on OpenAlex") },
                            leadingIcon = {
                                Icon(
                                    painterResource(Res.drawable.filled_search),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            },
                            onClick = { addMenuExpanded = false; onFindPaper() },
                        )
                        DropdownMenuItem(
                            text = { Text("Add manually") },
                            leadingIcon = {
                                Icon(
                                    painterResource(Res.drawable.filled_description),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                )
                            },
                            onClick = { addMenuExpanded = false; onAddManually() },
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("Add paper to Zotero") },
                            leadingIcon = {
                                Icon(
                                    painterResource(Res.drawable.filled_add),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            },
                            onClick = {
                                addMenuExpanded = false
                                onCreateZoteroItem?.invoke()
                            },
                        )
                    }
                }
            }
            if (source == LibrarySource.LOCAL) {
                HeaderAction(
                    label = "New collection",
                    icon = Res.drawable.filled_create_new_folder,
                    onClick = onNewFolder,
                    primary = false,
                )
            }
        },
        tools = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WorkspaceFilterChip(
                    selected = source == LibrarySource.LOCAL,
                    onClick = { onSourceChange(LibrarySource.LOCAL) },
                    label = { Text("Local") },
                )
                WorkspaceFilterChip(
                    selected = source == LibrarySource.ZOTERO,
                    onClick = { onSourceChange(LibrarySource.ZOTERO) },
                    label = { Text("Zotero") },
                )
            }
        },
    )
}

@Composable
private fun LibraryContextBar(
    view: LibraryView,
    selectedFolder: LibraryEntry?,
    onViewChange: (LibraryView) -> Unit,
    showArchive: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WorkspaceFilterChip(
            selected = view == LibraryView.ALL && selectedFolder == null,
            onClick = { onViewChange(LibraryView.ALL) },
            label = { Text("All papers") },
        )
        WorkspaceFilterChip(
            selected = view == LibraryView.UNFILED,
            onClick = { onViewChange(LibraryView.UNFILED) },
            label = { Text("Unfiled") },
        )
        WorkspaceFilterChip(
            selected = view == LibraryView.RECENT,
            onClick = { onViewChange(LibraryView.RECENT) },
            label = { Text("Recent") },
        )
        if (showArchive) {
            WorkspaceFilterChip(
                selected = view == LibraryView.ARCHIVE,
                onClick = { onViewChange(LibraryView.ARCHIVE) },
                label = { Text("Archive") },
                leadingIcon = { Icon(painterResource(Res.drawable.filled_archive), contentDescription = null) },
            )
        }
        selectedFolder?.let {
            Text(" / ${it.label}", color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun LibraryFolderPane(
    modifier: Modifier,
    folders: List<LibraryEntry>,
    entries: List<LibraryEntry>,
    selectedFolderId: String?,
    expandedFolders: Set<String>,
    onSelectFolder: (String?) -> Unit,
    onToggleFolder: (String) -> Unit,
    onNewFolder: () -> Unit,
    onArchive: () -> Unit,
    archiveCount: Int,
    archiveSelected: Boolean = false,
    onNewSubcollection: (LibraryEntry) -> Unit = {},
    onFindPaper: ((LibraryEntry) -> Unit)? = null,
    onAddManually: ((LibraryEntry) -> Unit)? = null,
    onNewItem: ((LibraryEntry) -> Unit)? = null,
    onRename: (LibraryEntry) -> Unit,
    onDelete: (LibraryEntry) -> Unit,
    showActions: Boolean = true,
    showCreateCollection: Boolean = true,
    showArchive: Boolean = true,
    archiveLabel: String = "Archive",
    archiveSubtitle: String = "Deleted items",
    headerSubtitle: String = "Local library",
    headerAction: (@Composable () -> Unit)? = null,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text("Collections", style = MaterialTheme.typography.titleMedium)
                    Text(
                        headerSubtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (headerAction != null) {
                    headerAction()
                } else if (showCreateCollection) {
                    IconButton(onClick = onNewFolder) {
                        Icon(painterResource(Res.drawable.filled_create_new_folder), contentDescription = "Create collection")
                    }
                }
            }
            LibraryRootRow(selected = selectedFolderId == null, onClick = { onSelectFolder(null) })
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                FolderTreeGroup(
                    folders = folders.filter { it.parentId == null },
                    allFolders = folders,
                    allEntries = entries,
                    selectedFolderId = selectedFolderId,
                    expandedFolders = expandedFolders,
                    onSelectFolder = onSelectFolder,
                    onToggleFolder = onToggleFolder,
                    onRename = onRename,
                    onDelete = onDelete,
                    onNewChild = onNewSubcollection,
                    onFindPaper = onFindPaper,
                    onAddManually = onAddManually,
                    onNewItem = onNewItem,
                    showActions = showActions,
                )
            }
            if (showArchive) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(
                    modifier = Modifier.padding(bottom = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                )
                ArchiveRow(
                    count = archiveCount,
                    selected = archiveSelected,
                    onClick = onArchive,
                    label = archiveLabel,
                    subtitle = archiveSubtitle,
                )
            }
        }
    }
}

@Composable
private fun FolderTreeGroup(
    folders: List<LibraryEntry>,
    allFolders: List<LibraryEntry>,
    allEntries: List<LibraryEntry>,
    selectedFolderId: String?,
    expandedFolders: Set<String>,
    onSelectFolder: (String?) -> Unit,
    onToggleFolder: (String) -> Unit,
    onRename: (LibraryEntry) -> Unit,
    onDelete: (LibraryEntry) -> Unit,
    onNewChild: (LibraryEntry) -> Unit,
    onFindPaper: ((LibraryEntry) -> Unit)?,
    onAddManually: ((LibraryEntry) -> Unit)?,
    onNewItem: ((LibraryEntry) -> Unit)?,
    showActions: Boolean = true,
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            folders.forEach { folder ->
                FolderTreeRow(
                    folder = folder,
                    allFolders = allFolders,
                    allEntries = allEntries,
                    selectedFolderId = selectedFolderId,
                    expandedFolders = expandedFolders,
                    onSelectFolder = onSelectFolder,
                    onToggleFolder = onToggleFolder,
                    onRename = onRename,
                    onDelete = onDelete,
                    onNewChild = onNewChild,
                    onFindPaper = onFindPaper,
                    onAddManually = onAddManually,
                    onNewItem = onNewItem,
                    showActions = showActions,
                )
            }
        }
    }
}

@Composable
private fun FolderTreeRow(
    folder: LibraryEntry,
    allFolders: List<LibraryEntry>,
    allEntries: List<LibraryEntry>,
    selectedFolderId: String?,
    expandedFolders: Set<String>,
    onSelectFolder: (String?) -> Unit,
    onToggleFolder: (String) -> Unit,
    onRename: (LibraryEntry) -> Unit,
    onDelete: (LibraryEntry) -> Unit,
    onNewChild: (LibraryEntry) -> Unit = {},
    onFindPaper: ((LibraryEntry) -> Unit)? = null,
    onAddManually: ((LibraryEntry) -> Unit)? = null,
    onNewItem: ((LibraryEntry) -> Unit)? = null,
    depth: Int = 0,
    showActions: Boolean = true,
) {
    val children = allFolders.filter { it.parentId == folder.id }
    val paperCount = allEntries.count { it.kind == LibraryEntryKind.PAPER && it.parentId == folder.id }
    val siblingGuideColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.32f)
    var contextMenuExpanded by remember(folder.id) { mutableStateOf(false) }
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = (depth * 12).dp),
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { onSelectFolder(folder.id) },
                        onLongClick = { contextMenuExpanded = true },
                    )
                    .pointerInput(folder.id) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                                    event.changes.forEach { it.consume() }
                                    contextMenuExpanded = true
                                }
                            }
                        }
                    },
                color = if (folder.id == selectedFolderId) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    Color.Transparent
                },
                border = if (folder.id == selectedFolderId) {
                    BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.32f))
                } else {
                    null
                },
                shape = MaterialTheme.shapes.small,
            ) {
            Row(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (children.isNotEmpty()) {
                    IconButton(
                        onClick = { onToggleFolder(folder.id) },
                        modifier = Modifier.width(32.dp),
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.filled_chevron_right),
                            contentDescription = if (folder.id in expandedFolders) "Collapse ${folder.label}" else "Expand ${folder.label}",
                            modifier = Modifier.rotate(if (folder.id in expandedFolders) 90f else 0f),
                        )
                    }
                } else {
                    Spacer(Modifier.width(32.dp))
                }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = if (folder.id == selectedFolderId) painterResource(Res.drawable.filled_folder_open) else painterResource(Res.drawable.filled_folder),
                        contentDescription = null,
                        tint = if (folder.id == selectedFolderId) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        folder.label,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (folder.id == selectedFolderId) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                    Surface(
                        color = if (folder.id == selectedFolderId) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = MaterialTheme.shapes.extraSmall,
                    ) {
                        Text(
                            paperCount.toString(),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (showActions) {
                    FolderActions(
                        onFindPaper = onFindPaper?.let { callback -> { callback(folder) } },
                        onAddManually = onAddManually?.let { callback -> { callback(folder) } },
                        onRename = { onRename(folder) },
                        onDelete = { onDelete(folder) },
                        onNewChild = { onNewChild(folder) },
                        onNewItem = onNewItem?.let { callback -> { callback(folder) } },
                        expanded = contextMenuExpanded,
                        onExpandedChange = { contextMenuExpanded = it },
                    )
                }
            }
            }
        }
        if (folder.id != selectedFolderId) {
            HorizontalDivider(
                modifier = Modifier.padding(start = (depth * 12 + 44).dp, end = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f),
            )
        }
        if (folder.id in expandedFolders) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        val x = (depth * 12 + 16).dp.toPx()
                        drawLine(
                            color = siblingGuideColor,
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = 1.dp.toPx(),
                        )
                    },
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(start = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    children.forEach { child ->
                        FolderTreeRow(
                            folder = child,
                            allFolders = allFolders,
                            allEntries = allEntries,
                            selectedFolderId = selectedFolderId,
                            expandedFolders = expandedFolders,
                            onSelectFolder = onSelectFolder,
                            onToggleFolder = onToggleFolder,
                            onRename = onRename,
                            onDelete = onDelete,
                            onNewChild = onNewChild,
                            onFindPaper = onFindPaper,
                            onAddManually = onAddManually,
                            onNewItem = onNewItem,
                            depth = depth + 1,
                            showActions = showActions,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryRootRow(
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.32f)) else null,
        shape = MaterialTheme.shapes.small,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                painterResource(Res.drawable.filled_folder_open),
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "All papers",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            )
            Text("Library", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ArchiveRow(
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
    label: String = "Archive",
    subtitle: String = "Deleted items",
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = if (selected) MaterialTheme.colorScheme.errorContainer else Color.Transparent,
        shape = MaterialTheme.shapes.small,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                painterResource(Res.drawable.filled_delete_outline),
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(label, color = if (selected) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = if (selected) MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(count.toString(), style = MaterialTheme.typography.labelMedium, color = if (selected) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun FolderActions(
    onFindPaper: (() -> Unit)?,
    onAddManually: (() -> Unit)?,
    onNewItem: (() -> Unit)?,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onNewChild: () -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
) {
    Box {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier.width(260.dp),
            shape = MaterialTheme.shapes.large,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 4.dp,
            shadowElevation = 8.dp,
        ) {
            Text(
                "Collection actions",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            onFindPaper?.let { onFind ->
                DropdownMenuItem(
                    text = { Text("Add paper") },
                    leadingIcon = {
                        Icon(
                            painterResource(Res.drawable.filled_search),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    onClick = {
                        onExpandedChange(false)
                        onFind()
                    },
                )
            }
            onAddManually?.let { onAdd ->
                DropdownMenuItem(
                    text = { Text("Add paper manually") },
                    leadingIcon = {
                        Icon(
                            painterResource(Res.drawable.filled_description),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                    },
                    onClick = {
                        onExpandedChange(false)
                        onAdd()
                    },
                )
            }
            onNewItem?.let { onCreateItem ->
                DropdownMenuItem(
                    text = { Text("Add paper") },
                    leadingIcon = {
                        Icon(
                            painterResource(Res.drawable.filled_add),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    onClick = {
                        onExpandedChange(false)
                        onCreateItem()
                    },
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
            DropdownMenuItem(
                text = { Text("New subcollection") },
                leadingIcon = {
                    Icon(
                        painterResource(Res.drawable.filled_create_new_folder),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                    )
                },
                onClick = {
                    onExpandedChange(false)
                    onNewChild()
                },
            )
            DropdownMenuItem(
                text = { Text("Rename collection") },
                leadingIcon = {
                    Icon(
                        painterResource(Res.drawable.filled_edit),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                onClick = {
                    onExpandedChange(false)
                    onRename()
                },
            )
            DropdownMenuItem(
                text = { Text("Delete collection", color = MaterialTheme.colorScheme.error) },
                leadingIcon = {
                    Icon(
                        painterResource(Res.drawable.filled_delete),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                },
                onClick = {
                    onExpandedChange(false)
                    onDelete()
                },
            )
        }
    }
}

@Composable
private fun LibraryPaperList(
    modifier: Modifier,
    entries: List<LibraryEntry>,
    page: Int,
    onPageChange: (Int) -> Unit,
    paperById: Map<String, Paper>,
    emptyLabel: String,
    onOpenPaper: (Paper) -> Unit,
    onOpenGraph: (Paper) -> Unit,
    onMove: (LibraryEntry) -> Unit,
    onDelete: (LibraryEntry) -> Unit,
    onRestore: (LibraryEntry) -> Unit,
    archiveView: Boolean,
    onEdit: (Paper) -> Unit,
    onToggleCompleted: (Paper) -> Unit,
    onSelect: (Paper) -> Unit,
    selectedPaperId: String?,
) {
    if (entries.isEmpty()) {
        LibraryEmptyState(modifier = modifier, label = emptyLabel)
        return
    }
    val pageCount = (entries.size + LIBRARY_PAGE_SIZE - 1) / LIBRARY_PAGE_SIZE
    val currentPage = page.coerceIn(0, (pageCount - 1).coerceAtLeast(0))
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            entries.drop(currentPage * LIBRARY_PAGE_SIZE).take(LIBRARY_PAGE_SIZE).forEach { entry ->
                val paper = entry.paperId?.let(paperById::get)
                if (paper == null) {
                    LibraryNotice("The saved paper for “${entry.label}” is no longer available.")
                } else {
                    PaperLibraryCard(
                        paper = paper,
                        onOpen = { onOpenPaper(paper) },
                        onOpenGraph = { onOpenGraph(paper) },
                        onMove = { onMove(entry) },
                        onRemove = { onDelete(entry) },
                        onRestore = { onRestore(entry) },
                        onEdit = { onEdit(paper) },
                        onToggleCompleted = { onToggleCompleted(paper) },
                        onSelect = { onSelect(paper) },
                        selected = paper.id == selectedPaperId,
                        canRemove = !entry.id.startsWith("unfiled-"),
                        archiveView = archiveView,
                    )
                }
            }
        }
        if (pageCount > 1) {
            LibraryPager(
                page = currentPage,
                pageCount = pageCount,
                onPageChange = onPageChange,
            )
        }
    }
}

@Composable
private fun LibraryPager(
    page: Int,
    pageCount: Int,
    onPageChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        OutlinedButton(
            onClick = { onPageChange(page - 1) },
            enabled = page > 0,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Text("Previous", style = MaterialTheme.typography.labelSmall)
        }
        Text(
            "Page ${page + 1} of $pageCount",
            modifier = Modifier.padding(horizontal = 10.dp),
            style = MaterialTheme.typography.labelMedium,
        )
        OutlinedButton(
            onClick = { onPageChange(page + 1) },
            enabled = page < pageCount - 1,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Text("Next", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun PaperLibraryCard(
    paper: Paper,
    onOpen: () -> Unit,
    onOpenGraph: () -> Unit,
    onMove: () -> Unit,
    onRemove: () -> Unit,
    onRestore: () -> Unit,
    onEdit: () -> Unit,
    onToggleCompleted: () -> Unit,
    onSelect: () -> Unit,
    selected: Boolean,
    canRemove: Boolean,
    archiveView: Boolean,
) {
    Surface(
        onClick = onSelect,
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp),
        shape = MaterialTheme.shapes.small,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                painter = if (paper.pdfPath != null) painterResource(Res.drawable.filled_description) else painterResource(Res.drawable.filled_unpublished),
                contentDescription = if (paper.pdfPath != null) "PDF attached" else "No PDF attached",
                tint = if (paper.pdfPath != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            )
            Column(Modifier.weight(1f)) {
                Text(
                    paper.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    paper.authors.ifEmpty { listOf("Unknown author") }.joinToString() +
                        "  ·  " + (paper.year?.toString() ?: "Year unknown"),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (paper.completed) {
                Icon(painterResource(Res.drawable.filled_check_circle), contentDescription = "Completed", tint = MaterialTheme.colorScheme.tertiary)
            }
            TextButton(onClick = if (paper.pdfPath != null) onOpen else onSelect) {
                Text(
                    if (paper.pdfPath != null) "Open PDF" else "View",
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                )
            }
            var actionsExpanded by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { actionsExpanded = true }) { Icon(painterResource(Res.drawable.filled_more_horiz), contentDescription = "Item actions") }
                DropdownMenu(expanded = actionsExpanded, onDismissRequest = { actionsExpanded = false }) {
                    if (paper.pdfPath != null) {
                        DropdownMenuItem(
                            text = { Text("Open PDF") },
                            onClick = { actionsExpanded = false; onOpen() },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(if (paper.pdfPath == null) "Attach PDF" else "Manage PDF") },
                        leadingIcon = { Icon(painterResource(Res.drawable.filled_attach_file), contentDescription = null) },
                        onClick = { actionsExpanded = false; onSelect() },
                    )
                    DropdownMenuItem(
                        text = { Text("Open graph") },
                        onClick = { actionsExpanded = false; onOpenGraph() },
                    )
                    DropdownMenuItem(
                        text = { Text("Edit details") },
                        onClick = { actionsExpanded = false; onEdit() },
                    )
                    DropdownMenuItem(
                        text = { Text("Move to collection") },
                        onClick = { actionsExpanded = false; onMove() },
                    )
                    DropdownMenuItem(
                        text = { Text(if (paper.completed) "Mark as to read" else "Mark completed") },
                        onClick = { actionsExpanded = false; onToggleCompleted() },
                    )
                    if (archiveView) {
                        DropdownMenuItem(
                            text = { Text("Restore to library") },
                            onClick = { actionsExpanded = false; onRestore() },
                            leadingIcon = { Icon(painterResource(Res.drawable.filled_restore_from_trash), contentDescription = null) },
                        )
                        DropdownMenuItem(
                            text = { Text("Delete permanently") },
                            onClick = { actionsExpanded = false; onRemove() },
                            leadingIcon = { Icon(painterResource(Res.drawable.filled_delete), contentDescription = null) },
                        )
                    } else if (canRemove) {
                        DropdownMenuItem(
                            text = { Text("Move to archive") },
                            onClick = { actionsExpanded = false; onRemove() },
                            leadingIcon = { Icon(painterResource(Res.drawable.filled_archive), contentDescription = null) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryEmptyState(
    modifier: Modifier,
    label: String,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                painter = painterResource(Res.drawable.research_graph_empty),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(150.dp).padding(bottom = 12.dp),
            )
            Text("Nothing here yet", style = MaterialTheme.typography.titleLarge)
            Text(
                label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PaperDetailsPane(
    modifier: Modifier,
    paper: Paper,
    onRead: () -> Unit,
    onAttach: suspend (String) -> Unit,
    onEdit: () -> Unit,
    onClose: () -> Unit,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Item details", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = onClose) { Icon(painterResource(Res.drawable.filled_close), contentDescription = "Close details") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onEdit) { Text("Edit details") }
                if (paper.pdfPath != null) {
                    Button(onClick = onRead) {
                        Icon(painterResource(Res.drawable.filled_picture_as_pdf), contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Open PDF")
                    }
                }
            }
            key(paper.id) {
                PdfAttachmentCard(
                    pdfPath = paper.pdfPath,
                    onAttached = onAttach,
                    onOpen = onRead,
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 360.dp),
            ) {
                PaperDetailsPanel(
                    paper = paper.toAssistantPaper(),
                    modifier = Modifier.fillMaxSize(),
                    scrollable = false,
                )
            }
        }
    }
}

@Composable
private fun ZoteroPaperDetailsPane(
    modifier: Modifier,
    paper: ZoteroLibraryPaper,
    onAttach: () -> Unit,
    onClose: () -> Unit,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Zotero item details", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Connected library",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(painterResource(Res.drawable.filled_close), contentDescription = "Close details")
                }
            }
            if (paper.attachment == null) {
                OutlinedButton(onClick = onAttach) {
                    Icon(painterResource(Res.drawable.filled_attach_file), contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Attach PDF to Zotero")
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 360.dp),
            ) {
                PaperDetailsPanel(
                    paper = paper.toAssistantPaper(),
                    modifier = Modifier.fillMaxSize(),
                    scrollable = false,
                )
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(82.dp))
        Text(value, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun LibraryNotice(message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Text(
            message,
            modifier = Modifier.padding(12.dp),
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

@Composable
private fun FolderNameDialog(
    title: String,
    initialValue: String = "",
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember(initialValue) { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Folder name") },
                singleLine = true,
            )
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank(),
                onClick = { onConfirm(name.trim()) },
            ) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun PaperDetailsDialog(
    paper: Paper,
    onDismiss: () -> Unit,
    onSave: (Paper) -> Unit,
) {
    var title by remember(paper.id) { mutableStateOf(paper.title) }
    var authors by remember(paper.id) { mutableStateOf(paper.authors.joinToString(", ")) }
    var year by remember(paper.id) { mutableStateOf(paper.year?.toString().orEmpty()) }
    var doi by remember(paper.id) { mutableStateOf(paper.doi.orEmpty()) }
    var abstractText by remember(paper.id) { mutableStateOf(paper.abstractText.orEmpty()) }
    var completed by remember(paper.id) { mutableStateOf(paper.completed) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Item details") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true)
                OutlinedTextField(authors, { authors = it }, label = { Text("Authors") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(year, { year = it }, label = { Text("Year") }, singleLine = true, modifier = Modifier.weight(0.35f))
                    OutlinedTextField(doi, { doi = it }, label = { Text("DOI") }, singleLine = true, modifier = Modifier.weight(0.65f))
                }
                OutlinedTextField(
                    value = abstractText,
                    onValueChange = { abstractText = it },
                    label = { Text("Abstract / notes") },
                    minLines = 3,
                    maxLines = 5,
                )
                FilterChip(
                    selected = completed,
                    onClick = { completed = !completed },
                    label = { Text(if (completed) "Completed item" else "Mark as completed") },
                    leadingIcon = { Icon(painterResource(Res.drawable.filled_check_circle), contentDescription = null) },
                )
                HorizontalDivider()
                Text("Use Attach PDF in item details to browse or drop a PDF, then Open PDF to read it in Assistant.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            Button(
                enabled = title.isNotBlank(),
                onClick = {
                    onSave(
                        paper.copy(
                            title = title.trim(),
                            authors = authors.split(',').map(String::trim).filter(String::isNotBlank),
                            year = year.toIntOrNull(),
                            doi = doi.trim().ifBlank { null },
                            abstractText = abstractText.trim().ifBlank { null },
                            completed = completed,
                        ),
                    )
                },
            ) { Text("Save changes") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun MovePaperDialog(
    entry: LibraryEntry,
    folders: List<LibraryEntry>,
    onDismiss: () -> Unit,
    onMove: (String?) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move ${entry.label}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { onMove(null) }) { Text("Unfiled") }
                folders.forEach { folder ->
                    TextButton(onClick = { onMove(folder.id) }) {
                        Text(folder.label)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
