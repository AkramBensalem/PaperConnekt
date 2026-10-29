package com.paperconnekt.app.legacygraph

import androidx.compose.ui.Modifier
import com.akram.bensalem.bistosh.core.designsystem.component.clipChipInteraction as roundedChipInteraction
import com.akram.bensalem.bistosh.core.designsystem.component.interactivePointer as interactiveCursor
import com.paperconnekt.domain.model.Paper

fun Modifier.interactivePointer(enabled: Boolean = true): Modifier = interactiveCursor(enabled)
fun Modifier.clipChipInteraction(): Modifier = roundedChipInteraction()

enum class TriageDecision { KEEP, DISQUALIFY }

data class PaperTriageResult(
    val paperId: String,
    val title: String,
    val decision: TriageDecision,
    val rationale: String,
)

data class RelevanceScore(val score: Int, val rationale: String = "")

/** Local text similarity used by the copied graph filtering and guided-review algorithms. */
fun paperContentSimilarity(first: Paper, second: Paper): Float {
    val left = (first.title + " " + first.abstractText.orEmpty()).lowercase()
        .split(Regex("\\W+"))
        .filter(String::isNotBlank)
        .toSet()
    val right = (second.title + " " + second.abstractText.orEmpty()).lowercase()
        .split(Regex("\\W+"))
        .filter(String::isNotBlank)
        .toSet()
    if (left.isEmpty() || right.isEmpty()) return 0f
    return (left intersect right).size.toFloat() / (left union right).size.toFloat()
}
