package com.paperconnekt.android

import android.app.Activity
import android.content.ClipDescription
import java.security.MessageDigest
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import com.paperconnekt.app.PdfAttachmentUi
import com.paperconnekt.app.PdfUploadFile
import com.paperconnekt.storage.AndroidPdfAttachmentStorage

class AndroidPdfAttachmentUi(
    private val activity: Activity,
    private val picker: suspend () -> String?,
) : PdfAttachmentUi {
    override val storage = AndroidPdfAttachmentStorage(activity)
    override suspend fun choosePdf(): String? = picker()

    override suspend fun choosePdfForUpload(): PdfUploadFile? {
        val source = picker() ?: return null
        val imported = storage.importPdf(source)
        val bytes = storage.readPdf(imported)
        return PdfUploadFile(
            name = imported.substringAfterLast('/').substringAfterLast('\\'),
            bytes = bytes,
            mtime = System.currentTimeMillis(),
            md5 = MessageDigest.getInstance("MD5")
                .digest(bytes)
                .joinToString("") { byte -> "%02x".format(byte) },
        )
    }

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
                    val native = event.toAndroidDragEvent()
                    val clip = native.clipData
                    if (clip == null || clip.itemCount != 1) {
                        currentError.value("Choose one PDF per item.")
                        return false
                    }
                    val uri = clip.getItemAt(0).uri
                    if (uri == null) {
                        currentError.value("Drop a PDF file, not a link or text.")
                        return false
                    }
                    // The grant remains valid for this activity, including replacement confirmation.
                    try {
                        activity.requestDragAndDropPermissions(native)
                    } catch (exception: SecurityException) {
                        currentError.value(exception.message ?: "Permission to read the dropped PDF was denied.")
                        return false
                    }
                    currentDrop.value(listOf(uri.toString()))
                    return true
                }
            }
        }
        return Modifier.dragAndDropTarget(
            shouldStartDragAndDrop = {
                enabled && it.toAndroidDragEvent().clipDescription?.let { description ->
                    description.hasMimeType("application/pdf") ||
                        description.hasMimeType("application/octet-stream") ||
                        description.hasMimeType(ClipDescription.MIMETYPE_TEXT_URILIST)
                } == true
            },
            target = target,
        )
    }
}
