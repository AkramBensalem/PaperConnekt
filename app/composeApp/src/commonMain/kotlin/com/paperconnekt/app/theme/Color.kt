package com.paperconnekt.app.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

object PaperConnektColors {
    val Blue = Color(0xFF2457A6)
    val Teal = Color(0xFF2E7D63)
    val Amber = Color(0xFFB86B00)
}

val PaperConnektLightColors = lightColorScheme(
    primary = Color(0xFF2457A6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9E6FF),
    onPrimaryContainer = Color(0xFF001A41),
    secondary = Color(0xFF52627A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCE5F3),
    onSecondaryContainer = Color(0xFF0E1C2E),
    tertiary = Color(0xFF356B68),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFBCEDE7),
    onTertiaryContainer = Color(0xFF00201E),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF1F4F8),
    onBackground = Color(0xFF191C20),
    surface = Color.White,
    onSurface = Color(0xFF191C20),
    surfaceVariant = Color(0xFFE5EAF1),
    onSurfaceVariant = Color(0xFF44474F),
    outline = Color(0xFF7B8798),
    outlineVariant = Color(0xFFC2CEDD),
    inverseSurface = Color(0xFF283444),
    inverseOnSurface = Color(0xFFF1F4F8),
    inversePrimary = Color(0xFFA9C7FF),
    surfaceDim = Color(0xFFDCE3ED),
    surfaceBright = Color.White,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF7F9FC),
    surfaceContainer = Color(0xFFEDF2F8),
    surfaceContainerHigh = Color(0xFFE3EAF3),
    surfaceContainerHighest = Color(0xFFD8E2EF),
)

val PaperConnektDarkColors = darkColorScheme(
    primary = Color(0xFFA9C7FF),
    onPrimary = Color(0xFF002E6A),
    primaryContainer = Color(0xFF0B478F),
    onPrimaryContainer = Color(0xFFD9E6FF),
    secondary = Color(0xFFBCC7DC),
    onSecondary = Color(0xFF263143),
    secondaryContainer = Color(0xFF3C485C),
    onSecondaryContainer = Color(0xFFDCE5F3),
    tertiary = Color(0xFFA0D1CB),
    onTertiary = Color(0xFF003733),
    tertiaryContainer = Color(0xFF1B514D),
    onTertiaryContainer = Color(0xFFBCEDE7),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0C1118),
    onBackground = Color(0xFFE2E2E9),
    surface = Color(0xFF151C26),
    onSurface = Color(0xFFE2E2E9),
    surfaceVariant = Color(0xFF283444),
    onSurfaceVariant = Color(0xFFC4C6D0),
    outline = Color(0xFF8E9099),
    outlineVariant = Color(0xFF425268),
    inverseSurface = Color(0xFFE5EAF1),
    inverseOnSurface = Color(0xFF283444),
    inversePrimary = Color(0xFF2457A6),
    surfaceDim = Color(0xFF0C1118),
    surfaceBright = Color(0xFF334156),
    surfaceContainerLowest = Color(0xFF090E15),
    surfaceContainerLow = Color(0xFF151C26),
    surfaceContainer = Color(0xFF1C2634),
    surfaceContainerHigh = Color(0xFF273447),
    surfaceContainerHighest = Color(0xFF34445B),
)
