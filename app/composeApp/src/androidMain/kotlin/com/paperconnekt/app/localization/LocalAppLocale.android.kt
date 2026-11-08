package com.paperconnekt.app.localization

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.staticCompositionLocalOf
import java.util.Locale

actual object LocalAppLocale {
    private var defaultLocale: Locale? = null
    private val locale = staticCompositionLocalOf { Locale.getDefault().toLanguageTag() }

    actual val current: String
        @Composable get() = locale.current

    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        if (defaultLocale == null) {
            defaultLocale = Locale.getDefault()
        }
        val selected = value?.let(Locale::forLanguageTag) ?: defaultLocale!!
        Locale.setDefault(selected)
        val configuration = Configuration(LocalConfiguration.current).apply {
            setLocale(selected)
        }
        LocalContext.current.resources.updateConfiguration(
            configuration,
            LocalContext.current.resources.displayMetrics,
        )
        return locale.provides(selected.toLanguageTag())
    }
}
