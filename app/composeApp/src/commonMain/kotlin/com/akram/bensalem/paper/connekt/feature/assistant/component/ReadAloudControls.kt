package com.akram.bensalem.paper.connekt.feature.assistant.component

import com.paperconnekt.app.resources.Res
import com.paperconnekt.app.resources.filled_description
import com.paperconnekt.app.resources.filled_download
import com.paperconnekt.app.resources.filled_first_page
import com.paperconnekt.app.resources.filled_last_page
import com.paperconnekt.app.resources.filled_more_vert
import com.paperconnekt.app.resources.filled_pause
import com.paperconnekt.app.resources.filled_play_arrow
import com.paperconnekt.app.resources.filled_skip_next
import com.paperconnekt.app.resources.filled_skip_previous
import com.paperconnekt.app.resources.filled_stop
import org.jetbrains.compose.resources.painterResource
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.akram.bensalem.bistosh.core.designsystem.component.interactivePointer
import com.akram.bensalem.paper.connekt.feature.assistant.ReadAloudController
import com.akram.bensalem.paper.connekt.feature.assistant.ReadAloudStatus
import com.akram.bensalem.paper.connekt.feature.assistant.SpeechPlan
import com.akram.bensalem.paper.connekt.feature.assistant.saveSpeechPlanJson
import com.akram.bensalem.paper.connekt.feature.assistant.toJson

