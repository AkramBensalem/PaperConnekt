package com.paperconnekt.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class AppSettings(
    val onboardingCompleted: Boolean = false,
    val workspaceName: String = "My research",
    val zoteroEnabled: Boolean = false,
    val selectedTheme: ThemePreference = ThemePreference.SYSTEM,
)

@Serializable
enum class ThemePreference {
    SYSTEM,
    LIGHT,
    DARK,
}
