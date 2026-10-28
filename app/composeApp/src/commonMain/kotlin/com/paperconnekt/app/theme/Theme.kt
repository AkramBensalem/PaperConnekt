package com.paperconnekt.app.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.paperconnekt.domain.model.ThemePreference

@Composable
fun PaperConnektTheme(
    preference: ThemePreference = ThemePreference.SYSTEM,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (preference) {
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
    }

    MaterialTheme(
        colorScheme = if (darkTheme) PaperConnektDarkColors else PaperConnektLightColors,
        typography = PaperConnektTypography,
        shapes = PaperConnektShapes,
        content = content,
    )
}
