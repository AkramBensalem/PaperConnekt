package com.paperconnekt.app.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

object PaperConnektColors {
    val Blue = Color(0xFF2457A6)
    val Teal = Color(0xFF2E7D63)
    val WarmTerracotta = Color(0xFFA85D3A)
}

val PaperConnektLightColors = lightColorScheme(
    primary = PaperConnektColors.Blue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9E6FF),
    onPrimaryContainer = Color(0xFF001A41),
    secondary = Color(0xFF53627C),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE4EAF5),
    onSecondaryContainer = Color(0xFF24334D),
    tertiary = PaperConnektColors.WarmTerracotta,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF7DDD0),
    onTertiaryContainer = Color(0xFF3B160B),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF5F7FB),
    onBackground = Color(0xFF1D293E),
    surface = Color.White,
    onSurface = Color(0xFF1D293E),
    surfaceVariant = Color(0xFFE5EBF5),
    onSurfaceVariant = Color(0xFF53627A),
    outline = Color(0xFF71829E),
    outlineVariant = Color(0xFFC5D0E1),
    inverseSurface = Color(0xFF26334B),
    inverseOnSurface = Color(0xFFF1F4F8),
    inversePrimary = Color(0xFFA9C7FF),
    surfaceDim = Color(0xFFDCE3F0),
    surfaceBright = Color.White,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFEDF2FA),
    surfaceContainer = Color(0xFFE7EDF7),
    surfaceContainerHigh = Color(0xFFDDE6F3),
    surfaceContainerHighest = Color(0xFFD3DFEF),
)

val PaperConnektDarkColors = darkColorScheme(
    primary = Color(0xFFA9C7FF),
    onPrimary = Color(0xFF002E6A),
    primaryContainer = Color(0xFF0B478F),
    onPrimaryContainer = Color(0xFFD9E6FF),
    secondary = Color(0xFFBDCAE3),
    onSecondary = Color(0xFF26334B),
    secondaryContainer = Color(0xFF394761),
    onSecondaryContainer = Color(0xFFE0E8F8),
    tertiary = Color(0xFFF2B69A),
    onTertiary = Color(0xFF54210F),
    tertiaryContainer = Color(0xFF713F2B),
    onTertiaryContainer = Color(0xFFF7DDD0),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF101725),
    onBackground = Color(0xFFE0E7F5),
    surface = Color(0xFF172133),
    onSurface = Color(0xFFE0E7F5),
    surfaceVariant = Color(0xFF2B3A53),
    onSurfaceVariant = Color(0xFFBAC7DF),
    outline = Color(0xFF94A6C3),
    outlineVariant = Color(0xFF526582),
    inverseSurface = Color(0xFFE8EDF7),
    inverseOnSurface = Color(0xFF26334B),
    inversePrimary = PaperConnektColors.Blue,
    surfaceDim = Color(0xFF101725),
    surfaceBright = Color(0xFF35455F),
    surfaceContainerLowest = Color(0xFF0C1220),
    surfaceContainerLow = Color(0xFF1D2A40),
    surfaceContainer = Color(0xFF24334B),
    surfaceContainerHigh = Color(0xFF2C3D58),
    surfaceContainerHighest = Color(0xFF354966),
)
