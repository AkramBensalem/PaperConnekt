package com.paperconnekt.app.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import com.paperconnekt.domain.model.LanguagePreference

@Composable
fun AppLocale(
    language: LanguagePreference,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalAppLocale provides language.resourceTag,
        LocalLayoutDirection provides language.layoutDirection,
    ) {
        key(language) {
            content()
        }
    }
}

private val LanguagePreference.resourceTag: String
    get() = when (this) {
        LanguagePreference.ENGLISH -> "en"
        LanguagePreference.FRENCH -> "fr"
        LanguagePreference.ARABIC -> "ar"
    }

private val LanguagePreference.layoutDirection: LayoutDirection
    get() = if (this == LanguagePreference.ARABIC) {
        LayoutDirection.Rtl
    } else {
        LayoutDirection.Ltr
    }
