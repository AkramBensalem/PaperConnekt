package com.paperconnekt.app

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.paperconnekt.app.resources.Res
import com.paperconnekt.app.resources.filled_attach_file
import com.paperconnekt.app.resources.filled_description
import com.paperconnekt.storage.PdfAttachmentStorage
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource

data class PdfUploadFile(
    val name: String,
    val bytes: ByteArray,
    val mtime: Long,
    val md5: String,
)

interface PdfAttachmentUi {
    val storage: PdfAttachmentStorage
    suspend fun choosePdf(): String?
    suspend fun choosePdfForUpload(): PdfUploadFile?

    @Composable
    fun dropTarget(
        enabled: Boolean,
        onHover: (Boolean) -> Unit,
        onDrop: (List<String>) -> Unit,
        onError: (String) -> Unit,
    ): Modifier
}

val LocalPdfAttachments = staticCompositionLocalOf<PdfAttachmentUi> {
    error("The platform must provide PDF attachment support.")
}

@Composable
internal fun PdfAttachmentCard(
    pdfPath: String?,
    onAttached: suspend (String) -> Unit,
    onOpen: (() -> Unit)? = null,
    onBusyChanged: (Boolean) -> Unit = {},
    enabled: Boolean = true,
) {
    val platform = LocalPdfAttachments.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var hovering by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var pendingReplacement by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(busy) { onBusyChanged(busy) }
    val background by animateColorAsState(
        if (hovering) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
    )

    fun attach(source: String) {
        if (busy || !enabled) return
        busy = true
        error = null
        message = null
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                withContext(NonCancellable) {
                    val path = platform.storage.importPdf(source)
                    onAttached(path)
                }
                message = "PDF attached and saved on this device."
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                Napier.e("Could not attach PDF", exception)
                error = exception.message ?: "Could not attach PDF. Please try again."
            } finally {
                busy = false
            }
        }
    }

    fun accept(sources: List<String>) {
        if (busy || !enabled) return
        error = null
        message = null
        if (sources.size != 1) {
            error = "Choose one PDF per item."
        } else if (pdfPath != null) {
            pendingReplacement = sources.single()
        } else {
            attach(sources.single())
        }
    }

    val dropModifier = platform.dropTarget(
        enabled = enabled && !busy && pendingReplacement == null,
        onHover = { hovering = it },
        onDrop = ::accept,
        onError = { error = it },
    )
    Surface(
        modifier = Modifier.fillMaxWidth().then(dropModifier),
        color = background,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            if (hovering) 2.dp else 1.dp,
            if (hovering) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (pdfPath != null) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerHighest,
            ) {
                if (busy) {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp).size(24.dp), strokeWidth = 2.dp)
                } else {
                    Icon(
                        painterResource(if (pdfPath == null) Res.drawable.filled_attach_file else Res.drawable.filled_description),
                        contentDescription = null,
                        modifier = Modifier.padding(16.dp).size(24.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Text(
                if (busy) "Copying your PDF..." else if (hovering) "Drop to attach" else if (pdfPath == null) "Drop your PDF here" else "Ready to read",
                style = MaterialTheme.typography.titleSmall,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            if (pdfPath != null) {
                Text(
                    pdfPath.substringAfterLast('/').substringAfterLast('\\'),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
            Text(
                if (pdfPath == null) "Or browse to choose a file.\nOne PDF per paper."
                else "An independent copy is stored on this device.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (pdfPath != null && onOpen != null) {
                Button(onClick = onOpen, enabled = enabled && !busy, modifier = Modifier.fillMaxWidth()) { Text("Open PDF") }
            }
            OutlinedButton(
                enabled = enabled && !busy && pendingReplacement == null,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    busy = true
                    error = null
                    scope.launch {
                        val source = try {
                            platform.choosePdf()
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (exception: Exception) {
                            Napier.e("Could not choose PDF", exception)
                            error = exception.message ?: "Could not open the file picker."
                            return@launch
                        } finally {
                            busy = false
                        }
                        if (source != null) accept(listOf(source))
                    }
                },
            ) { Text(if (pdfPath == null) "Browse PDF" else "Replace PDF") }
            (error ?: message)?.let { status ->
                Text(
                    status,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }
        }
    }

    pendingReplacement?.let { source ->
        AlertDialog(
            onDismissRequest = { pendingReplacement = null },
            title = { Text("Replace attached PDF?") },
            text = { Text("This item will open the new PDF instead. The original source file will not be changed.") },
            confirmButton = {
                Button(onClick = { pendingReplacement = null; attach(source) }) { Text("Replace PDF") }
            },
            dismissButton = { TextButton(onClick = { pendingReplacement = null }) { Text("Cancel") } },
        )
    }
}
