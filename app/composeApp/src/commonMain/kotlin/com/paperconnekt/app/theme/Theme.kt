package com.paperconnekt.app.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.lerp
import com.paperconnekt.domain.model.ThemePreference

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun PaperConnektTheme(
    preference: ThemePreference = ThemePreference.SYSTEM,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (preference) {
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
    }

    val colors = if (darkTheme) PaperConnektDarkColors else PaperConnektLightColors
    val interactions = remember(colors) {
        RippleConfiguration(
            // Keep press and focus feedback visible without a rectangular hover wash.
            color = lerp(colors.primary, colors.onPrimary, 0.35f),
            rippleAlpha = RippleAlpha(
                hoveredAlpha = 0f,
                focusedAlpha = 0.12f,
                pressedAlpha = 0.12f,
                draggedAlpha = 0.16f,
            ),
        )
    }

    MaterialTheme(
        colorScheme = colors,
        typography = PaperConnektTypography,
        shapes = PaperConnektShapes,
    ) {
        CompositionLocalProvider(
            LocalRippleConfiguration provides interactions,
            content = content,
        )
    }
}
