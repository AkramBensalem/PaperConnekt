package com.paperconnekt.app.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.staticCompositionLocalOf
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.js

actual object LocalAppLocale {
    private val locale = staticCompositionLocalOf { "en" }

    actual val current: String
        @Composable get() = locale.current

    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        updateCustomLocale(value)
        return locale.provides(value ?: "en")
    }
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun updateCustomLocale(value: String?) {
    js(
        """
        window.__customLocale = value;
        window.dispatchEvent(new Event("languagechange"));
        """.trimIndent(),
    )
}
