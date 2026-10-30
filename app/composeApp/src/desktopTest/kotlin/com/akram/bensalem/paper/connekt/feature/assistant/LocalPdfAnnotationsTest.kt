package com.akram.bensalem.paper.connekt.feature.assistant

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.akram.bensalem.paper.connekt.models.ZoteroConfig
import com.akram.bensalem.paper.connekt.shared.zotero.model.zotero.ZoteroAnnotation
import com.akram.bensalem.paper.connekt.shared.zotero.model.zotero.ZoteroItem
import com.paperconnekt.storage.DesktopPdfAttachmentStorage
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.JsonObject

@OptIn(ExperimentalCoroutinesApi::class)
class LocalPdfAnnotationsTest {
    @Test
    fun allLocalToolsSaveReloadAndEditWithoutZotero() = runTest {
        val store = MemoryAnnotations()
        withWorkspace(store) { workspace ->
            assertTrue(workspace.state.canWrite)
            workspace.beginNote()
            workspace.changeDraft("A page note", "#ffd400")
            workspace.saveDraft()
            workspace.beginIdea(Rect(30f, 30f, 50f, 50f))
            workspace.changeDraft("A page idea", "#ffd400")
            workspace.saveDraft()
            runCurrent()
            workspace.addInkPath(listOf(Offset(10f, 10f), Offset(20f, 20f)), "#ffd400")
            workspace.saveDraft()
            runCurrent()
            workspace.beginRectangle(Rect(15f, 15f, 45f, 45f))
            workspace.saveDraft()
            runCurrent()
            workspace.beginText(Offset(10f, 30f))
            workspace.changeDraft("Text box", "#ffd400")
            workspace.saveDraft()
            runCurrent()
            workspace.selectAllText()
            workspace.highlightSelectedText()
            workspace.saveDraft()
            runCurrent()
            workspace.selectAllText()
            workspace.underlineSelectedText()
            workspace.saveDraft()
            runCurrent()

            assertEquals(7, workspace.state.annotations.size)
            assertEquals(setOf("note", "ink", "image", "text", "highlight", "underline"),
                workspace.state.annotations.map { it.type }.toSet())
            assertTrue(workspace.state.annotations.any { it.isIdea })
            assertEquals(workspace.state.annotations, store.saved)
            assertEquals(0, workspace.state.pendingWrites)
            assertFalse(workspace.state.needsRefresh)

            workspace.openLocal(byteArrayOf(), "Local paper", KEY)
            runCurrent()
            assertEquals(store.saved, workspace.state.annotations)
            val note = workspace.state.annotations.first { it.type == "note" && !it.isIdea }
            workspace.editAnnotation(note)
            workspace.changeDraft("Edited note", "#ff6666")
            workspace.saveDraft()
            runCurrent()
            assertEquals("Edited note", store.saved.first { it.key == note.key }.comment)
            assertEquals("#ff6666", store.saved.first { it.key == note.key }.color)

            val ink = workspace.state.annotations.first { it.type == "ink" }
            workspace.resizeAnnotation(ink, larger = true)
            runCurrent()
            assertTrue(store.saved.first { it.key == ink.key }.position.width > ink.position.width)
            val rectangle = workspace.state.annotations.first { it.type == "image" }
            val from = workspace.state.page!!.geometry.displayRect(rectangle.position.rects.single())
            workspace.transformAnnotation(rectangle, 0, from, from.translate(Offset(10f, 10f)))
            runCurrent()
            assertTrue(store.saved.first { it.key == rectangle.key }.position.rects != rectangle.position.rects)
            workspace.deleteAnnotation(note)
            runCurrent()
            assertFalse(store.saved.any { it.key == note.key })
        }
    }

    @Test
    fun failedSaveRetainsChangesAndRetryPersistsThem() = runTest {
        val store = MemoryAnnotations()
        withWorkspace(store) { workspace ->
            store.failSave = true
            workspace.beginNote()
            workspace.changeDraft("Do not lose this", "#ffd400")
            workspace.saveDraft()
            runCurrent()
            assertEquals(1, workspace.state.annotations.size)
            assertEquals(1, workspace.state.pendingWrites)
            assertTrue(workspace.state.needsRefresh)
            assertNotNull(workspace.state.error)
            assertTrue(store.saved.isEmpty())

            store.failSave = false
            workspace.refreshAnnotations()
            runCurrent()
            assertEquals("Do not lose this", store.saved.single().comment)
            assertEquals(0, workspace.state.pendingWrites)
            assertFalse(workspace.state.needsRefresh)
        }
    }

    @Test
    fun failedLoadCannotOverwriteSavedAnnotations() = runTest {
        val store = MemoryAnnotations().apply { failLoad = true }
        withWorkspace(store) { workspace ->
            assertFalse(workspace.state.canWrite)
            assertTrue(workspace.state.needsRefresh)
            workspace.beginNote()
            workspace.saveDraft()
            runCurrent()
            assertEquals(0, store.saveCount)

            store.failLoad = false
            workspace.refreshAnnotations()
            runCurrent()
            assertTrue(workspace.state.canWrite)
        }
    }

