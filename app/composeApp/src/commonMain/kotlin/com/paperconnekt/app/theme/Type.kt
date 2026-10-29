package com.paperconnekt.app.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.paperconnekt.app.resources.Res
import com.paperconnekt.app.resources.dm_sans_bold
import com.paperconnekt.app.resources.dm_sans_medium
import com.paperconnekt.app.resources.dm_sans_regular
import com.paperconnekt.app.resources.dm_sans_semibold
import org.jetbrains.compose.resources.Font

val PaperConnektTypography: Typography
    @Composable get() {
        val family = FontFamily(
            Font(Res.font.dm_sans_regular, FontWeight.Normal),
            Font(Res.font.dm_sans_medium, FontWeight.Medium),
            Font(Res.font.dm_sans_semibold, FontWeight.SemiBold),
            Font(Res.font.dm_sans_bold, FontWeight.Bold),
        )

        fun style(
            size: Int,
            lineHeight: Int,
            weight: FontWeight = FontWeight.Normal,
            tracking: Float = 0f,
        ) = TextStyle(
            fontFamily = family,
            fontWeight = weight,
            fontSize = size.sp,
            lineHeight = lineHeight.sp,
            letterSpacing = tracking.sp,
        )

        return Typography(
            displayLarge = style(57, 64, FontWeight.SemiBold, -1.2f),
            displayMedium = style(45, 52, FontWeight.SemiBold, -0.8f),
            displaySmall = style(36, 44, FontWeight.SemiBold, -0.6f),
            headlineLarge = style(30, 38, FontWeight.SemiBold, -0.5f),
            headlineMedium = style(25, 34, FontWeight.SemiBold, -0.3f),
            headlineSmall = style(21, 29, FontWeight.SemiBold, -0.2f),
            titleLarge = style(22, 30, FontWeight.SemiBold, -0.2f),
            titleMedium = style(16, 24, FontWeight.SemiBold),
            titleSmall = style(14, 20, FontWeight.SemiBold),
            bodyLarge = style(16, 26),
            bodyMedium = style(14, 22),
            bodySmall = style(12, 18),
            labelLarge = style(14, 20, FontWeight.SemiBold, 0.1f),
            labelMedium = style(12, 18, FontWeight.Medium, 0.1f),
            labelSmall = style(11, 16, FontWeight.Medium, 0.3f),
        )
    }
