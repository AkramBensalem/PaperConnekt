package com.paperconnekt.app

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

enum class NavigationGlyph(
    val value: String,
) {
    DASHBOARD("▦"),
    ASSISTANT("✦"),
    GRAPH("◇"),
    LIBRARY("▰"),
    SETTINGS("⚙"),
}

@Composable
fun NavigationIcon(
    glyph: NavigationGlyph,
    contentDescription: String,
) {
    Text(
        text = glyph.value,
        fontSize = 20.sp,
        modifier = Modifier.semantics {
            this.contentDescription = contentDescription
        },
        textAlign = TextAlign.Center,
    )
}
