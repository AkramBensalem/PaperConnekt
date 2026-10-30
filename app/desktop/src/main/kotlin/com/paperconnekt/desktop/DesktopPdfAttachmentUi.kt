package com.paperconnekt.desktop

import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.awtTransferable
import com.paperconnekt.app.PdfAttachmentUi
import com.paperconnekt.app.PdfUploadFile
import com.paperconnekt.storage.DesktopPdfAttachmentStorage
import java.awt.EventQueue
import java.awt.FileDialog
import java.awt.Frame
import java.awt.datatransfer.DataFlavor
import java.io.File
import java.security.MessageDigest
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class DesktopPdfAttachmentUi : PdfAttachmentUi {
    override val storage = DesktopPdfAttachmentStorage()

    override suspend fun choosePdf(): String? = suspendCancellableCoroutine { continuation ->
        EventQueue.invokeLater {
            if (!continuation.isActive) return@invokeLater
            val dialog = FileDialog(null as Frame?, "Attach PDF", FileDialog.LOAD)
            continuation.invokeOnCancellation { EventQueue.invokeLater { dialog.dispose() } }
            try {
                dialog.setFilenameFilter { _, name -> name.endsWith(".pdf", ignoreCase = true) }
                dialog.isVisible = true
                val path = dialog.file?.let { File(dialog.directory, it).absolutePath }
                if (continuation.isActive) continuation.resume(path)
            } catch (exception: Exception) {
                if (continuation.isActive) continuation.resumeWithException(exception)
            } finally {
                dialog.dispose()
            }

            override suspend fun choosePdfForUpload(): PdfUploadFile? = suspendCancellableCoroutine { continuation ->
                EventQueue.invokeLater {
                    if (!continuation.isActive) return@invokeLater
                    val dialog = FileDialog(null as Frame?, "Upload PDF to Zotero", FileDialog.LOAD)
                    continuation.invokeOnCancellation { EventQueue.invokeLater { dialog.dispose() } }
                    try {
                        dialog.setFilenameFilter { _, name -> name.endsWith(".pdf", ignoreCase = true) }
                        dialog.isVisible = true
                        val file = dialog.file?.let { File(dialog.directory, it) }
                        if (continuation.isActive) {
                            continuation.resume(
                                file?.let {
                                    val bytes = it.readBytes()
                                    require(bytes.size >= 5 && bytes.copyOfRange(0, 5).decodeToString() == "%PDF-") {
                                        "Select a valid PDF file."
                                    }
                                    PdfUploadFile(
                                        name = it.name,
                                        bytes = bytes,
                                        mtime = it.lastModified(),
                                        md5 = MessageDigest.getInstance("MD5")
                                            .digest(bytes)
                                            .joinToString("") { byte -> "%02x".format(byte) },
                                    )
                                },
                            )
                        }
                    } catch (exception: Exception) {
                        if (continuation.isActive) continuation.resumeWithException(exception)
                    } finally {
                        dialog.dispose()
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    override fun dropTarget(
        enabled: Boolean,
        onHover: (Boolean) -> Unit,
        onDrop: (List<String>) -> Unit,
        onError: (String) -> Unit,
    ): Modifier {
        val currentEnabled = rememberUpdatedState(enabled)
        val currentHover = rememberUpdatedState(onHover)
        val currentDrop = rememberUpdatedState(onDrop)
        val currentError = rememberUpdatedState(onError)
        val target = remember {
            object : DragAndDropTarget {
                override fun onEntered(event: DragAndDropEvent) { currentHover.value(true) }
                override fun onExited(event: DragAndDropEvent) { currentHover.value(false) }
                override fun onEnded(event: DragAndDropEvent) { currentHover.value(false) }

                override fun onDrop(event: DragAndDropEvent): Boolean {
                    currentHover.value(false)
                    if (!currentEnabled.value) return false
                    return try {
                        val files = event.awtTransferable.getTransferData(DataFlavor.javaFileListFlavor) as? List<*>
                        val paths = files.orEmpty().filterIsInstance<File>().map(File::getAbsolutePath)
                        currentDrop.value(paths)
                        true
                    } catch (exception: Exception) {
                        currentError.value(exception.message ?: "Could not read the dropped file.")
                        false
                    }
                }
            }
        }
        return Modifier.dragAndDropTarget(
            shouldStartDragAndDrop = { enabled && it.awtTransferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor) },
            target = target,
        )
    }
}
