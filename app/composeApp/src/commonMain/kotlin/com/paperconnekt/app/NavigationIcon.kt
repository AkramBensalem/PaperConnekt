package com.paperconnekt.app

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import com.paperconnekt.app.resources.Res
import com.paperconnekt.app.resources.book
import com.paperconnekt.app.resources.dashboard
import com.paperconnekt.app.resources.lightbulb
import com.paperconnekt.app.resources.settings
import com.paperconnekt.app.resources.trending_up
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

enum class NavigationGlyph {
    DASHBOARD,
    ASSISTANT,
    GRAPH,
    LIBRARY,
    SETTINGS,
}

@Composable
fun NavigationIcon(
    glyph: NavigationGlyph,
    contentDescription: String,
) {
    val iconResource: DrawableResource = when (glyph) {
        NavigationGlyph.DASHBOARD -> Res.drawable.dashboard
        NavigationGlyph.ASSISTANT -> Res.drawable.lightbulb
        NavigationGlyph.GRAPH -> Res.drawable.trending_up
        NavigationGlyph.LIBRARY -> Res.drawable.book
        NavigationGlyph.SETTINGS -> Res.drawable.settings
    }

    Icon(
        painter = painterResource(iconResource),
        contentDescription = contentDescription,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .alpha(0.92f)
            .size(24.dp),
    )
}
