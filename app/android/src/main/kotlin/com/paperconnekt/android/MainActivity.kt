package com.paperconnekt.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.remember
import com.paperconnekt.app.PaperConnektApp
import com.paperconnekt.storage.AndroidWorkspaceStorage
import com.akram.bensalem.paper.connekt.core.settings.setAndroidApplicationContext
import kotlinx.coroutines.CompletableDeferred

class MainActivity : ComponentActivity() {
    private var pdfSelection: CompletableDeferred<String?>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setAndroidApplicationContext(applicationContext)
        setContent {
            val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                pdfSelection?.complete(uri?.toString())
            }
            val pdfAttachments = remember(picker) {
                AndroidPdfAttachmentUi(this) {
                    check(pdfSelection == null) { "A file picker is already open." }
                    val result = CompletableDeferred<String?>()
                    pdfSelection = result
                    try {
                        picker.launch(arrayOf("application/pdf"))
                        result.await()
                    } finally {
                        pdfSelection = null
                    }
                }
            }
            PaperConnektApp(
                secretStore = AndroidSecretStore(applicationContext),
                workspaceStorage = AndroidWorkspaceStorage(applicationContext),
                pdfAttachments = pdfAttachments,
            )
        }
    }
}
