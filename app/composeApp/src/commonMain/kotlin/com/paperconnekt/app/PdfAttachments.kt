package com.paperconnekt.app

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

interface PdfAttachmentUi {
    val storage: PdfAttachmentStorage
    suspend fun choosePdf(): String?

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
) {
    val platform = LocalPdfAttachments.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var hovering by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var pendingReplacement by remember { mutableStateOf<String?>(null) }
    val background by animateColorAsState(
        if (hovering) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
    )

    fun attach(source: String) {
        if (busy) return
        busy = true
        error = null
        message = null
        scope.launch {
            try {
                val path = platform.storage.importPdf(source)
                onAttached(path)
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
        if (busy) return
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
        enabled = !busy && pendingReplacement == null,
        onHover = { hovering = it },
        onDrop = ::accept,
        onError = { error = it },
    )
    Surface(
        modifier = Modifier.fillMaxWidth().then(dropModifier),
        color = background,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(
            if (hovering) 2.dp else 1.dp,
            if (hovering) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (busy) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Icon(
                        painterResource(if (pdfPath == null) Res.drawable.filled_attach_file else Res.drawable.filled_description),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    if (busy) "Attaching PDF..." else if (hovering) "Drop PDF here" else if (pdfPath == null) "Attach a PDF" else "PDF attached",
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            if (pdfPath != null) {
                Text(
                    pdfPath.substringAfterLast('/').substringAfterLast('\\'),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                "Drop one PDF here or browse files. A copy stays on this device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (pdfPath != null && onOpen != null) {
                Button(onClick = onOpen, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Open PDF") }
            }
            OutlinedButton(
                enabled = !busy && pendingReplacement == null,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    busy = true
                    error = null
                    scope.launch {
                        try {
                            val source = platform.choosePdf()
                            busy = false
                            if (source != null) accept(listOf(source))
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (exception: Exception) {
                            Napier.e("Could not choose PDF", exception)
                            error = exception.message ?: "Could not open the file picker."
                        } finally {
                            // Import owns the busy state after a successful selection.
                            if (error != null) busy = false
                        }
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
