package com.paperconnekt.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.paperconnekt.app.PaperConnektApp
import com.paperconnekt.storage.AndroidWorkspaceStorage
import com.akram.bensalem.paper.connekt.core.settings.setAndroidApplicationContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setAndroidApplicationContext(applicationContext)
        setContent {
            PaperConnektApp(
                secretStore = AndroidSecretStore(applicationContext),
                workspaceStorage = AndroidWorkspaceStorage(applicationContext),
            )
        }
    }
}
