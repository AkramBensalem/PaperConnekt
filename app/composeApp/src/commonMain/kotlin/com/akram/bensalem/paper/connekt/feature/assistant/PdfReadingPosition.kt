package com.akram.bensalem.paper.connekt.feature.assistant

data class PdfReadingPosition(val pageIndex: Int = 0, val fraction: Float = 0f)

enum class PdfScrollSource { Pdf, Sidebar, Navigation }

internal data class AnnotationPageTrack(
    val fractions: List<Float>,
    val offsets: List<Int>,
    val height: Int,
    val header: Int
) {
    fun offsetAt(fraction: Float): Int = interpolate(
        fraction.coerceIn(0f, 1f),
        listOf(0f) + fractions + 1f,
        listOf(header.toFloat()) + offsets.map(Int::toFloat) + height.toFloat()
    ).toInt()

    fun fractionAt(offset: Int): Float = interpolate(
        offset.toFloat(),
        listOf(header.toFloat()) + offsets.map(Int::toFloat) + height.toFloat(),
        listOf(0f) + fractions + 1f
    ).coerceIn(0f, 1f)
}

internal fun annotationPageTrack(fractions: List<Float>, heights: List<Int>, pageHeight: Int, header: Int, gap: Int): AnnotationPageTrack {
    require(fractions.size == heights.size)
    require(fractions.zipWithNext().all { (a, b) -> a <= b })
    var bottom = header
    val offsets = fractions.mapIndexed { index, fraction ->
        maxOf(header + (fraction * pageHeight).toInt(), bottom).also { bottom = it + heights[index] + gap }
    }
    return AnnotationPageTrack(fractions, offsets, maxOf(header + pageHeight, bottom), header)
}

private fun interpolate(value: Float, from: List<Float>, to: List<Float>): Float {
    if (value <= from.first()) return to.first()
    for (index in 1 until from.size) {
        if (value <= from[index] && from[index] > from[index - 1]) {
            val ratio = (value - from[index - 1]) / (from[index] - from[index - 1])
            return to[index - 1] + ratio * (to[index] - to[index - 1])
        }
    }
    return to.last()
}