@Composable
internal fun ReadAloudControls(
    controller: ReadAloudController,
    text: String,
    plan: SpeechPlan? = null,
    rootId: String,
    exampleId: String,
    label: String,
    modifier: Modifier = Modifier,
    /** AI-cleaned `reading.md`, offered for download so the reading script can be reviewed. */
    readingScript: String? = null,
    preparingScript: Boolean = false,
    /** Phone layout: keeps prev/play/next/stop visible and moves the rest into an overflow menu. */
    compact: Boolean = false,
) {
    val state = controller.state
    val speechPlan = plan ?: com.akram.bensalem.paper.connekt.feature.assistant.createSpeechPlan(text)
    val canStart = speechPlan.segments.any { !it.skipped } && !state.isActive
    val canNavigate = state.hasContent
    val canTogglePlayback = canStart || state.status == ReadAloudStatus.Playing || state.status == ReadAloudStatus.Paused
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        shadowElevation = 12.dp,
        tonalElevation = 5.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        val canGoBack = canNavigate && state.currentChunk > 0
        val canGoForward = canNavigate && state.currentChunk < state.chunks.lastIndex
        val togglePlayback: () -> Unit = {
            when (state.status) {
                ReadAloudStatus.Playing -> controller.pause()
                ReadAloudStatus.Paused -> controller.resume()
                else -> controller.start(
                    speechPlan,
                    rootId,
                    exampleId,
                    startChunk = state.currentChunk.takeIf { state.status == ReadAloudStatus.Error } ?: 0,
                )
            }
        }
        if (compact) Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            IconButton(enabled = canGoBack, onClick = controller::previousPart,
                modifier = Modifier.interactivePointer(enabled = canGoBack)) {
                Icon(painterResource(Res.drawable.filled_skip_previous), "Read previous part")
            }
            FilledIconButton(enabled = canTogglePlayback, onClick = togglePlayback,
                modifier = Modifier.size(44.dp).interactivePointer(enabled = canTogglePlayback)) {
                Icon(
                    if (state.status == ReadAloudStatus.Playing) painterResource(Res.drawable.filled_pause) else painterResource(Res.drawable.filled_play_arrow),
                    if (state.status == ReadAloudStatus.Playing) "Pause read aloud" else "Start read aloud",
                )
            }
            IconButton(enabled = canGoForward, onClick = controller::nextPart,
                modifier = Modifier.interactivePointer(enabled = canGoForward)) {
                Icon(painterResource(Res.drawable.filled_skip_next), "Read next part")
            }
            IconButton(enabled = state.isActive, onClick = controller::stop,
                modifier = Modifier.interactivePointer(enabled = state.isActive)) {
                Icon(painterResource(Res.drawable.filled_stop), "Stop read aloud")
            }
            Box {
                var menu by remember { mutableStateOf(false) }
                IconButton(onClick = { menu = true }, modifier = Modifier.interactivePointer()) {
                    Icon(painterResource(Res.drawable.filled_more_vert), "More read aloud options")
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("First part") }, enabled = canGoBack,
                        leadingIcon = { Icon(painterResource(Res.drawable.filled_first_page), null) },
                        onClick = { menu = false; controller.firstPart() },
                        modifier = Modifier.interactivePointer(enabled = canGoBack))
                    DropdownMenuItem(text = { Text("Last part") }, enabled = canGoForward,
                        leadingIcon = { Icon(painterResource(Res.drawable.filled_last_page), null) },
                        onClick = { menu = false; controller.lastPart() },
                        modifier = Modifier.interactivePointer(enabled = canGoForward))
                    DropdownMenuItem(text = { Text("Download speech plan") },
                        leadingIcon = { Icon(painterResource(Res.drawable.filled_download), null) },
                        onClick = { menu = false; saveSpeechPlanJson("$exampleId-speech-plan.json", speechPlan.toJson()) },
                        modifier = Modifier.interactivePointer())
                    if (readingScript != null) DropdownMenuItem(text = { Text("Download reading script") },
                        leadingIcon = { Icon(painterResource(Res.drawable.filled_description), null) },
                        onClick = { menu = false; saveSpeechPlanJson("$rootId-reading.md", readingScript) },
                        modifier = Modifier.interactivePointer())
                }
            }
        } else Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            IconButton(
                enabled = canNavigate && state.currentChunk > 0,
                onClick = controller::firstPart,
                modifier = Modifier.interactivePointer(enabled = canNavigate && state.currentChunk > 0),
            ) {
                Icon(painterResource(Res.drawable.filled_first_page), "Read first part")
            }
            IconButton(
                enabled = canNavigate && state.currentChunk > 0,
                onClick = controller::previousPart,
                modifier = Modifier.interactivePointer(enabled = canNavigate && state.currentChunk > 0),
            ) {
                Icon(painterResource(Res.drawable.filled_skip_previous), "Read previous part")
            }
            IconButton(
                enabled = canTogglePlayback,
                onClick = {
                    when (state.status) {
                        ReadAloudStatus.Playing -> controller.pause()
                        ReadAloudStatus.Paused -> controller.resume()
                        else -> controller.start(
                            speechPlan,
                            rootId,
                            exampleId,
                            startChunk = state.currentChunk.takeIf { state.status == ReadAloudStatus.Error } ?: 0,
                        )
                    }
                },
                modifier = Modifier.interactivePointer(enabled = canTogglePlayback),
            ) {
                Icon(
                    if (state.status == ReadAloudStatus.Playing) painterResource(Res.drawable.filled_pause) else painterResource(Res.drawable.filled_play_arrow),
                    if (state.status == ReadAloudStatus.Playing) "Pause read aloud" else "Start read aloud",
                )
            }
            IconButton(
                enabled = canNavigate && state.currentChunk < state.chunks.lastIndex,
                onClick = controller::nextPart,
                modifier = Modifier.interactivePointer(enabled = canNavigate && state.currentChunk < state.chunks.lastIndex),
            ) {
                Icon(painterResource(Res.drawable.filled_skip_next), "Read next part")
            }
            IconButton(
                enabled = canNavigate && state.currentChunk < state.chunks.lastIndex,
                onClick = controller::lastPart,
                modifier = Modifier.interactivePointer(enabled = canNavigate && state.currentChunk < state.chunks.lastIndex),
            ) {
                Icon(painterResource(Res.drawable.filled_last_page), "Read last part")
            }
            IconButton(
                enabled = state.isActive,
                onClick = controller::stop,
                modifier = Modifier.interactivePointer(enabled = state.isActive),
            ) {
                Icon(painterResource(Res.drawable.filled_stop), "Stop read aloud")
            }
            IconButton(
                onClick = {
                    saveSpeechPlanJson(
                        "$exampleId-speech-plan.json",
                        speechPlan.toJson(),
                    )
                },
                modifier = Modifier.interactivePointer(),
            ) {
                Icon(painterResource(Res.drawable.filled_download), "Download speech plan JSON")
            }
            if (readingScript != null) {
                IconButton(
                    onClick = { saveSpeechPlanJson("$rootId-reading.md", readingScript) },
                    modifier = Modifier.interactivePointer(),
                ) {
                    Icon(painterResource(Res.drawable.filled_description), "Download reading script (Markdown)")
                }
            }
        }
        if (state.status == ReadAloudStatus.Preparing || preparingScript) {
            LinearProgressIndicator()
        }
    }
}
