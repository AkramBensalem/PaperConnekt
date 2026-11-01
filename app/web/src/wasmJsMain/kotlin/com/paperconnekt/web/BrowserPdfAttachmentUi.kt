package com.paperconnekt.web

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.paperconnekt.app.PdfAttachmentUi
import com.paperconnekt.app.PdfUploadFile
import com.paperconnekt.storage.PdfAttachmentStorage
import com.paperconnekt.storage.requirePdfHeader
import kotlin.io.encoding.Base64
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class BrowserPdfAttachmentUi : PdfAttachmentUi {
    override val storage: PdfAttachmentStorage = BrowserPdfAttachmentStorage()

    override suspend fun choosePdf(): String? = pickPdf()

    override suspend fun choosePdfForUpload(): PdfUploadFile? {
        val picked = pickPdfDetails() ?: return null
        val bytes = Base64.decode(picked.dataUrl.substringAfter("base64,"))
        requirePdfHeader(bytes)
        return PdfUploadFile(
            name = picked.name,
            bytes = bytes,
            mtime = picked.lastModified,
            md5 = "",
        )
    }

    @Composable
    override fun dropTarget(
        enabled: Boolean,
        onHover: (Boolean) -> Unit,
        onDrop: (List<String>) -> Unit,
        onError: (String) -> Unit,
    ): Modifier = Modifier
}

private data class PickedPdf(
    val name: String,
    val dataUrl: String,
    val lastModified: Long,
)

private suspend fun pickPdf(): String? = pickPdfDetails()?.dataUrl

private suspend fun pickPdfDetails(): PickedPdf? = suspendCancellableCoroutine { continuation ->
    pickPdfFromBrowser(
        onPicked = { name, dataUrl, lastModified ->
            if (continuation.isActive) {
                continuation.resume(PickedPdf(name, dataUrl, lastModified.toLong()))
            }
        },
        onCancel = { if (continuation.isActive) continuation.resume(null) },
    )
}

private fun pickPdfFromBrowser(
    onPicked: (String, String, Double) -> Unit,
    onCancel: () -> Unit,
): Unit = js(
    """
    {
        const input = document.createElement("input");
        input.type = "file";
        input.accept = "application/pdf,.pdf";
        input.style.display = "none";
        input.oncancel = function() { input.remove(); onCancel(); };
        input.onchange = function() {
            const file = input.files && input.files[0];
            input.remove();
            if (!file) { onCancel(); return; }
            if (file.type !== "application/pdf" && !file.name.toLowerCase().endsWith(".pdf")) {
                onCancel();
                return;
            }
            const reader = new FileReader();
            reader.onload = function() {
                onPicked(file.name, String(reader.result), file.lastModified);
            };
            reader.onerror = function() { onCancel(); };
            reader.readAsDataURL(file);
        };
        document.body.appendChild(input);
        input.click();
    }
    """,
)