    @Test
    fun agentAnnotationsAlsoSaveLocally() = runTest {
        val store = MemoryAnnotations()
        withWorkspace(store) { workspace ->
            val keys = workspace.agentCreateAnnotations(listOf(
                AnnotationDraft("note", 0, listOf(listOf(10f, 10f, 30f, 30f)), comment = "Agent note"),
            ))
            assertTrue(store.saved.single().isAiGenerated)
            workspace.agentUpdateAnnotation(keys.single(), "Updated agent note", "#ff6666")
            assertEquals("Updated agent note", store.saved.single().comment)
            workspace.agentDeleteAnnotation(keys.single())
            assertTrue(store.saved.isEmpty())
        }
    }

    @Test
    fun localPdfNeverUsesConfiguredZoteroAccount() = runTest {
        val store = MemoryAnnotations()
        withWorkspace(store) { workspace ->
            workspace.updateConfig(ZoteroConfig(apiKey = "unused-test-key", userId = "1"))
            workspace.openLocal(byteArrayOf(), "Local paper", KEY)
            runCurrent()
            workspace.beginNote()
            workspace.changeDraft("Local, even with an account", "#ffd400")
            workspace.saveDraft()
            runCurrent()
            workspace.agentCreateAnnotations(listOf(
                AnnotationDraft("note", 0, listOf(listOf(10f, 10f, 30f, 30f)), comment = "Local agent note"),
            ))
            assertEquals(2, store.saved.size)
            assertEquals(0, workspace.state.pendingWrites)
        }
    }

    @Test
    fun fileStoreRoundTripsAndIsolatesAttachments() = runTest {
        val directory = Files.createTempDirectory("paperconnekt-annotation-test").toFile()
        try {
            val store = StoredLocalPdfAnnotations(DesktopPdfAttachmentStorage(directory))
            val payload = annotationPayload(KEY,
                AnnotationDraft("note", 0, listOf(listOf(10f, 10f, 30f, 30f)), comment = "Saved on disk", key = "TESTNOTE"))
            val annotations = listOf(ZoteroAnnotation("TESTNOTE", 0, payload))
            store.save(KEY, annotations)
            val reopened = StoredLocalPdfAnnotations(DesktopPdfAttachmentStorage(directory))
            assertEquals(annotations, reopened.load(KEY))
            assertTrue(reopened.load("replacement-attachment").isEmpty())
            store.save(KEY, emptyList())
            assertTrue(reopened.load(KEY).isEmpty())
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun corruptAndUnsupportedSnapshotsAreNotSilentlyReplaced() = runTest {
        val directory = Files.createTempDirectory("paperconnekt-corrupt-annotations-test").toFile()
        try {
            val storage = DesktopPdfAttachmentStorage(directory)
            val store = StoredLocalPdfAnnotations(storage)
            for (content in listOf("invalid json", """{"version":2,"annotations":[]}""")) {
                storage.writeAnnotations(KEY, content)
                assertTrue(runCatching { store.load(KEY) }.isFailure)
                assertEquals(content, storage.readAnnotations(KEY))
            }
        } finally {
            directory.deleteRecursively()
        }
    }

    private suspend fun TestScope.withWorkspace(
        store: MemoryAnnotations,
        block: suspend TestScope.(PdfWorkspaceViewModel) -> Unit,
    ) {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val workspace = PdfWorkspaceViewModel(
            repository = NoZoteroCalls(),
            localAnnotationStore = store,
        )
        try {
            workspace.openLocal(byteArrayOf(), "Local paper", KEY)
            workspace.attachPageProvider(1) {
                PdfPage(
                    PdfGeometry(0f, 0f, 100f, 100f, 0),
                    listOf(PdfGlyph("Selectable text", Rect(10f, 10f, 80f, 20f))),
                )
            }
            runCurrent()
            block(workspace)
        } finally {
            workspace.close()
            Dispatchers.resetMain()
        }
    }

    private class MemoryAnnotations : LocalPdfAnnotationStore {
        var saved = emptyList<ZoteroAnnotation>()
        var failLoad = false
        var failSave = false
        var saveCount = 0
        override suspend fun load(key: String): List<ZoteroAnnotation> {
            check(!failLoad) { "Unreadable annotation file" }
            return saved
        }
        override suspend fun save(key: String, annotations: List<ZoteroAnnotation>) {
            check(!failSave) { "Disk full" }
            saveCount++
            saved = annotations
        }
    }

    private class NoZoteroCalls : PdfWorkspaceRepository {
        override suspend fun download(config: ZoteroConfig, attachment: ZoteroItem): ByteArray = error("Unexpected Zotero call")
        override suspend fun annotations(config: ZoteroConfig, attachmentKey: String): List<ZoteroAnnotation> = error("Unexpected Zotero call")
        override suspend fun create(config: ZoteroConfig, data: JsonObject): String = error("Unexpected Zotero call")
        override suspend fun update(config: ZoteroConfig, annotation: ZoteroAnnotation, comment: String, color: String): Unit = error("Unexpected Zotero call")
        override suspend fun updateTextRange(config: ZoteroConfig, annotation: ZoteroAnnotation, draft: AnnotationDraft): Unit = error("Unexpected Zotero call")
        override suspend fun repairInkPosition(config: ZoteroConfig, annotation: ZoteroAnnotation): Unit = error("Unexpected Zotero call")
        override suspend fun updatePosition(config: ZoteroConfig, annotation: ZoteroAnnotation, comment: String, position: JsonObject): Unit = error("Unexpected Zotero call")
        override suspend fun delete(config: ZoteroConfig, annotation: ZoteroAnnotation): Unit = error("Unexpected Zotero call")
    }

    private companion object {
        const val KEY = "local/paper-1/attachment-1"
    }
}
