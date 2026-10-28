package com.paperconnekt.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.paperconnekt.app.PaperConnektApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PaperConnektApp(secretStore = AndroidSecretStore(applicationContext))
        }
    }
}
