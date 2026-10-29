package com.paperconnekt.app

import com.akram.bensalem.paper.connekt.feature.assistant.FileIdeaNoteStore
import com.akram.bensalem.paper.connekt.feature.assistant.FilePaperAgentChatStore
import com.akram.bensalem.paper.connekt.feature.assistant.Idea
import com.paperconnekt.domain.model.Paper
import com.paperconnekt.domain.model.ResearchGraph
import com.paperconnekt.storage.DesktopPdfAttachmentStorage
import com.paperconnekt.storage.DesktopWorkspaceStorage
import com.paperconnekt.storage.PersistentResearchStore
import com.paperconnekt.storage.WorkspaceStorage
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ResearchPersistenceTest {
    @Test
    fun discoveredPapersStayInTheirGraphUntilExplicitlySaved() = runTest {
        withTemporaryDirectory { directory ->
            val storage = DesktopWorkspaceStorage(directory.toFile())
            val store = PersistentResearchStore(storage)
            store.initialize()
            val result = Paper(id = "W123", title = "Discovery")
            store.saveGraph(
                ResearchGraph(
                    id = "graph",
                    name = "Research",
                    paperIds = listOf(result.id),
                    discoveredPapers = listOf(result),
                ),
            )
            val reopened = PersistentResearchStore(storage)
            reopened.initialize()
            assertTrue(reopened.papers.value.isEmpty())
            assertEquals(listOf(result), reopened.graphs.value.single().discoveredPapers)
            reopened.savePaper(result)
            assertEquals(listOf(result), reopened.papers.value)
        }
    }

    @Test
    fun copiedPdfAndItsAssociationSurviveSourceDeletionAndReopening() = runTest {
        withTemporaryDirectory { directory ->
            val storage = DesktopWorkspaceStorage(directory.resolve("data").toFile())
            val attachments = DesktopPdfAttachmentStorage(directory.resolve("data/attachments").toFile())
            val source = directory.resolve("source.pdf")
            val bytes = "%PDF-1.7\nsample".toByteArray()
            Files.write(source, bytes)
            val path = attachments.importPdf(source.toString())
            val store = PersistentResearchStore(storage)
            store.initialize()
            store.savePaper(Paper(id = "paper", title = "Saved item"))
            store.updatePaper("paper") { it.copy(pdfPath = path) }
            store.savePaper(Paper(id = "paper", title = "Refreshed metadata"))
            store.updatePaper("paper") { it.copy(completed = true) }
            Files.delete(source)
            val reopened = PersistentResearchStore(storage)
            reopened.initialize()
            assertEquals(path, reopened.papers.value.single().pdfPath)
            assertTrue(reopened.papers.value.single().completed)
            assertTrue(bytes.contentEquals(attachments.readPdf(path)))
            if (directory.fileSystem.supportedFileAttributeViews().contains("posix")) {
                assertEquals(
                    PosixFilePermissions.fromString("rwx------"),
                    Files.getPosixFilePermissions(directory.resolve("data")),
                )
                assertEquals(
                    PosixFilePermissions.fromString("rw-------"),
                    Files.getPosixFilePermissions(Path.of(path)),
                )
            }
        }
    }

    @Test
    fun failedWorkspaceWriteDoesNotPublishOrLosePreviouslySavedItems() = runTest {
        val storage = FailingWorkspaceStorage()
        val store = PersistentResearchStore(storage)
        store.initialize()
        val paper = Paper(id = "paper", title = "Saved")
        store.savePaper(paper)
        storage.failWrites = true
        assertFailsWith<IllegalStateException> {
            store.updatePaper(paper.id) { it.copy(pdfPath = "/managed/document.pdf") }
        }
        assertEquals(listOf(paper), store.papers.value)
        storage.failWrites = false
        store.updatePaper(paper.id) { it.copy(completed = true) }
        val reopened = PersistentResearchStore(storage)
        reopened.initialize()
        assertTrue(reopened.papers.value.single().completed)
        assertEquals(paper.id, reopened.papers.value.single().id)
    }

    @Test
    fun ideaMarkdownPreservesPaperAssociationPositionAndColor() = runTest {
        withTemporaryDirectory { directory ->
            val store = FileIdeaNoteStore(directory.resolve("ideas"))
            val idea = Idea("idea", "Title", "A note", x = 132f, y = 247f, pageColor = "sage")
            store.save("paper-one", listOf(idea))
            val reopened = FileIdeaNoteStore(directory.resolve("ideas"))
            assertEquals(listOf(idea), reopened.load("paper-one"))
            assertTrue(reopened.load("paper-two").isEmpty())
        }
    }

    @Test
    fun corruptChatHistoryFailsWithoutBecomingAnEmptySuccessfulLoad() = runTest {
        withTemporaryDirectory { directory ->
            val file = directory.resolve("local/paper.json")
            Files.createDirectories(file.parent)
            Files.writeString(file, "not valid JSON")
            val store = FilePaperAgentChatStore(directory)
            assertTrue(runCatching { store.load("local/paper") }.isFailure)
            assertEquals("not valid JSON", Files.readString(file))
        }
    }

    @Test
    fun readerRejectsUnmanagedFilesWithoutDeletingThem() = runTest {
        withTemporaryDirectory { directory ->
            val source = directory.resolve("original.pdf")
            Files.writeString(source, "%PDF-1.7\nsample")
            val storage = DesktopPdfAttachmentStorage(directory.resolve("attachments").toFile())
            assertFailsWith<IllegalArgumentException> { storage.readPdf(source.toString()) }
            assertTrue(Files.exists(source))
            assertFalse(Files.exists(directory.resolve("attachments")))
        }
    }

    private suspend fun withTemporaryDirectory(block: suspend (Path) -> Unit) {
        val directory = Files.createTempDirectory("paperconnekt-persistence-")
        try {
            block(directory)
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    private class FailingWorkspaceStorage : WorkspaceStorage {
        private var content: String? = null
        var failWrites = false
        override suspend fun read(): String? = content
        override suspend fun write(content: String) {
            check(!failWrites) { "Disk unavailable" }
            this.content = content
        }
        override suspend fun quarantineCorruptData() = error("Unexpected corrupt data")
    }
}
